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
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import java.io.File;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * v3.4.0 beginner listening + speaking bridge.
 * It reuses the 2000-word route and the ten-article aligned sentences instead of
 * introducing a separate disconnected content library.
 */
public class ListeningSpeakingFragment extends Fragment {
    private static final int REQ_AUDIO=790;
    private static final int MODE_MIXED=0,MODE_SENTENCE_LISTEN=1,MODE_WORD_LISTEN=2,MODE_REPEAT=3,MODE_ZH_SPEAK=4;
    private static final int TOTAL=10;
    private static final int ACTION_NONE=0,ACTION_SPEECH=1,ACTION_RECORD=2;

    private static class SentenceCard {
        String italian,chinese,source;
        final List<Integer> wordIds=new ArrayList<>();
        SentenceCard(String it,String zh,String s){italian=it;chinese=zh;source=s;}
    }

    private final Random random=new Random();
    private final List<SentenceCard> sentences=new ArrayList<>();
    private final List<Word> listeningWords=new ArrayList<>();
    private final Set<Integer> weakWordIds=new LinkedHashSet<>();
    private ProgressStore progress;private WordRepository words;private AudioPlayer audio;
    private Spinner modeSpinner;private TextView summary,progressText,modeText,prompt,subtitle,feedback;private ProgressBar progressBar;
    private LinearLayout choices,speaking,audioControls;private MaterialButton play,slow,speech,record,recordPlay,next,reviewWrong;
    private final MaterialButton[] optionButtons=new MaterialButton[4];
    private int selectedMode=MODE_MIXED,currentMode=MODE_SENTENCE_LISTEN,questionIndex=0,correctCount=0,pendingAudioAction=ACTION_NONE;
    private boolean answered=false,recording=false;private long startedAt=0L;
    private SentenceCard currentSentence;private Word currentWord;private String correctOption="";
    private SpeechRecognizer recognizer;private MediaRecorder recorder;private MediaPlayer recordedPlayer;private File recordedFile;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_listening_speaking,parent,false);progress=new ProgressStore(requireContext());words=WordRepository.get(requireContext());audio=new AudioPlayer(requireContext(),progress);
        modeSpinner=v.findViewById(R.id.spinner_ls_mode);summary=v.findViewById(R.id.text_ls_summary);progressText=v.findViewById(R.id.text_ls_progress);progressBar=v.findViewById(R.id.progress_ls);modeText=v.findViewById(R.id.text_ls_mode);prompt=v.findViewById(R.id.text_ls_prompt);subtitle=v.findViewById(R.id.text_ls_subtitle);feedback=v.findViewById(R.id.text_ls_feedback);choices=v.findViewById(R.id.container_ls_choices);speaking=v.findViewById(R.id.container_ls_speaking);audioControls=v.findViewById(R.id.container_ls_audio);play=v.findViewById(R.id.button_ls_play);slow=v.findViewById(R.id.button_ls_slow);speech=v.findViewById(R.id.button_ls_speech);record=v.findViewById(R.id.button_ls_record);recordPlay=v.findViewById(R.id.button_ls_record_play);next=v.findViewById(R.id.button_ls_next);reviewWrong=v.findViewById(R.id.button_ls_review_wrong);
        optionButtons[0]=v.findViewById(R.id.button_ls_option1);optionButtons[1]=v.findViewById(R.id.button_ls_option2);optionButtons[2]=v.findViewById(R.id.button_ls_option3);optionButtons[3]=v.findViewById(R.id.button_ls_option4);
        buildPools();ArrayAdapter<String> adapter=new ArrayAdapter<>(requireContext(),R.layout.item_spinner_text,new String[]{"综合10题 · 推荐","听一句选中文","听单词认拼写","跟读句子","看中文自己说"});adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);modeSpinner.setAdapter(adapter);
        modeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View x,int pos,long id){selectedMode=pos;startSession();}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        play.setOnClickListener(x->playCurrent(false));slow.setOnClickListener(x->playCurrent(true));speech.setOnClickListener(x->requestOrStartSpeech());record.setOnClickListener(x->requestOrToggleRecording());recordPlay.setOnClickListener(x->playRecording());next.setOnClickListener(x->advance());reviewWrong.setOnClickListener(x->openWeakReview());v.findViewById(R.id.button_ls_back).setOnClickListener(x->((MainActivity)requireActivity()).openPractice());
        for(int i=0;i<optionButtons.length;i++){final int idx=i;optionButtons[i].setOnClickListener(x->answerOption(idx));}
        return v;
    }

    private void buildPools(){
        MemoryArticleRepository repo=MemoryArticleRepository.get(requireContext());List<SentenceCard> studied=new ArrayList<>(),fallback=new ArrayList<>();
        for(MemoryArticle a:repo.all())for(MemoryArticleSection s:a.sections){boolean learned=progress.memoryArticleSectionDone(s.id)||progress.memoryArticleSentenceStudyDone(s.id);for(MemoryArticleSentence p:s.sentences){if(p.italian==null||p.chinese==null||p.italian.trim().isEmpty()||p.chinese.trim().isEmpty())continue;SentenceCard c=new SentenceCard(p.italian.trim(),p.chinese.trim(),a.titleZh+" · "+s.titleZh);for(Integer id:s.targetWordIds)if(id!=null&&containsWord(c.italian,wordText(id)))c.wordIds.add(id);for(Integer id:s.reviewWordIds)if(id!=null&&!c.wordIds.contains(id)&&containsWord(c.italian,wordText(id)))c.wordIds.add(id);fallback.add(c);if(learned)studied.add(c);}}
        if(!studied.isEmpty()){sentences.addAll(studied);for(SentenceCard c:fallback){if(sentences.size()>=32)break;if(!sentences.contains(c))sentences.add(c);}}else sentences.addAll(fallback.subList(0,Math.min(32,fallback.size())));
        List<Word> fallbackWords=new ArrayList<>();for(Word w:words.all())if(w.id<=2000&&w.word!=null&&!w.word.trim().isEmpty()&&w.localAudio!=null&&!w.localAudio.trim().isEmpty()){fallbackWords.add(w);if(progress.mastery(w.id)>0||progress.memoryArticleExposureCount(w.id)>0)listeningWords.add(w);}
        if(listeningWords.size()<12)listeningWords.clear();if(listeningWords.isEmpty())listeningWords.addAll(fallbackWords.subList(0,Math.min(120,fallbackWords.size())));
        if(sentences.isEmpty())sentences.add(new SentenceCard("Vorrei un caffè, per favore.","我想要一杯咖啡，谢谢。","基础交流"));
    }

    private String wordText(int id){Word w=words.byId(id);return w==null||w.word==null?"":w.word.trim();}
    private boolean containsWord(String sentence,String word){if(word==null||word.isEmpty())return false;return Pattern.compile("(?iu)(?<![\\p{L}])"+Pattern.quote(word)+"(?![\\p{L}])").matcher(sentence).find();}

    private void startSession(){stopRecordingQuietly();weakWordIds.clear();questionIndex=0;correctCount=0;answered=false;next.setVisibility(View.GONE);reviewWrong.setVisibility(View.GONE);summary.setText("10题一组 · 听力历史 "+progress.auxiliaryAccuracy("listen_speak")+"% · 听错/说错会进入智能复习");showQuestion();}
    private int mixedModeFor(int index){int[] order={MODE_SENTENCE_LISTEN,MODE_WORD_LISTEN,MODE_REPEAT,MODE_ZH_SPEAK,MODE_WORD_LISTEN,MODE_SENTENCE_LISTEN,MODE_ZH_SPEAK,MODE_REPEAT,MODE_SENTENCE_LISTEN,MODE_ZH_SPEAK};return order[Math.floorMod(index,order.length)];}

    private void showQuestion(){
        cleanupRecordedPlayer();stopRecordingQuietly();recordedFile=null;recordPlay.setEnabled(false);record.setText("● 录下自己");answered=false;speech.setEnabled(true);feedback.setText("");next.setVisibility(View.GONE);reviewWrong.setVisibility(View.GONE);currentMode=selectedMode==MODE_MIXED?mixedModeFor(questionIndex):selectedMode;progressText.setText("第 "+(questionIndex+1)+" / "+TOTAL+" 题");progressBar.setProgress((int)Math.round(questionIndex*100.0/TOTAL));startedAt=System.currentTimeMillis();
        if(currentMode==MODE_WORD_LISTEN)showWordListening();else{currentSentence=sentences.get(random.nextInt(sentences.size()));currentWord=null;if(currentMode==MODE_SENTENCE_LISTEN)showSentenceListening();else showSpeaking();}
    }

    private void showSentenceListening(){modeText.setText("听一句 · 选择中文意思");prompt.setText("🔊 先听，不看字幕");subtitle.setText(currentSentence.source+" · 可重复听，也可以用慢速");choices.setVisibility(View.VISIBLE);speaking.setVisibility(View.GONE);audioControls.setVisibility(View.VISIBLE);List<String> opts=new ArrayList<>();opts.add(currentSentence.chinese);while(opts.size()<4){String z=sentences.get(random.nextInt(sentences.size())).chinese;if(!opts.contains(z))opts.add(z);}Collections.shuffle(opts,random);correctOption=currentSentence.chinese;bindOptions(opts);prompt.postDelayed(()->{if(isAdded()&&!answered&&currentMode==MODE_SENTENCE_LISTEN)audio.speak(currentSentence.italian,1.0f);},220);}
    private void showWordListening(){if(listeningWords.isEmpty()){currentMode=MODE_SENTENCE_LISTEN;currentSentence=sentences.get(random.nextInt(sentences.size()));showSentenceListening();return;}currentWord=listeningWords.get(random.nextInt(listeningWords.size()));currentSentence=null;modeText.setText("听单词 · 认出意大利语拼写");prompt.setText("🔊 听声音，选择你听到的单词");subtitle.setText("先用耳朵认词，不提前显示中文");choices.setVisibility(View.VISIBLE);speaking.setVisibility(View.GONE);audioControls.setVisibility(View.VISIBLE);List<String> opts=new ArrayList<>();opts.add(currentWord.word);while(opts.size()<4){Word w=listeningWords.get(random.nextInt(listeningWords.size()));if(!opts.contains(w.word))opts.add(w.word);}Collections.shuffle(opts,random);correctOption=currentWord.word;bindOptions(opts);prompt.postDelayed(()->{if(isAdded()&&!answered&&currentMode==MODE_WORD_LISTEN)audio.play(currentWord,1.0f);},220);}
    private void showSpeaking(){choices.setVisibility(View.GONE);speaking.setVisibility(View.VISIBLE);if(currentMode==MODE_REPEAT){modeText.setText("跟读句子 · 听后模仿");prompt.setText(currentSentence.italian);subtitle.setText(currentSentence.chinese+"\n先听原句，再完整说一遍；系统语音识别用于内容匹配，不代替人工发音评分。");audioControls.setVisibility(View.VISIBLE);prompt.postDelayed(()->{if(isAdded()&&!answered&&currentMode==MODE_REPEAT)audio.speak(currentSentence.italian,1.0f);},220);}else{modeText.setText("主动开口 · 看中文说意大利语");prompt.setText(currentSentence.chinese);subtitle.setText("先自己说，不看意大利语。说完后系统会显示识别结果和标准句。");audioControls.setVisibility(View.GONE);}speech.setText("🎙 说出来并评分");}

    private void bindOptions(List<String> opts){for(int i=0;i<4;i++){optionButtons[i].setText(opts.get(i));optionButtons[i].setEnabled(true);}}
    private void playCurrent(boolean slowRate){if(currentMode==MODE_WORD_LISTEN&&currentWord!=null){audio.play(currentWord,slowRate?0.70f:1.0f);}else if(currentSentence!=null)audio.speak(currentSentence.italian,slowRate?0.70f:1.0f);}

    private void answerOption(int index){if(answered)return;String chosen=String.valueOf(optionButtons[index].getText());boolean ok=chosen.equals(correctOption);long ms=Math.max(1,System.currentTimeMillis()-startedAt);answered=true;if(ok)correctCount++;for(MaterialButton b:optionButtons)b.setEnabled(false);optionButtons[index].setText((ok?"✓ ":"✗ ")+chosen);
        if(currentMode==MODE_WORD_LISTEN&&currentWord!=null){progress.recordEmbeddedDimensionResult(currentWord.id,ProgressStore.DIM_LISTENING,ok,ms);progress.recordAuxiliaryResult("listen_speak",ok,ms);if(!ok){weakWordIds.add(currentWord.id);progress.recordErrorCause(ErrorCause.LISTENING_CONFUSION,currentWord.id,"listen_speak",currentWord.word,chosen,"听音认词");}feedback.setText((ok?"✓ 听对了":"△ 已加入智能复习")+"\n"+SmartReviewModeEngine.studyForm(currentWord)+" = "+currentWord.chinese);}
        else if(currentSentence!=null){progress.recordAuxiliaryResult("listen_speak",ok,ms);SentenceFsrsRepository.recordDimension(requireContext(),"听说训练·十篇通关",currentSentence.italian,currentSentence.chinese,SentenceFsrsRepository.DIM_LISTENING,ok,ok?100:35,null);recordSentenceWords(ProgressStore.DIM_LISTENING,ok,ms);if(!ok)progress.recordErrorCause(ErrorCause.LISTENING_CONFUSION,0,"listen_speak",currentSentence.italian,chosen,"听句选义");feedback.setText((ok?"✓ 听懂了":"△ 再听一次")+"\n"+currentSentence.italian+"\n"+currentSentence.chinese);}
        feedback.setTextColor(ContextCompat.getColor(requireContext(),ok?R.color.success:R.color.error));showNextButton();}

    private void requestOrStartSpeech(){if(answered)return;if(recording)stopRecordingQuietly();if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){pendingAudioAction=ACTION_SPEECH;requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return;}startSpeechRecognition();}
    private void startSpeechRecognition(){if(currentSentence==null)return;if(!SpeechRecognizer.isRecognitionAvailable(requireContext())){feedback.setText("这台设备没有可用的系统意大利语语音识别服务。你仍可用“录下自己”进行回放对比。");return;}if(recognizer!=null)recognizer.destroy();recognizer=SpeechRecognizer.createSpeechRecognizer(requireContext());recognizer.setRecognitionListener(new RecognitionListener(){public void onReadyForSpeech(Bundle b){speech.setText("正在听…");}public void onBeginningOfSpeech(){}public void onRmsChanged(float r){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){speech.setText("正在识别…");}public void onError(int e){speech.setText("🎙 再说一次");feedback.setText("没有识别清楚，请放慢一点再说。");}public void onResults(Bundle b){ArrayList<String> xs=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);scoreSpeech(xs);}public void onPartialResults(Bundle b){}public void onEvent(int t,Bundle b){}});Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"it-IT");i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);startedAt=System.currentTimeMillis();recognizer.startListening(i);}
    private void scoreSpeech(List<String> hypotheses){if(answered||currentSentence==null)return;String best="";int score=0;if(hypotheses!=null)for(String h:hypotheses){int s=similarity(currentSentence.italian,h);if(s>score){score=s;best=h;}}boolean ok=score>=72;long ms=Math.max(1,System.currentTimeMillis()-startedAt);answered=true;if(ok)correctCount++;progress.recordAuxiliaryResult("listen_speak",ok,ms);SentenceFsrsRepository.recordDimension(requireContext(),"听说训练·十篇通关",currentSentence.italian,currentSentence.chinese,SentenceFsrsRepository.DIM_SPEAKING,ok,score,null);recordSentenceWords(ProgressStore.DIM_SPEAKING,ok,ms);if(!ok)progress.recordErrorCause(ErrorCause.PRONUNCIATION,0,"listen_speak",currentSentence.italian,best,"语音识别内容匹配="+score);prompt.setText(currentSentence.italian);subtitle.setText(currentSentence.chinese);audioControls.setVisibility(View.VISIBLE);feedback.setText((ok?"✓ 表达清楚":"△ 继续练一次")+" · 内容匹配 "+score+"%\n识别到："+(best.isEmpty()?"（未识别到完整内容）":best)+"\n标准句："+currentSentence.italian+(ok?"":"\n已把本句薄弱词加入复习队列"));feedback.setTextColor(ContextCompat.getColor(requireContext(),ok?R.color.success:R.color.error));speech.setText("本题已评分");speech.setEnabled(false);showNextButton();}

    private void recordSentenceWords(int dim,boolean ok,long ms){if(currentSentence==null)return;int n=0;for(Integer id:currentSentence.wordIds){if(id==null||id<=0)continue;progress.recordEmbeddedDimensionResult(id,dim,ok,ms);if(!ok)weakWordIds.add(id);if(++n>=3)break;}}
    private void showNextButton(){next.setVisibility(View.VISIBLE);next.setText(questionIndex+1>=TOTAL?"查看本组结果":"下一题 →");}
    private void advance(){if(!answered)return;if(questionIndex+1>=TOTAL){finishSession();return;}questionIndex++;showQuestion();}
    private void finishSession(){stopRecordingQuietly();progressBar.setProgress(100);progressText.setText("本组完成 ✅");modeText.setText("听说训练完成");prompt.setText(correctCount+" / "+TOTAL+" 正确");subtitle.setText("综合正确率 "+(correctCount*10)+"% · 历史听说正确率 "+progress.auxiliaryAccuracy("listen_speak")+"%\n听错或说错的词已经降低对应听力/口语掌握度，并提前进入智能复习。");feedback.setText(weakWordIds.isEmpty()?"这组没有新增薄弱词。":"本组发现 "+weakWordIds.size()+" 个需要继续巩固的词。建议现在复习一次。 ");choices.setVisibility(View.GONE);speaking.setVisibility(View.GONE);audioControls.setVisibility(View.GONE);next.setVisibility(View.GONE);reviewWrong.setVisibility(weakWordIds.isEmpty()?View.GONE:View.VISIBLE);}
    private void openWeakReview(){if(weakWordIds.isEmpty())return;int[] ids=new int[weakWordIds.size()];int i=0;for(Integer id:weakWordIds)ids[i++]=id;((MainActivity)requireActivity()).openMemoryArticleReview(ids,"听说训练薄弱词",Math.min(24,ids.length));}

    private void requestOrToggleRecording(){if(recording){stopRecordingQuietly();return;}if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){pendingAudioAction=ACTION_RECORD;requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return;}startRecording();}
    private void startRecording(){if(currentSentence==null)return;try{cleanupRecordedPlayer();recordedFile=new File(requireContext().getCacheDir(),"listen_speak_last.m4a");if(recordedFile.exists())recordedFile.delete();recorder=new MediaRecorder();recorder.setAudioSource(MediaRecorder.AudioSource.MIC);recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);recorder.setAudioEncodingBitRate(96000);recorder.setAudioSamplingRate(44100);recorder.setOutputFile(recordedFile.getAbsolutePath());recorder.prepare();recorder.start();recording=true;record.setText("■ 停止录音");recordPlay.setEnabled(false);feedback.setText("正在录音。说完后点“停止录音”，再回放和标准音比较。 ");}catch(Exception e){stopRecordingQuietly();feedback.setText("录音启动失败，但仍可使用语音识别评分。 ");}}
    private void stopRecordingQuietly(){if(recorder!=null){try{if(recording)recorder.stop();}catch(Exception ignored){}try{recorder.release();}catch(Exception ignored){}recorder=null;}if(recording){recording=false;record.setText("● 重新录音");recordPlay.setEnabled(recordedFile!=null&&recordedFile.exists()&&recordedFile.length()>0);}}
    private void playRecording(){if(recordedFile==null||!recordedFile.exists())return;try{cleanupRecordedPlayer();recordedPlayer=new MediaPlayer();recordedPlayer.setDataSource(recordedFile.getAbsolutePath());recordedPlayer.setOnPreparedListener(MediaPlayer::start);recordedPlayer.prepareAsync();}catch(Exception e){cleanupRecordedPlayer();feedback.setText("暂时无法回放录音，请重新录一次。 ");}}
    private void cleanupRecordedPlayer(){if(recordedPlayer!=null){try{recordedPlayer.stop();}catch(Exception ignored){}try{recordedPlayer.release();}catch(Exception ignored){}recordedPlayer=null;}}

    private int similarity(String a,String b){String x=normalize(a),y=normalize(b);if(x.isEmpty()||y.isEmpty())return 0;int max=Math.max(x.length(),y.length()),d=editDistance(x,y);return (int)Math.round(Math.max(0,1-d/(double)max)*100);}
    private String normalize(String s){String n=Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ITALIAN).replace('’','\'');return n.replaceAll("[^a-z0-9' ]"," ").replaceAll("\\s+"," ").trim();}
    private int editDistance(String a,String b){int[] prev=new int[b.length()+1],cur=new int[b.length()+1];for(int j=0;j<=b.length();j++)prev[j]=j;for(int i=1;i<=a.length();i++){cur[0]=i;for(int j=1;j<=b.length();j++){int cost=a.charAt(i-1)==b.charAt(j-1)?0:1;cur[j]=Math.min(Math.min(cur[j-1]+1,prev[j]+1),prev[j-1]+cost);}int[] t=prev;prev=cur;cur=t;}return prev[b.length()];}

    @Override public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,@NonNull int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode!=REQ_AUDIO)return;if(grantResults.length==0||grantResults[0]!=PackageManager.PERMISSION_GRANTED){feedback.setText("需要麦克风权限才能跟读评分或录音回放。听力练习仍然可以正常使用。 ");pendingAudioAction=ACTION_NONE;return;}int action=pendingAudioAction;pendingAudioAction=ACTION_NONE;if(action==ACTION_SPEECH)startSpeechRecognition();else if(action==ACTION_RECORD)startRecording();}
    @Override public void onDestroyView(){stopRecordingQuietly();cleanupRecordedPlayer();if(recognizer!=null){recognizer.destroy();recognizer=null;}if(audio!=null)audio.release();super.onDestroyView();}
}
