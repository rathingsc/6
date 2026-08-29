package com.italiano2774.nativeapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Five-step memorisation loop with spiral review of older words. */
public class MemoryArticleStudyFragment extends Fragment {
    private static final String ARG_ARTICLE="article",ARG_SECTION="section";
    private MemoryArticleRepository repo;private WordRepository words;private ProgressStore progress;private MemoryArticle article;private MemoryArticleSection section;private AudioPlayer audio;
    private int sectionIndex=0,step=0;private boolean revealed=false,complete=false,manualTextVisible=false;
    private TextView progressText,title,mode,italian,chinese,targets,finishHint,reinforcementStats,reinforcementIt,reinforcementZh;
    private ProgressBar bar;private MaterialButton audioNormal,audioSlow,toggle,reveal,next,review;private MaterialCardView targetCard,reinforcementCard;

    public static MemoryArticleStudyFragment newInstance(String articleId,int sectionIndex){MemoryArticleStudyFragment f=new MemoryArticleStudyFragment();Bundle b=new Bundle();b.putString(ARG_ARTICLE,articleId);b.putInt(ARG_SECTION,sectionIndex);f.setArguments(b);return f;}

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_memory_article_study,parent,false);repo=MemoryArticleRepository.get(requireContext());words=WordRepository.get(requireContext());progress=new ProgressStore(requireContext());audio=new AudioPlayer(requireContext(),progress);
        String id=getArguments()==null?null:getArguments().getString(ARG_ARTICLE);sectionIndex=getArguments()==null?0:getArguments().getInt(ARG_SECTION,0);article=repo.byId(id);section=repo.section(id,sectionIndex);if(article==null||section==null)return v;
        progressText=v.findViewById(R.id.text_memory_study_progress);bar=v.findViewById(R.id.progress_memory_study);title=v.findViewById(R.id.text_memory_study_title);mode=v.findViewById(R.id.text_memory_study_mode);italian=v.findViewById(R.id.text_memory_italian);chinese=v.findViewById(R.id.text_memory_chinese);targets=v.findViewById(R.id.text_memory_targets);finishHint=v.findViewById(R.id.text_memory_finish_hint);
        audioNormal=v.findViewById(R.id.button_memory_audio_normal);audioSlow=v.findViewById(R.id.button_memory_audio_slow);toggle=v.findViewById(R.id.button_memory_toggle);reveal=v.findViewById(R.id.button_memory_reveal);next=v.findViewById(R.id.button_memory_next);review=v.findViewById(R.id.button_memory_section_review);targetCard=v.findViewById(R.id.card_memory_targets);
        reinforcementCard=v.findViewById(R.id.card_memory_reinforcement);reinforcementStats=v.findViewById(R.id.text_memory_reinforcement_stats);reinforcementIt=v.findViewById(R.id.text_memory_reinforcement_it);reinforcementZh=v.findViewById(R.id.text_memory_reinforcement_zh);
        title.setText(article.emoji+" "+section.titleZh+"\n"+section.title);targets.setText(targetWordsText());renderReinforcement();
        audioNormal.setOnClickListener(x->audio.speak(fullAudioText(),1.0f));audioSlow.setOnClickListener(x->audio.speak(fullAudioText(),0.70f));toggle.setOnClickListener(x->toggleHelp());reveal.setOnClickListener(x->{revealed=true;render();});next.setOnClickListener(x->advance());
        review.setOnClickListener(x->((MainActivity)requireActivity()).openMemoryArticleReview(combinedReviewIds(),article.titleZh+" · "+section.titleZh,Math.min(52,section.targetWordIds.size()+section.reviewWordIds.size())));
        render();return v;
    }

    private void advance(){if(complete){if(sectionIndex+1<article.sections.size())((MainActivity)requireActivity()).openMemoryArticleStudy(article.id,sectionIndex+1);else ((MainActivity)requireActivity()).openMemoryArticle(article.id);return;}if((step==3||step==4)&&!revealed)return;if(step<4){step++;revealed=false;manualTextVisible=false;render();if(step==2)italian.postDelayed(()->{if(isAdded()&&step==2)audio.speak(fullAudioText());},220);}else finish();}

    private void finish(){
        complete=true;progress.markMemoryArticleSectionDone(section.id);
        for(Integer id:section.targetWordIds)if(id!=null)progress.recordMemoryArticleExposure(id);
        for(Integer id:section.reviewWordIds)if(id!=null)progress.recordMemoryArticleExposure(id);
        targets.setText(targetWordsText());renderReinforcement();render();
    }

    private void toggleHelp(){if(step==2){manualTextVisible=!manualTextVisible;render();return;}chinese.setVisibility(chinese.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);toggle.setText(chinese.getVisibility()==View.VISIBLE?"隐藏中文":"显示中文");if(reinforcementCard!=null&&reinforcementCard.getVisibility()==View.VISIBLE)reinforcementZh.setVisibility(chinese.getVisibility());}

    private void render(){
        if(complete){
            progressText.setText("本节完成 ✅");bar.setProgress(100);mode.setText("这40个目标词已经完成一次文章接触；旧词也在后文重新出现。现在用四模式智能复习把薄弱项继续送进间隔复习。");
            italian.setText(section.text);italian.setVisibility(View.VISIBLE);chinese.setText(section.translation);chinese.setVisibility(View.VISIBLE);targetCard.setVisibility(View.VISIBLE);reinforcementCard.setVisibility(section.reviewWordIds.isEmpty()?View.GONE:View.VISIBLE);
            toggle.setVisibility(View.GONE);reveal.setVisibility(View.GONE);audioNormal.setVisibility(View.VISIBLE);audioSlow.setVisibility(View.VISIBLE);review.setVisibility(View.VISIBLE);next.setEnabled(true);next.setText(sectionIndex+1<article.sections.size()?"继续下一小节":"返回本篇");finishHint.setVisibility(View.VISIBLE);
            finishHint.setText("本节40个目标词 + "+section.reviewWordIds.size()+"个旧词已记录文章接触 · 只有四维能力达标才算真正掌握");return;
        }
        progressText.setText("第"+(step+1)+"步 / 5 · 本篇第"+(sectionIndex+1)+" / "+article.sections.size()+"小节");bar.setProgress((step+1)*20);audioNormal.setVisibility(View.VISIBLE);audioSlow.setVisibility(View.VISIBLE);toggle.setVisibility(View.VISIBLE);review.setVisibility(View.GONE);finishHint.setVisibility(View.GONE);reveal.setVisibility(View.GONE);next.setEnabled(true);targetCard.setVisibility(step==0?View.VISIBLE:View.GONE);
        reinforcementCard.setVisibility((step<=1&&!section.reviewWordIds.isEmpty())?View.VISIBLE:View.GONE);
        switch(step){
            case 0:mode.setText("双语精读 · 先理解故事，逐个看完40个目标词；下面还会带回已经学过的高频旧词");italian.setText(section.text);italian.setVisibility(View.VISIBLE);chinese.setText(section.translation);chinese.setVisibility(View.VISIBLE);toggle.setText("隐藏中文");next.setText("下一步 · 只看意文");break;
            case 1:mode.setText("只看意大利语 · 尽量自己理解；旧词复现区也先读意文，再核对中文");italian.setText(section.text);italian.setVisibility(View.VISIBLE);chinese.setText(section.translation);chinese.setVisibility(View.GONE);toggle.setText("显示中文");next.setText("下一步 · 只听");break;
            case 2:mode.setText("只听 · 正文和旧词复现句会连续朗读，先不看文字");italian.setText(manualTextVisible?fullAudioText():"🔊 正文和旧词复现句已隐藏。点击正常或慢速朗读，先只靠耳朵理解。");italian.setVisibility(View.VISIBLE);chinese.setVisibility(View.GONE);toggle.setText(manualTextVisible?"隐藏原文":"显示原文");next.setText("下一步 · 挖空回忆");break;
            case 3:mode.setText("挖空回忆 · 先在脑中补出空缺，再显示完整原文核对");italian.setText(revealed?section.text:clozeText());italian.setVisibility(View.VISIBLE);chinese.setText(section.translation);chinese.setVisibility(View.VISIBLE);toggle.setVisibility(View.GONE);reveal.setVisibility(revealed?View.GONE:View.VISIBLE);reveal.setText("显示完整原文");next.setEnabled(revealed);next.setText(revealed?"下一步 · 中文反推":"先显示答案再继续");break;
            default:mode.setText("中文反推 · 只看中文，尽量把整段意大利语说出来，再核对");chinese.setText(section.translation);chinese.setVisibility(View.VISIBLE);italian.setText(revealed?section.text:"先不要看意大利语。根据下面中文，在脑中复述或大声说出意大利语。");italian.setVisibility(View.VISIBLE);toggle.setVisibility(View.GONE);reveal.setVisibility(revealed?View.GONE:View.VISIBLE);reveal.setText("显示意大利语答案");next.setEnabled(revealed);next.setText(revealed?"完成本节":"先显示答案再完成");break;
        }
        if(reinforcementCard.getVisibility()==View.VISIBLE)reinforcementZh.setVisibility(step==1&&chinese.getVisibility()!=View.VISIBLE?View.GONE:View.VISIBLE);
    }

    private String clozeText(){String out=section.text;for(String w:section.clozeWords){Pattern p=Pattern.compile("(?iu)(?<![\\p{L}])"+Pattern.quote(w)+"(?![\\p{L}])");Matcher m=p.matcher(out);if(m.find())out=m.replaceFirst("______");}return out;}

    private String targetWordsText(){
        int encountered=progress.memoryArticleEncounteredCount(section.targetWordIds),recognized=progress.memoryArticleRecognizedCount(section.targetWordIds),mastered=progress.memoryArticleMasteredCount(section.targetWordIds);
        StringBuilder sb=new StringBuilder();for(int i=0;i<section.targetWordIds.size();i++){Word w=words.byId(section.targetWordIds.get(i));if(w==null)continue;sb.append(w.word).append("  ").append(w.chinese==null?"":w.chinese);if(i<section.targetWordIds.size()-1)sb.append(i%2==1?"\n":"    ·    ");}
        return "已遇到 "+encountered+" / 40 · 已认识 "+recognized+" · 真正掌握 "+mastered+" · 待巩固 "+(40-mastered)+"\n"+sb;
    }

    private void renderReinforcement(){
        if(section.reviewWordIds.isEmpty()){reinforcementCard.setVisibility(View.GONE);return;}
        int seen=progress.memoryArticleEncounteredCount(section.reviewWordIds),mastered=progress.memoryArticleMasteredCount(section.reviewWordIds);StringBuilder wordsLine=new StringBuilder();
        for(int i=0;i<section.reviewWordIds.size();i++){Word w=words.byId(section.reviewWordIds.get(i));if(w==null)continue;if(wordsLine.length()>0)wordsLine.append(i%3==0?"\n":" · ");wordsLine.append(w.word).append(" ").append(w.chinese==null?"":w.chinese);}
        reinforcementStats.setText("本节复现 "+section.reviewWordIds.size()+" 个旧词 · 已再次遇到 "+seen+" · 已掌握 "+mastered+"\n"+wordsLine);
        StringBuilder it=new StringBuilder(),zh=new StringBuilder();for(MemoryArticleReinforcement r:section.reinforcementItems){if(it.length()>0){it.append("\n");zh.append("\n");}it.append("• ").append(r.sentence);zh.append("• ").append(r.translation);}
        reinforcementIt.setText(it);reinforcementZh.setText(zh);
    }

    private String fullAudioText(){StringBuilder sb=new StringBuilder(section.text);for(MemoryArticleReinforcement r:section.reinforcementItems)if(r.sentence!=null&&!r.sentence.isEmpty())sb.append(" ").append(r.sentence);return sb.toString();}

    private int[] combinedReviewIds(){List<Integer> ids=new ArrayList<>();ids.addAll(section.targetWordIds);for(Integer id:section.reviewWordIds)if(id!=null&&!ids.contains(id))ids.add(id);int[] a=new int[ids.size()];for(int i=0;i<a.length;i++)a[i]=ids.get(i);return a;}

    @Override public void onDestroyView(){if(audio!=null)audio.release();super.onDestroyView();}
}
