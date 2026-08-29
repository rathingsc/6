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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * v3.3.8 sentence-by-sentence memorisation mode.
 * Each aligned sentence cycles through: bilingual -> Italian only -> cloze -> Chinese recall.
 */
public class MemoryArticleSentenceStudyFragment extends Fragment {
    private static final String ARG_ARTICLE="article",ARG_SECTION="section";
    private static final Set<String> STOPWORDS=new HashSet<>(Arrays.asList(
            "sono","essere","avere","anche","della","delle","degli","dello","alla","alle","agli","allo","nella","nelle","negli","nello","dalla","dalle","dagli","dallo","dopo","prima","quando","mentre","perché","perche","come","questa","questo","quella","quello","molto","sempre","senza","verso","qualche","ogni","solo","tutto","tutta","tutti","tutte","ancora","quindi","poi","dove","cosa","fare","fatto","viene","hanno","abbiamo","avete","essere","anche"));

    private MemoryArticleRepository repo;private WordRepository words;private ProgressStore progress;private MemoryArticle article;private MemoryArticleSection section;private AudioPlayer audio;
    private int sectionIndex=0,sentenceIndex=0,phase=0;private boolean revealed=false,complete=false;
    private TextView progressText,title,mode,italian,chinese,keyWords,finishHint;private ProgressBar bar;
    private MaterialButton normalAudio,slowAudio,reveal,next,fullStudy,review;

