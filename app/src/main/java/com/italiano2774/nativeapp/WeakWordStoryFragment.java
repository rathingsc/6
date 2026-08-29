package com.italiano2774.nativeapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** v4.7.0: weak words -> audited micro-reading -> listening cloze -> active retell. */
public class WeakWordStoryFragment extends Fragment {
    private static final int REQ_AUDIO=847;
    private static final Pattern TOKEN=Pattern.compile("[\\p{L}À-ÿ'’]+",Pattern.UNICODE_CASE);
    private ProgressStore progress;private WordRepository words;private WeakWordStory story;private AudioPlayer audio;private SpeechRecognizer recognizer;
    private TextView step,source,targets,body,translation,instruction,feedback;private EditText input;private ProgressBar bar;
    private MaterialButton slow,normal,toggleTranslation,mic,primary,next;
    private int stage=0,listenPlays=0,retellAttempts=0,variant=0;private boolean clozeRecorded=false,completed=false,speechUsed=false;private long startedAt;

    public static WeakWordStoryFragment newInstance(int variant){WeakWordStoryFragment f=new WeakWordStoryFragment();Bundle b=new Bundle();b.putInt("variant",Math.max(0,variant));f.setArguments(b);return f;}

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_weak_word_story,parent,false);variant=getArguments()==null?0:getArguments().getInt("variant",0);progress=new ProgressStore(requireContext());words=WordRepository.get(requireContext());story=new WeakWordStoryEngine(requireContext(),words,progress).build(LocalDate.now(),variant);audio=new AudioPlayer(requireContext(),progress);
        step=v.findViewById(R.id.text_weak_story_step);source=v.findViewById(R.id.text_weak_story_source);targets=v.findViewById(R.id.text_weak_story_targets);body=v.findViewById(R.id.text_weak_story_body);translation=v.findViewById(R.id.text_weak_story_translation);instruction=v.findViewById(R.id.text_weak_story_instruction);feedback=v.findViewById(R.id.text_weak_story_feedback);input=v.findViewById(R.id.edit_weak_story_answer);bar=v.findViewById(R.id.progress_weak_story);slow=v.findViewById(R.id.button_weak_story_slow);normal=v.findViewById(R.id.button_weak_story_normal);toggleTranslation=v.findViewById(R.id.button_weak_story_translation);mic=v.findViewById(R.id.button_weak_story_mic);primary=v.findViewById(R.id.button_weak_story_primary);next=v.findViewById(R.id.button_weak_story_next);
        v.findViewById(R.id.button_weak_story_back).setOnClickListener(x->((MainActivity)requireActivity()).openPractice());slow.setOnClickListener(x->{listenPlays++;audio.speak(story.italian,0.70f);if(stage==1)primary.setEnabled(true);updateListenHint();});normal.setOnClickListener(x->{listenPlays++;audio.speak(story.italian,0.92f);if(stage==1)primary.setEnabled(true);updateListenHint();});toggleTranslation.setOnClickListener(x->{boolean show=translation.getVisibility()!=View.VISIBLE;translation.setVisibility(show?View.VISIBLE:View.GONE);toggleTranslation.setText(show?"隐藏中文":"显示中文");});mic.setOnClickListener(x->requestOrStartSpeech());next.setOnClickListener(x->advanceAfterResult());startedAt=System.currentTimeMillis();render();return v;
    }

    private void render(){
        source.setText(story.title+"\n"+story.sourceLabel+" · "+story.wordCount+"词 · 从"+story.weakCandidateCount+"个当前薄弱/已学词中筛选");targets.setText(story.targetWords.isEmpty()?"目标词：先积累一些学习记录，个性化会更精准。":"今日重点词（"+story.targetWords.size()+"）：\n"+story.targetSummary());feedback.setVisibility(View.GONE);next.setVisibility(View.GONE);input.setVisibility(View.GONE);mic.setVisibility(View.GONE);translation.setVisibility(View.GONE);toggleTranslation.setText("显示中文");slow.setVisibility(View.VISIBLE);normal.setVisibility(View.VISIBLE);toggleTranslation.setVisibility(View.VISIBLE);primary.setEnabled(true);
        if(stage==0){step.setText("第1 / 4步 · 先读懂");bar.setProgress(10);body.setText(story.italian);body.setVisibility(View.VISIBLE);instruction.setText("先自己读一遍。遇到蓝色重点词不必逐字背，先理解整段意思；需要时再显示中文。");primary.setText("完成阅读 · 去听力");primary.setOnClickListener(x->{stage=1;render();});}
        else if(stage==1){step.setText("第2 / 4步 · 不看文字听");bar.setProgress(35);body.setText("🔊 把短文文字暂时藏起来。先听完整短文至少1遍，再进入挖空。\n\n建议：第一遍 0.70×，第二遍 0.92×。");instruction.setText("这一轮不看意大利语正文。听完后下一步会从刚才的重点词里抽4个做听力+拼写挖空。");translation.setVisibility(View.GONE);toggleTranslation.setVisibility(View.GONE);primary.setText(listenPlays>0?"我听完了 · 去挖空":"先播放至少1遍");primary.setEnabled(listenPlays>0);primary.setOnClickListener(x->{if(listenPlays>0){stage=2;render();}});}
        else if(stage==2){step.setText("第3 / 4步 · 听后挖空");bar.setProgress(62);body.setText(clozeText());instruction.setText("按空格编号顺序输入缺失词，用空格或逗号分隔。这里同时检查听力记忆和拼写。共 "+story.clozeTargets.size()+" 个空。 ");input.setVisibility(View.VISIBLE);input.setHint("例如：prendere autobus stazione");slow.setVisibility(View.GONE);normal.setVisibility(View.GONE);toggleTranslation.setVisibility(View.GONE);primary.setText("检查挖空");primary.setOnClickListener(x->checkCloze());}
        else if(stage==3){step.setText("第4 / 4步 · 只看中文复述");bar.setProgress(82);body.setText(story.chinese);translation.setVisibility(View.GONE);toggleTranslation.setVisibility(View.GONE);instruction.setText("不要求逐字背原文。用自己的意大利语复述主要意思，并尽量主动用上今日重点词。可以直接说，也可以输入。通过线60%。");input.setVisibility(View.VISIBLE);input.setHint("用意大利语复述这一小段…");mic.setVisibility(View.VISIBLE);slow.setVisibility(View.GONE);normal.setVisibility(View.GONE);primary.setText(retellAttempts==0?"检查复述":"再检查一次");primary.setOnClickListener(x->checkRetell(input.getText()==null?"":input.getText().toString(),false));}
        else finishView();
    }

    private void updateListenHint(){if(stage==1){primary.setText("我听完了 · 去挖空");instruction.setText("已播放 "+listenPlays+" 遍。可以再听一次，或者进入挖空。听力本身不直接加分，下一步答对才会更新听力维度。 ");}}

    private String clozeText(){String text=story.italian;for(int i=0;i<story.clozeTargets.size();i++){WeakWordStory.ClozeTarget c=story.clozeTargets.get(i);String replacement="____("+(i+1)+")____";text=replaceFirstToken(text,c.answer,replacement);}return text;}
    private String replaceFirstToken(String text,String answer,String replacement){if(text==null||answer==null||answer.isEmpty())return text;Pattern p=Pattern.compile("(?iu)(?<!\\p{L})"+Pattern.quote(answer)+"(?!\\p{L})");Matcher m=p.matcher(text);return m.find()?m.replaceFirst(Matcher.quoteReplacement(replacement)):text;}

    private void checkCloze(){if(clozeRecorded)return;if(story.clozeTargets.isEmpty()){feedback.setText("这篇短文暂时没有可稳定抽取的重点词，直接进入中文复述。 ");feedback.setVisibility(View.VISIBLE);next.setText("去中文复述 →");next.setVisibility(View.VISIBLE);clozeRecorded=true;return;}String raw=input.getText()==null?"":input.getText().toString().trim();if(raw.isEmpty()){input.setError("先按顺序填写空缺词");return;}String[] entered=raw.split("[,，;；\\s]+");int correct=0;StringBuilder detail=new StringBuilder();long ms=Math.max(1,System.currentTimeMillis()-startedAt);for(int i=0;i<story.clozeTargets.size();i++){WeakWordStory.ClozeTarget c=story.clozeTargets.get(i);String actual=i<entered.length?entered[i]:"";boolean ok=ErrorCauseAnalyzer.basic(c.answer).equals(ErrorCauseAnalyzer.basic(actual));if(ok)correct++;progress.recordEmbeddedDimensionResults(c.wordId,new int[]{ProgressStore.DIM_LISTENING,ProgressStore.DIM_SPELLING},ok,ms);if(detail.length()>0)detail.append("\n");detail.append(i+1).append(". ").append(ok?"✓ ":"✗ ").append(c.answer);if(!ok&&!actual.isEmpty())detail.append("（你填：").append(actual).append("）");}
        int score=(int)Math.round(correct*100.0/story.clozeTargets.size());feedback.setText((score>=70?"✓ 挖空通过":"△ 还需要巩固")+" · "+correct+" / "+story.clozeTargets.size()+" · "+score+"%\n"+detail+"\n\n答错的目标词已经回流听力/拼写维度。 ");feedback.setTextColor(ContextCompat.getColor(requireContext(),score>=70?R.color.success:R.color.error));feedback.setVisibility(View.VISIBLE);input.setEnabled(false);primary.setEnabled(false);next.setText("去中文复述 →");next.setVisibility(View.VISIBLE);clozeRecorded=true;bar.setProgress(75);
    }

    private void advanceAfterResult(){if(stage==2){stage=3;input.setText("");input.setEnabled(true);render();}else if(stage>=4){((MainActivity)requireActivity()).openWeakWordStory(variant+1);}}

    private void requestOrStartSpeech(){if(stage!=3||completed)return;if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return;}startSpeech();}
    private void startSpeech(){if(!SpeechRecognizer.isRecognitionAvailable(requireContext())){feedback.setText("这台手机暂时没有可用的意大利语语音识别。可以直接在输入框里写复述，仍然会检查重点词覆盖。 ");feedback.setVisibility(View.VISIBLE);return;}if(recognizer!=null)recognizer.destroy();recognizer=SpeechRecognizer.createSpeechRecognizer(requireContext());recognizer.setRecognitionListener(new RecognitionListener(){public void onReadyForSpeech(Bundle b){mic.setText("🎙 正在听…");}public void onBeginningOfSpeech(){}public void onRmsChanged(float r){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){mic.setText("正在识别…");}public void onError(int e){mic.setText("🎙 说复述");feedback.setText("没有识别清楚。可以慢一点再说，或者直接文字输入。 ");feedback.setVisibility(View.VISIBLE);}public void onResults(Bundle b){mic.setText("🎙 说复述");ArrayList<String> xs=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);String best="";int bestScore=-1;if(xs!=null)for(String x:xs){int s=retellScore(x);if(s>bestScore){bestScore=s;best=x;}}speechUsed=true;input.setText(best);checkRetell(best,true);}public void onPartialResults(Bundle b){}public void onEvent(int t,Bundle b){}});Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"it-IT");i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);recognizer.startListening(i);}

    private void checkRetell(String text,boolean fromSpeech){if(stage!=3||completed)return;String answer=text==null?"":text.trim();if(answer.isEmpty()){input.setError("先说或写一小段意大利语复述");return;}speechUsed=speechUsed||fromSpeech;int score=retellScore(answer);retellAttempts++;boolean pass=score>=60;if(!pass&&retellAttempts<2){feedback.setText("第一次复述 "+score+"% · 还差一点。\n再看中文想一遍，尽量带上更多今日重点词，再试一次。第二次会作为今天的正式结果。 ");feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.error));feedback.setVisibility(View.VISIBLE);primary.setText("再检查一次");return;}finalizeRetell(answer,score,pass);}

    private void finalizeRetell(String answer,int score,boolean pass){completed=true;long ms=Math.max(1,System.currentTimeMillis()-startedAt);Set<Integer> hits=targetHits(answer);int dim=speechUsed?ProgressStore.DIM_SPEAKING:ProgressStore.DIM_SPELLING;int penalized=0;for(Word w:story.targetWords){if(hits.contains(w.id))progress.recordEmbeddedDimensionResult(w.id,dim,true,ms);else if(!pass&&penalized<3){progress.recordEmbeddedDimensionResult(w.id,dim,false,ms);penalized++;}}
        int sentenceDim=speechUsed?SentenceFsrsRepository.DIM_SPEAKING:SentenceFsrsRepository.DIM_RECALL;for(MemoryArticleSentence s:story.sentences)SentenceFsrsRepository.recordDimension(requireContext(),"今日弱词微短文",s.italian,s.chinese,sentenceDim,pass,score,null);progress.recordAuxiliaryResult("weak_story",pass,ms);feedback.setText((pass?"✓ 复述通过":"△ 今天先记为需要巩固")+" · "+score+"% · 主动用到重点词 "+hits.size()+" / "+story.targetWords.size()+"\n"+(speechUsed?"本次按口语主动输出记录。":"本次按文字主动回忆记录；下次可用麦克风练口语。")+"\n目标词和句子已写回间隔复习，明天的智能计划会继续根据结果调度。 ");feedback.setTextColor(ContextCompat.getColor(requireContext(),pass?R.color.success:R.color.error));feedback.setVisibility(View.VISIBLE);primary.setEnabled(false);mic.setEnabled(false);input.setEnabled(false);stage=4;bar.setProgress(100);next.setText("换一篇继续巩固");next.setVisibility(View.VISIBLE);step.setText("今日弱词微短文完成 ✅");}

    private int retellScore(String text){Set<Integer> hits=targetHits(text);int targetCount=Math.max(1,story.targetWords.size()),coverage=(int)Math.round(hits.size()*100.0/targetCount);int tokens=countWords(text);int expected=Math.max(8,(int)Math.round(story.wordCount*0.34));int length=Math.min(100,(int)Math.round(tokens*100.0/expected));return Math.max(0,Math.min(100,(int)Math.round(coverage*0.72+length*0.28)));}
    private Set<Integer> targetHits(String text){Set<Integer> out=new HashSet<>();Matcher m=TOKEN.matcher(text==null?"":text);while(m.find()){Word seen=words.lookupSurface(m.group());if(seen==null)continue;for(Word target:story.targetWords)if(sameLexeme(seen,target))out.add(target.id);}return out;}
    private boolean sameLexeme(Word a,Word b){if(a==null||b==null)return false;if(a.id==b.id)return true;String la=(a.lemma==null||a.lemma.trim().isEmpty()?a.word:a.lemma),lb=(b.lemma==null||b.lemma.trim().isEmpty()?b.word:b.lemma);return la!=null&&lb!=null&&!la.trim().isEmpty()&&la.trim().equalsIgnoreCase(lb.trim());}
    private int countWords(String text){int n=0;Matcher m=TOKEN.matcher(text==null?"":text);while(m.find())n++;return n;}

    private void finishView(){body.setText(story.chinese);instruction.setText("今天这篇已经完成。可以换一篇继续巩固，或者返回练习中心。 ");input.setVisibility(View.GONE);mic.setVisibility(View.GONE);slow.setVisibility(View.GONE);normal.setVisibility(View.GONE);toggleTranslation.setVisibility(View.GONE);primary.setVisibility(View.GONE);next.setVisibility(View.VISIBLE);next.setText("换一篇继续巩固");bar.setProgress(100);}

    @Override public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,@NonNull int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==REQ_AUDIO&&grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)startSpeech();}
    @Override public void onDestroyView(){if(recognizer!=null){recognizer.destroy();recognizer=null;}if(audio!=null)audio.release();super.onDestroyView();}
}
