package com.italiano2774.nativeapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * v4.6.0 Shadowing training room.
 * Three passes deliberately remove support: bilingual -> Chinese only -> no prompt.
 * The learner can record/play back every pass, then use local Android speech recognition
 * for a word-level content comparison. Only the final blind recall is written to the
 * daily Shadowing result, sentence FSRS and speaking mastery.
 */
public class ShadowingFragment extends Fragment {
    private static final int REQ_AUDIO=771;
    private static final int ACTION_NONE=0,ACTION_SPEECH=1,ACTION_RECORD=2;
    private static final int SESSION_SIZE=5;
    private static final int[] PASS_LINE={60,72,82};

    private static class Line {
        final String it,zh,topic;
        Line(String i,String z,String t){it=i;zh=z;topic=t;}
    }

    private final List<Line> allLines=new ArrayList<>(),session=new ArrayList<>();
    private int index=0,pass=0,batch=0,pendingAudioAction=ACTION_NONE,sessionFinalCount=0,sessionFinalScore=0;
    private final boolean[] scoredPass={false,false,false};
    private final int[] passBest={0,0,0};
    private boolean standardPlayed=false,recording=false,finalized=false;
    private float rate=1.0f;private long started=0L;

    private TextView stats,passText,it,zh,result,recordHint;
    private MaterialButton playStandard,record,recordPlay,speech,next;
    private AudioPlayer audio;private ProgressStore progress;private WordRepository words;
    private SpeechRecognizer recognizer;private MediaRecorder recorder;private MediaPlayer recordedPlayer;private File recordedFile;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_shadowing,parent,false);
        progress=new ProgressStore(requireContext());words=WordRepository.get(requireContext());audio=new AudioPlayer(requireContext(),progress);
        stats=v.findViewById(R.id.text_shadow_stats);passText=v.findViewById(R.id.text_shadow_stage);it=v.findViewById(R.id.text_shadow_it);zh=v.findViewById(R.id.text_shadow_zh);result=v.findViewById(R.id.text_shadow_result);recordHint=v.findViewById(R.id.text_shadow_record_hint);
        playStandard=v.findViewById(R.id.button_shadow_stage);record=v.findViewById(R.id.button_shadow_record);recordPlay=v.findViewById(R.id.button_shadow_record_play);speech=v.findViewById(R.id.button_shadow_speech);next=v.findViewById(R.id.button_shadow_next);
        buildAllLines();buildSession();
        MaterialButtonToggleGroup speeds=v.findViewById(R.id.group_shadow_speed);speeds.check(R.id.button_speed_normal);speeds.addOnButtonCheckedListener((g,id,checked)->{if(!checked)return;rate=id==R.id.button_speed_slow?0.70f:(id==R.id.button_speed_mid?0.85f:1.0f);});
        v.findViewById(R.id.button_shadow_back).setOnClickListener(x->((MainActivity)requireActivity()).openPractice());
        playStandard.setOnClickListener(x->playStandard());record.setOnClickListener(x->requestOrToggleRecording());recordPlay.setOnClickListener(x->playRecording());speech.setOnClickListener(x->requestOrStartSpeech());next.setOnClickListener(x->advance());
        render();return v;
    }

    private void buildAllLines(){
        allLines.clear();for(Scenario s:ScenarioRepository.get(requireContext()).all())for(ScenarioPhrase p:s.phrases)if(p.it!=null&&!p.it.trim().isEmpty())allLines.add(new Line(p.it.trim(),p.zh==null?"":p.zh.trim(),s.title));
        if(allLines.isEmpty())allLines.add(new Line("Vorrei un caffè, per favore.","我想要一杯咖啡，谢谢。","基础交流"));
    }
    private void buildSession(){
        session.clear();List<Line> copy=new ArrayList<>(allLines);long seed=LocalDate.now().toEpochDay()*1009L+batch*7919L;Collections.shuffle(copy,new Random(seed));for(int i=0;i<Math.min(SESSION_SIZE,copy.size());i++)session.add(copy.get(i));index=0;pass=0;sessionFinalCount=0;sessionFinalScore=0;resetPassState();
    }
    private Line current(){return session.get(Math.max(0,Math.min(index,session.size()-1)));}

    private void render(){
        if(session.isEmpty())return;Line l=current();it.setText(l.it);zh.setText(l.zh);result.setText("");result.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));
        stats.setText("今日训练 "+(index+1)+" / "+session.size()+" · 历史裸说正确率 "+progress.auxiliaryAccuracy("shadowing")+"% · "+l.topic);
        if(pass==0){passText.setText("第1遍 · 看双语跟读");it.setVisibility(View.VISIBLE);zh.setVisibility(View.VISIBLE);recordHint.setText("先听标准句，再跟着说。建议录下自己并回听一次。评分线 "+PASS_LINE[0]+"%。");speech.setText("🎙 跟读并评分");}
        else if(pass==1){passText.setText("第2遍 · 只看中文主动说");it.setVisibility(View.INVISIBLE);zh.setVisibility(View.VISIBLE);recordHint.setText("意大利语已经隐藏。只看中文把整句说出来，再看逐词差异。评分线 "+PASS_LINE[1]+"%。");speech.setText("🎙 不看意语说并评分");}
        else{passText.setText("第3遍 · 裸说迁移");it.setVisibility(View.INVISIBLE);zh.setVisibility(View.INVISIBLE);recordHint.setText("中文和意大利语都隐藏。直接复述刚才整句；这一遍才计入今日 Shadowing 成绩。评分线 "+PASS_LINE[2]+"%。");speech.setText("🎙 裸说并最终评分");}
        playStandard.setText(pass==0?"🔊 播放标准句":"🔊 需要时再听标准句");record.setText(recording?"■ 停止录音":"● 录下自己");recordPlay.setEnabled(recordedFile!=null&&recordedFile.exists()&&recordedFile.length()>0);
        next.setText(pass<2?"进入第"+(pass+2)+"遍 →":(index+1<session.size()?"下一句 →":"完成本组"));next.setEnabled(pass==0?standardPlayed:(pass==1?scoredPass[1]:finalized));
        if(scoredPass[pass])result.setText("本遍最好匹配 "+passBest[pass]+"% · "+(passBest[pass]>=PASS_LINE[pass]?"达到本遍目标":"建议再说一次后继续"));
    }

    private void playStandard(){standardPlayed=true;audio.speak(current().it,rate);if(pass==0)next.setEnabled(true);}

    private void advance(){
        if(recording)stopRecordingQuietly();
        if(pass==0&&!standardPlayed&&!scoredPass[0]){result.setText("先听一遍标准句，再进入第2遍。 ");return;}if(pass==1&&!scoredPass[1]){result.setText("第2遍要先只看中文主动说一次，再进入裸说。 ");return;}if(pass<2){pass++;standardPlayed=false;cleanupAttemptMedia();render();return;}
        if(!finalized){result.setText("先完成第3遍裸说评分；如果设备没有系统语音识别，点击评分按钮可使用自评完成。 ");return;}
        if(index+1<session.size()){index++;pass=0;resetPassState();render();return;}
        showSessionResult();
    }

    private void showSessionResult(){
        int avg=sessionFinalCount==0?0:(int)Math.round(sessionFinalScore/(double)sessionFinalCount);stats.setText("本组完成 ✅ · "+sessionFinalCount+"句完成裸说 · 平均匹配 "+avg+"% · 历史正确率 "+progress.auxiliaryAccuracy("shadowing")+"%");passText.setText("Shadowing 三遍训练完成");it.setVisibility(View.VISIBLE);zh.setVisibility(View.VISIBLE);it.setText("从“跟得上”练到“离开字幕也能说”");zh.setText("失败句已经进入个人错句本；所有最终裸说结果都会进入句子间隔复习。 ");recordHint.setText("建议每天只练一小组，第二天再回来，比一次刷很多句更有效。 ");result.setText("第1遍双语跟读 → 第2遍中文提取 → 第3遍裸说迁移\n本组平均裸说匹配："+avg+"%");result.setTextColor(ContextCompat.getColor(requireContext(),avg>=82?R.color.success:R.color.text_secondary));playStandard.setVisibility(View.GONE);record.setVisibility(View.GONE);recordPlay.setVisibility(View.GONE);speech.setVisibility(View.GONE);next.setText("再练5句");next.setEnabled(true);next.setOnClickListener(x->{batch++;playStandard.setVisibility(View.VISIBLE);record.setVisibility(View.VISIBLE);recordPlay.setVisibility(View.VISIBLE);speech.setVisibility(View.VISIBLE);next.setOnClickListener(y->advance());buildSession();render();});
    }

    private void resetPassState(){for(int i=0;i<3;i++){scoredPass[i]=false;passBest[i]=0;}standardPlayed=false;finalized=false;cleanupAttemptMedia();}
    private void cleanupAttemptMedia(){stopRecordingQuietly();cleanupRecordedPlayer();if(recordedFile!=null&&recordedFile.exists())recordedFile.delete();recordedFile=null;recordPlay.setEnabled(false);}

    private void requestOrStartSpeech(){
        if(recording)stopRecordingQuietly();
        if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){pendingAudioAction=ACTION_SPEECH;requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return;}
        startSpeech();
    }
    private void startSpeech(){
        if(!SpeechRecognizer.isRecognitionAvailable(requireContext())){selfAssessFallback();return;}
        if(recognizer!=null)recognizer.destroy();recognizer=SpeechRecognizer.createSpeechRecognizer(requireContext());recognizer.setRecognitionListener(new RecognitionListener(){
            public void onReadyForSpeech(Bundle b){speech.setText("正在听…");started=System.currentTimeMillis();}
            public void onBeginningOfSpeech(){}public void onRmsChanged(float r){}public void onBufferReceived(byte[] b){}
            public void onEndOfSpeech(){speech.setText("正在识别…");}
            public void onError(int e){speech.setText(pass==2?"🎙 裸说并最终评分":"🎙 再说一次");result.setText("没有识别清楚。请靠近麦克风、放慢一点再说；也可以先录音回听。 ");}
            public void onResults(Bundle b){ArrayList<String> xs=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);scoreHypotheses(xs);}
            public void onPartialResults(Bundle b){}public void onEvent(int t,Bundle b){}
        });
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"it-IT");i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);started=System.currentTimeMillis();recognizer.startListening(i);
    }

    private void scoreHypotheses(List<String> xs){
        Line l=current();String best="";ErrorCauseAnalyzer.SentenceAnalysis bestAnalysis=null;int bestScore=-1;
        if(xs!=null)for(String h:xs){ErrorCauseAnalyzer.SentenceAnalysis a=ErrorCauseAnalyzer.analyzeSentence(l.it,h,words);if(a.score>bestScore){bestScore=a.score;best=h;bestAnalysis=a;}}
        if(bestAnalysis==null){bestAnalysis=ErrorCauseAnalyzer.analyzeSentence(l.it,"",words);bestScore=0;}
        long ms=Math.max(1,System.currentTimeMillis()-started);scoredPass[pass]=true;passBest[pass]=Math.max(passBest[pass],bestScore);boolean ok=bestScore>=PASS_LINE[pass];
        if(pass<2)progress.recordAuxiliarySubskill("shadowing_pass"+(pass+1),ok,ms);else finalizeSentence(ok,bestScore,best,bestAnalysis,ms);
        String header=(ok?"✓ 达到本遍目标":"△ 继续练一次")+" · 内容匹配 "+bestScore+"%";
        result.setText(header+"\n识别到："+(best.isEmpty()?"（未识别到完整内容）":best)+"\n问题："+bestAnalysis.summary()+"\n逐词："+bestAnalysis.diff+"\n标准："+l.it);
        result.setTextColor(ContextCompat.getColor(requireContext(),ok?R.color.success:R.color.error));speech.setText(pass==2?"🎙 再裸说一次":"🎙 再说一次");next.setEnabled(pass<2||finalized);
    }

    private void selfAssessFallback(){
        scoredPass[pass]=true;passBest[pass]=Math.max(passBest[pass],PASS_LINE[pass]);result.setText("这台设备没有可用的系统意大利语语音识别。已切换为自评：请先录音并回听；确认自己完整说出后再进入下一遍。 ");result.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));
        if(pass==2){ErrorCauseAnalyzer.SentenceAnalysis a=ErrorCauseAnalyzer.analyzeSentence(current().it,current().it,words);finalizeSentence(true,PASS_LINE[pass],"（设备无识别服务 · 自评完成）",a,1000L);}
        else progress.recordAuxiliarySubskill("shadowing_pass"+(pass+1),true,0L);next.setEnabled(true);
    }

    private void finalizeSentence(boolean ok,int score,String heard,ErrorCauseAnalyzer.SentenceAnalysis analysis,long ms){
        if(!finalized){finalized=true;sessionFinalCount++;sessionFinalScore+=score;progress.recordAuxiliaryResult("shadowing",ok,ms);SentenceFsrsRepository.recordDimension(requireContext(),"Shadowing训练室",current().it,current().zh,SentenceFsrsRepository.DIM_SPEAKING,ok,score,null);recordSentenceWords(ok,ms);if(!ok)progress.recordErrorCause(ErrorCause.PRONUNCIATION,0,"shadowing",current().it,heard,"裸说匹配="+score+" · "+analysis.summary()+" · "+analysis.diff);}
    }
    private void recordSentenceWords(boolean ok,long ms){
        Set<Integer> used=new LinkedHashSet<>();String clean=ErrorCauseAnalyzer.basic(current().it);if(clean.isEmpty())return;for(String token:clean.split(" ")){Word w=words.lookupSurface(token);if(w==null||w.id<=0||!used.add(w.id))continue;progress.recordEmbeddedDimensionResult(w.id,ProgressStore.DIM_SPEAKING,ok,ms);if(used.size()>=3)break;}
    }

    private void requestOrToggleRecording(){
        if(recording){stopRecordingQuietly();return;}
        if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){pendingAudioAction=ACTION_RECORD;requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return;}
        startRecording();
    }
    private void startRecording(){
        try{cleanupRecordedPlayer();recordedFile=new File(requireContext().getCacheDir(),"shadowing_last.m4a");if(recordedFile.exists())recordedFile.delete();recorder=new MediaRecorder();recorder.setAudioSource(MediaRecorder.AudioSource.MIC);recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);recorder.setAudioEncodingBitRate(96000);recorder.setAudioSamplingRate(44100);recorder.setOutputFile(recordedFile.getAbsolutePath());recorder.prepare();recorder.start();recording=true;record.setText("■ 停止录音");recordPlay.setEnabled(false);recordHint.setText("正在录音… 说完后停止，再点“回放自己”与标准句比较。 ");}
        catch(Exception e){stopRecordingQuietly();result.setText("录音启动失败，但系统语音评分仍可正常使用。 ");}
    }
    private void stopRecordingQuietly(){
        if(recorder!=null){try{if(recording)recorder.stop();}catch(Exception ignored){}try{recorder.release();}catch(Exception ignored){}recorder=null;}
        if(recording){recording=false;record.setText("● 重新录音");recordPlay.setEnabled(recordedFile!=null&&recordedFile.exists()&&recordedFile.length()>0);}
    }
    private void playRecording(){
        if(recordedFile==null||!recordedFile.exists()||recordedFile.length()==0){result.setText("先录下自己，再回放。 ");return;}
        try{cleanupRecordedPlayer();recordedPlayer=new MediaPlayer();recordedPlayer.setDataSource(recordedFile.getAbsolutePath());recordedPlayer.setOnPreparedListener(MediaPlayer::start);recordedPlayer.prepareAsync();}
        catch(Exception e){cleanupRecordedPlayer();result.setText("暂时无法回放这次录音，请重新录一次。 ");}
    }
    private void cleanupRecordedPlayer(){if(recordedPlayer!=null){try{recordedPlayer.stop();}catch(Exception ignored){}try{recordedPlayer.release();}catch(Exception ignored){}recordedPlayer=null;}}

    @Override public void onRequestPermissionsResult(int r,@NonNull String[] p,@NonNull int[] g){super.onRequestPermissionsResult(r,p,g);if(r!=REQ_AUDIO)return;if(g.length==0||g[0]!=PackageManager.PERMISSION_GRANTED){int deniedAction=pendingAudioAction;pendingAudioAction=ACTION_NONE;if(deniedAction==ACTION_SPEECH){selfAssessFallback();return;}result.setText("没有麦克风权限，无法录音回放。标准音频和无麦克风自评仍可继续。 ");return;}int action=pendingAudioAction;pendingAudioAction=ACTION_NONE;if(action==ACTION_RECORD)startRecording();else if(action==ACTION_SPEECH)startSpeech();}
    @Override public void onDestroyView(){cleanupAttemptMedia();if(recognizer!=null){recognizer.destroy();recognizer=null;}if(audio!=null)audio.release();super.onDestroyView();}
}
