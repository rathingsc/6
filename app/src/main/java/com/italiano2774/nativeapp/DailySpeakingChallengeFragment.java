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
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** v4.0.0: Chinese prompt -> speak first -> local score -> reveal + audio. */
public class DailySpeakingChallengeFragment extends Fragment {
    private static final int REQ_AUDIO=794;
    private ProgressStore progress;private WordRepository words;private DailySpeakingChallengeEngine engine;private AudioPlayer audio;private SpeechRecognizer recognizer;
    private List<CoreSentence> items=new ArrayList<>();private int index=0,correct=0;private CoreSentence current;private long startedAt;
    private TextView summary,step,level,theme,prompt,hint,recognized,answer,feedback;private EditText typed;private ProgressBar bar;private MaterialButton mic,check,hintButton,reveal,play,slow,next;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_daily_speaking,parent,false);progress=new ProgressStore(requireContext());words=WordRepository.get(requireContext());engine=new DailySpeakingChallengeEngine(requireContext(),progress);audio=new AudioPlayer(requireContext(),progress);
        summary=v.findViewById(R.id.text_speak5_summary);step=v.findViewById(R.id.text_speak5_step);level=v.findViewById(R.id.text_speak5_level);theme=v.findViewById(R.id.text_speak5_theme);prompt=v.findViewById(R.id.text_speak5_prompt);hint=v.findViewById(R.id.text_speak5_hint);recognized=v.findViewById(R.id.text_speak5_recognized);answer=v.findViewById(R.id.text_speak5_answer);feedback=v.findViewById(R.id.text_speak5_feedback);typed=v.findViewById(R.id.edit_speak5_answer);bar=v.findViewById(R.id.progress_speak5);mic=v.findViewById(R.id.button_speak5_mic);check=v.findViewById(R.id.button_speak5_check);hintButton=v.findViewById(R.id.button_speak5_hint);reveal=v.findViewById(R.id.button_speak5_reveal);play=v.findViewById(R.id.button_speak5_play);slow=v.findViewById(R.id.button_speak5_slow);next=v.findViewById(R.id.button_speak5_next);
        v.findViewById(R.id.button_speak5_back).setOnClickListener(x->((MainActivity)requireActivity()).openPractice());mic.setOnClickListener(x->requestOrStartSpeech());check.setOnClickListener(x->scoreTyped());hintButton.setOnClickListener(x->hint.setVisibility(hint.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));reveal.setOnClickListener(x->reveal());play.setOnClickListener(x->{if(current!=null)audio.speak(current.italian,1.0f);});slow.setOnClickListener(x->{if(current!=null)audio.speak(current.italian,0.70f);});next.setOnClickListener(x->{index++;show();});
        int already=progress.dailyAuxiliaryAttempts("daily_speaking",LocalDate.now());items=engine.todayBatch();if(already>=DailySpeakingChallengeEngine.DAILY_TARGET){index=items.size();finish();}else{index=Math.min(Math.floorMod(already,DailySpeakingChallengeEngine.DAILY_TARGET),Math.max(0,items.size()-1));show();}return v;
    }

    private void show(){
        if(items.isEmpty()){prompt.setText("暂无可用核心句");disableInput();return;}
        if(index>=items.size()){finish();return;}current=items.get(index);int today=Math.min(DailySpeakingChallengeEngine.DAILY_TARGET,progress.dailyAuxiliaryAttempts("daily_speaking",LocalDate.now()));summary.setText("今天 "+today+" / 5 · 连续开口 "+progress.dailySpeakingStreak()+" 天 · 历史正确率 "+progress.auxiliaryAccuracy("daily_speaking")+"%");step.setText("第 "+(index+1)+" / "+items.size()+" 句");bar.setProgress((int)Math.round(index*100.0/Math.max(1,items.size())));level.setText(engine.targetLevel()+" · 通过线 "+engine.passScore()+"%");theme.setText("今日主题："+current.category);prompt.setText(current.chinese);hint.setText(current.note==null||current.note.trim().isEmpty()?"先不要看答案，完整说出一句意大利语。":"提示可参考："+current.note);hint.setVisibility(View.GONE);recognized.setVisibility(View.GONE);answer.setVisibility(View.GONE);feedback.setText("先说，再看答案。语音内容只由手机系统识别，本页不调用云端 AI。");feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));typed.setText("");typed.setEnabled(true);mic.setEnabled(true);check.setEnabled(true);hintButton.setEnabled(true);reveal.setEnabled(true);play.setVisibility(View.GONE);slow.setVisibility(View.GONE);next.setVisibility(View.GONE);startedAt=System.currentTimeMillis();
    }

    private void requestOrStartSpeech(){if(current==null)return;if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return;}startSpeech();}
    private void startSpeech(){if(!SpeechRecognizer.isRecognitionAvailable(requireContext())){feedback.setText("这台手机暂时没有可用的意大利语语音识别。可以直接在下面输入你想说的句子，再点“检查输入”。");return;}if(recognizer!=null)recognizer.destroy();recognizer=SpeechRecognizer.createSpeechRecognizer(requireContext());recognizer.setRecognitionListener(new RecognitionListener(){public void onReadyForSpeech(Bundle b){mic.setText("🎙 正在听…");}public void onBeginningOfSpeech(){}public void onRmsChanged(float r){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){mic.setText("正在识别…");}public void onError(int e){mic.setText("🎙 再说一次");feedback.setText("没有识别清楚。可以放慢一点重说，或者用文字输入。 ");}public void onResults(Bundle b){mic.setText("🎙 说出来");ArrayList<String> xs=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);scoreSpeech(xs);}public void onPartialResults(Bundle b){}public void onEvent(int t,Bundle b){}});Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"it-IT");i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);recognizer.startListening(i);}

    private void scoreSpeech(List<String> xs){if(current==null||!mic.isEnabled())return;String best="";int score=0;if(xs!=null)for(String s:xs){int v=sentenceScore(current.italian,s);if(v>score){score=v;best=s;}}complete(best,score,false);}
    private void scoreTyped(){if(current==null||!check.isEnabled())return;String text=typed.getText()==null?"":typed.getText().toString().trim();if(text.isEmpty()){typed.setError("先输入一句意大利语，或直接使用麦克风");return;}complete(text,sentenceScore(current.italian,text),false);}
    private void reveal(){if(current==null||!reveal.isEnabled())return;complete("（直接查看答案）",0,true);}

    private void complete(String user,int score,boolean gaveUp){boolean ok=!gaveUp&&score>=engine.passScore();long ms=Math.max(1,System.currentTimeMillis()-startedAt);if(ok)correct++;progress.recordAuxiliaryResult("daily_speaking",ok,ms);SentenceFsrsRepository.recordDimension(requireContext(),"每日5句开口",current.italian,current.chinese,SentenceFsrsRepository.DIM_SPEAKING,ok,score,null);recordSentenceWords(ok,ms);if(!ok)progress.recordErrorCause(ErrorCause.PRONUNCIATION,0,"daily_speaking",current.italian,user,"主动开口内容匹配="+score);
        recognized.setText("你说/写："+user);recognized.setVisibility(View.VISIBLE);answer.setText("标准表达：\n"+current.italian);answer.setVisibility(View.VISIBLE);String diagnosis=gaveUp?"这句记为需要巩固。":ErrorCauseAnalyzer.analyzeSentence(current.italian,user,words).summary();feedback.setText((ok?"✓ 表达通过":"△ 这句继续巩固")+" · 内容匹配 "+score+"%\n"+diagnosis+(ok?"":"\n薄弱词和本句口语维度已送回智能复习。"));feedback.setTextColor(ContextCompat.getColor(requireContext(),ok?R.color.success:R.color.error));disableInput();play.setVisibility(View.VISIBLE);slow.setVisibility(View.VISIBLE);next.setVisibility(View.VISIBLE);next.setText(index+1>=items.size()?"查看今天结果":"下一句 →");bar.setProgress((int)Math.round((index+1)*100.0/Math.max(1,items.size())));}

    private void recordSentenceWords(boolean ok,long ms){Set<Integer> ids=new LinkedHashSet<>();for(String token:normalize(current.italian).split(" ")){if(token.isEmpty())continue;Word w=words.lookupSurface(token);if(w!=null&&w.id>0)ids.add(w.id);if(ids.size()>=4)break;}for(Integer id:ids)progress.recordEmbeddedDimensionResult(id,ProgressStore.DIM_SPEAKING,ok,ms);}
    private int sentenceScore(String expected,String actual){return ErrorCauseAnalyzer.analyzeSentence(expected,actual,words).score;}
    private String normalize(String s){return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ITALIAN).replace('’','\'').replaceAll("[^a-z0-9' ]"," ").replaceAll("\\s+"," ").trim();}
    private void disableInput(){typed.setEnabled(false);mic.setEnabled(false);check.setEnabled(false);hintButton.setEnabled(false);reveal.setEnabled(false);}
    private void finish(){int attempts=progress.dailyAuxiliaryAttempts("daily_speaking",LocalDate.now()),cor=progress.dailyAuxiliaryCorrect("daily_speaking",LocalDate.now());bar.setProgress(100);step.setText("今日5句完成 ✅");level.setText(engine.targetLevel()+" 主动输出");theme.setText("今天已经真正开口，而不只是认答案");prompt.setText("今日已完成 "+Math.min(5,attempts)+" / 5 句");hint.setVisibility(View.GONE);recognized.setVisibility(View.GONE);answer.setVisibility(View.GONE);feedback.setText("今日开口正确 "+Math.min(5,cor)+" / 5 · 连续开口 "+progress.dailySpeakingStreak()+" 天。\n错误句已经回流口语维度和句子间隔复习。明天会自动换一组主题。");feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_primary));typed.setVisibility(View.GONE);mic.setVisibility(View.GONE);check.setVisibility(View.GONE);hintButton.setVisibility(View.GONE);reveal.setVisibility(View.GONE);play.setVisibility(View.GONE);slow.setVisibility(View.GONE);next.setVisibility(View.VISIBLE);next.setText("再加练5句");next.setOnClickListener(x->{items=engine.todayBatch();int done=progress.dailyAuxiliaryAttempts("daily_speaking",LocalDate.now());index=Math.min(Math.floorMod(done,DailySpeakingChallengeEngine.DAILY_TARGET),Math.max(0,items.size()-1));typed.setVisibility(View.VISIBLE);mic.setVisibility(View.VISIBLE);check.setVisibility(View.VISIBLE);hintButton.setVisibility(View.VISIBLE);reveal.setVisibility(View.VISIBLE);next.setOnClickListener(y->{index++;show();});show();});}

    @Override public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,@NonNull int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==REQ_AUDIO&&grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)startSpeech();}
    @Override public void onDestroyView(){if(recognizer!=null){recognizer.destroy();recognizer=null;}if(audio!=null)audio.release();super.onDestroyView();}
}