    public static MemoryArticleSentenceStudyFragment newInstance(String articleId,int sectionIndex){MemoryArticleSentenceStudyFragment f=new MemoryArticleSentenceStudyFragment();Bundle b=new Bundle();b.putString(ARG_ARTICLE,articleId);b.putInt(ARG_SECTION,sectionIndex);f.setArguments(b);return f;}

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_memory_article_sentence_study,parent,false);repo=MemoryArticleRepository.get(requireContext());words=WordRepository.get(requireContext());progress=new ProgressStore(requireContext());audio=new AudioPlayer(requireContext(),progress);
        String id=getArguments()==null?null:getArguments().getString(ARG_ARTICLE);sectionIndex=getArguments()==null?0:getArguments().getInt(ARG_SECTION,0);article=repo.byId(id);section=repo.section(id,sectionIndex);if(article==null||section==null||section.sentences.isEmpty())return v;
        progressText=v.findViewById(R.id.text_memory_sentence_progress);bar=v.findViewById(R.id.progress_memory_sentence);title=v.findViewById(R.id.text_memory_sentence_title);mode=v.findViewById(R.id.text_memory_sentence_mode);italian=v.findViewById(R.id.text_memory_sentence_italian);chinese=v.findViewById(R.id.text_memory_sentence_chinese);keyWords=v.findViewById(R.id.text_memory_sentence_keywords);finishHint=v.findViewById(R.id.text_memory_sentence_finish_hint);
        normalAudio=v.findViewById(R.id.button_memory_sentence_audio_normal);slowAudio=v.findViewById(R.id.button_memory_sentence_audio_slow);reveal=v.findViewById(R.id.button_memory_sentence_reveal);next=v.findViewById(R.id.button_memory_sentence_next);fullStudy=v.findViewById(R.id.button_memory_sentence_full_study);review=v.findViewById(R.id.button_memory_sentence_review);
        title.setText(article.emoji+" "+section.titleZh+"\n逐句背文章 · "+section.title);normalAudio.setOnClickListener(x->speakCurrent(1.0f));slowAudio.setOnClickListener(x->speakCurrent(0.70f));reveal.setOnClickListener(x->{revealed=true;render();});next.setOnClickListener(x->advance());
        fullStudy.setOnClickListener(x->((MainActivity)requireActivity()).openMemoryArticleStudy(article.id,sectionIndex));review.setOnClickListener(x->((MainActivity)requireActivity()).openMemoryArticleReview(combinedReviewIds(),article.titleZh+" · "+section.titleZh,Math.min(52,section.targetWordIds.size()+section.reviewWordIds.size())));
        render();return v;
    }

    private MemoryArticleSentence current(){return section.sentences.get(Math.max(0,Math.min(sentenceIndex,section.sentences.size()-1)));}
    private void speakCurrent(float rate){if(!complete)audio.speak(current().italian,rate);else audio.speak(section.text,rate);}

    private void advance(){
        if(complete){((MainActivity)requireActivity()).openMemoryArticle(article.id);return;}
        if((phase==2||phase==3)&&!revealed)return;
        if(phase<3){phase++;revealed=false;render();if(phase==1)italian.postDelayed(()->{if(isAdded()&&!complete&&phase==1)audio.speak(current().italian,1.0f);},180);return;}
        sentenceIndex++;phase=0;revealed=false;if(sentenceIndex>=section.sentences.size())finish();else render();
    }

    private void finish(){
        complete=true;progress.markMemoryArticleSentenceStudyDone(section.id);for(Integer id:section.targetWordIds)if(id!=null)progress.recordMemoryArticleExposure(id);render();
    }

    private void render(){
        if(complete){
            progressText.setText("逐句背诵完成 ✅");bar.setProgress(100);mode.setText("本节已经逐句完成“看懂 → 听读 → 挖空 → 中文反推”。下一步可以做整节5步背诵，或直接用四模式智能复习检查是否真的记住。");italian.setText(section.text);italian.setVisibility(View.VISIBLE);chinese.setText(section.translation);chinese.setVisibility(View.VISIBLE);keyWords.setText("逐句背诵完成 · 共 "+section.sentences.size()+" 句 · 本节40个目标词已再次记录文章接触");normalAudio.setText("🔊 正常朗读整节");slowAudio.setText("🐢 慢速朗读整节");reveal.setVisibility(View.GONE);next.setText("返回本篇");next.setEnabled(true);fullStudy.setVisibility(View.VISIBLE);review.setVisibility(View.VISIBLE);finishHint.setVisibility(View.VISIBLE);finishHint.setText("建议：先做“整节5步背诵”，再用智能复习检验薄弱词。阅读完成不等于真正掌握。");return;
        }
        MemoryArticleSentence s=current();int total=section.sentences.size()*4,currentStep=sentenceIndex*4+phase+1;progressText.setText("第 "+(sentenceIndex+1)+" / "+section.sentences.size()+" 句 · 第 "+(phase+1)+" / 4 轮");bar.setProgress((int)Math.round(currentStep*100.0/Math.max(1,total)));normalAudio.setText("🔊 正常");slowAudio.setText("🐢 慢速");fullStudy.setVisibility(View.GONE);review.setVisibility(View.GONE);finishHint.setVisibility(View.GONE);reveal.setVisibility(View.GONE);next.setEnabled(true);keyWords.setText(keyWordsText(s.italian));
        switch(phase){
            case 0:
                mode.setText("第1轮 · 双语理解：先把这一句真正看懂，再进入听读。");italian.setText(s.italian);italian.setVisibility(View.VISIBLE);chinese.setText(s.chinese);chinese.setVisibility(View.VISIBLE);next.setText("下一步 · 听读这句");break;
            case 1:
                mode.setText("第2轮 · 听读：先听正常速度或慢速，再只看意大利语复述意思。");italian.setText(s.italian);italian.setVisibility(View.VISIBLE);chinese.setText("中文已隐藏 · 先自己理解这句");chinese.setVisibility(View.VISIBLE);next.setText("下一步 · 挖空回忆");break;
            case 2:
                mode.setText("第3轮 · 挖空回忆：先补出空缺，再显示完整句子核对。");italian.setText(revealed?s.italian:clozeSentence(s.italian));italian.setVisibility(View.VISIBLE);chinese.setText(s.chinese);chinese.setVisibility(View.VISIBLE);reveal.setVisibility(revealed?View.GONE:View.VISIBLE);reveal.setText("显示完整句子");next.setEnabled(revealed);next.setText(revealed?"下一步 · 中文反推":"先显示答案再继续");break;
            default:
                mode.setText("第4轮 · 中文反推：只看中文，大声说出整句意大利语，再核对。");italian.setText(revealed?s.italian:"先不要看意大利语 · 根据下面中文大声说出整句");italian.setVisibility(View.VISIBLE);chinese.setText(s.chinese);chinese.setVisibility(View.VISIBLE);reveal.setVisibility(revealed?View.GONE:View.VISIBLE);reveal.setText("显示意大利语答案");next.setEnabled(revealed);next.setText(revealed?(sentenceIndex+1<section.sentences.size()?"下一句":"完成逐句背诵"):"先显示答案再继续");break;
        }
    }

    private String keyWordsText(String sentence){
        List<String> found=new ArrayList<>();String lower=sentence.toLowerCase(Locale.ITALIAN);
        for(Integer id:section.targetWordIds){if(id==null)continue;Word w=words.byId(id);if(w==null||w.word==null||w.word.trim().isEmpty())continue;String token=w.word.trim();if(containsWord(lower,token.toLowerCase(Locale.ITALIAN))){found.add(token+"  "+(w.chinese==null?"":w.chinese));if(found.size()>=6)break;}}
        if(found.isEmpty())return "本句重点：先整体理解，不需要逐词翻译";
        StringBuilder sb=new StringBuilder("本句重点：");for(int i=0;i<found.size();i++){if(i>0)sb.append(" · ");sb.append(found.get(i));}return sb.toString();
    }

    private String clozeSentence(String sentence){
        String out=sentence;int hidden=0;
        for(Integer id:section.targetWordIds){if(id==null)continue;Word w=words.byId(id);if(w==null||w.word==null)continue;Pattern p=wordPattern(w.word);Matcher m=p.matcher(out);if(m.find()){out=m.replaceFirst("______");hidden++;if(hidden>=2)break;}}
        if(hidden>0)return out;
        Matcher m=Pattern.compile("(?iu)\\b[\\p{L}À-ÿ']{4,}\\b").matcher(sentence);while(m.find()){String token=m.group();if(!STOPWORDS.contains(token.toLowerCase(Locale.ITALIAN)))return sentence.substring(0,m.start())+"______"+sentence.substring(m.end());}
        return "______";
    }

    private boolean containsWord(String lowerSentence,String lowerWord){return wordPattern(lowerWord).matcher(lowerSentence).find();}
    private Pattern wordPattern(String word){return Pattern.compile("(?iu)(?<![\\p{L}])"+Pattern.quote(word)+"(?![\\p{L}])");}
    private int[] combinedReviewIds(){List<Integer> ids=new ArrayList<>();ids.addAll(section.targetWordIds);for(Integer id:section.reviewWordIds)if(id!=null&&!ids.contains(id))ids.add(id);int[] a=new int[ids.size()];for(int i=0;i<a.length;i++)a[i]=ids.get(i);return a;}
    @Override public void onDestroyView(){if(audio!=null)audio.release();super.onDestroyView();}
}
