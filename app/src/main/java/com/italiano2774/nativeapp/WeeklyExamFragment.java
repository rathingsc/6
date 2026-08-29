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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** v4.5.0 seven-active-day practical exam with six-skill diagnosis. */
public class WeeklyExamFragment extends Fragment {
    private static final int REQ_AUDIO=945;
    private ProgressStore progress;private WordRepository words;private WeeklyExamEngine engine;private AudioPlayer audio;private SpeechRecognizer recognizer;
    private final List<WeeklyExamEngine.Item> items=new ArrayList<>();private final List<MaterialButton> choiceButtons=new ArrayList<>();
    private final int[] scoreSum=new int[WeeklyExamEngine.SKILL_KEYS.length],scoreCount=new int[WeeklyExamEngine.SKILL_KEYS.length],baseline=new int[WeeklyExamEngine.SKILL_KEYS.length];
    private int index=0;private long startedAt=0L;private boolean graded=false;
    private View intro,questionPanel,resultPanel,choicePanel,inputPanel;private TextView status,step,skill,prompt,hint,feedback,result;private EditText input;private ProgressBar bar;private MaterialButton replay,mic,submit,giveup,next;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_weekly_exam,parent,false);progress=new ProgressStore(requireContext());words=WordRepository.get(requireContext());engine=new WeeklyExamEngine(requireContext(),words,progress);audio=new AudioPlayer(requireContext(),progress);
        intro=v.findViewById(R.id.panel_week_exam_intro);questionPanel=v.findViewById(R.id.panel_week_exam_question);resultPanel=v.findViewById(R.id.panel_week_exam_result);choicePanel=v.findViewById(R.id.panel_week_exam_choices);inputPanel=v.findViewById(R.id.panel_week_exam_input);status=v.findViewById(R.id.text_week_exam_status);step=v.findViewById(R.id.text_week_exam_step);skill=v.findViewById(R.id.text_week_exam_skill);prompt=v.findViewById(R.id.text_week_exam_prompt);hint=v.findViewById(R.id.text_week_exam_hint);feedback=v.findViewById(R.id.text_week_exam_feedback);result=v.findViewById(R.id.text_week_exam_result);input=v.findViewById(R.id.edit_week_exam_answer);bar=v.findViewById(R.id.progress_week_exam);replay=v.findViewById(R.id.button_week_exam_replay);mic=v.findViewById(R.id.button_week_exam_mic);submit=v.findViewById(R.id.button_week_exam_submit);giveup=v.findViewById(R.id.button_week_exam_giveup);next=v.findViewById(R.id.button_week_exam_next);
        choiceButtons.add(v.findViewById(R.id.button_week_exam_c1));choiceButtons.add(v.findViewById(R.id.button_week_exam_c2));choiceButtons.add(v.findViewById(R.id.button_week_exam_c3));choiceButtons.add(v.findViewById(R.id.button_week_exam_c4));
        v.findViewById(R.id.button_week_exam_start).setOnClickListener(x->startExam());v.findViewById(R.id.button_week_exam_report_intro).setOnClickListener(x->((MainActivity)requireActivity()).openWeeklyReport());v.findViewById(R.id.button_week_exam_report).setOnClickListener(x->((MainActivity)requireActivity()).openWeeklyReport());v.findViewById(R.id.button_week_exam_passport).setOnClickListener(x->((MainActivity)requireActivity()).openMasteryPassport());
        for(MaterialButton b:choiceButtons)b.setOnClickListener(x->gradeChoice(((MaterialButton)x).getText().toString()));submit.setOnClickListener(x->gradeTyped());mic.setOnClickListener(x->requestOrStartSpeech());giveup.setOnClickListener(x->grade("",0,true));next.setOnClickListener(x->{index++;showQuestion();});replay.setOnClickListener(x->playCurrent());
        showIntro();return v;
    }

    private void showIntro(){intro.setVisibility(View.VISIBLE);questionPanel.setVisibility(View.GONE);resultPanel.setVisibility(View.GONE);int active=progress.weeklyExamActiveDaysSinceLast();String due=progress.weeklyExamDue()?"✅ 已到周测时间":"还差 "+Math.max(0,7-active)+" 个活跃学习日";String last=progress.weeklyExamCount()==0?"还没有周测记录":"上次总分 "+progress.lastWeeklyExamScore()+"% · 已完成 "+progress.weeklyExamCount()+" 次";status.setText(due+" · 本轮已累计 "+Math.min(7,active)+" / 7 个活跃日\n目标阶段："+engine.targetLevel()+" 内部学习检查 · "+last+"\n题目来自本地课程与审校句库，可离线完成；你也可以提前手动测一次，完成后会重新开始下一轮7个活跃日计数。");}

    private void startExam(){
        items.clear();items.addAll(engine.build());if(items.size()<WeeklyExamEngine.TOTAL_QUESTIONS){status.setText("当前本地题库不足以生成完整周测，请先继续完成主课程。 ");return;}
        MasteryPassportEngine.Snapshot s=MasteryPassportEngine.build(requireContext(),words,progress);for(int i=0;i<baseline.length;i++){MasteryPassportEngine.Skill x=s.skill(WeeklyExamEngine.SKILL_KEYS[i]);baseline[i]=progress.weeklyCycleBaselineScore(WeeklyExamEngine.SKILL_KEYS[i],x==null?0:x.score);}for(int i=0;i<scoreSum.length;i++){scoreSum[i]=0;scoreCount[i]=0;}index=0;intro.setVisibility(View.GONE);resultPanel.setVisibility(View.GONE);questionPanel.setVisibility(View.VISIBLE);showQuestion();
    }

    private WeeklyExamEngine.Item current(){return index>=0&&index<items.size()?items.get(index):null;}
    private void showQuestion(){WeeklyExamEngine.Item q=current();if(q==null){finishExam();return;}graded=false;step.setText("第 "+(index+1)+" / "+items.size()+" 题");skill.setText(q.skillLabel);prompt.setText(q.prompt);hint.setText(q.hint);bar.setProgress((int)Math.round(index*100.0/Math.max(1,items.size())));feedback.setText("先独立作答。周测不提供中途提示；答错会自动进入对应复习通道。");feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));replay.setVisibility(q.hasAudio()?View.VISIBLE:View.GONE);choicePanel.setVisibility(q.type==WeeklyExamEngine.TYPE_CHOICE?View.VISIBLE:View.GONE);inputPanel.setVisibility(q.type==WeeklyExamEngine.TYPE_CHOICE?View.GONE:View.VISIBLE);input.setText("");input.setEnabled(true);submit.setEnabled(true);mic.setVisibility(q.type==WeeklyExamEngine.TYPE_SPEAK?View.VISIBLE:View.GONE);mic.setEnabled(true);mic.setText("🎙 说出来");giveup.setVisibility(View.VISIBLE);giveup.setEnabled(true);next.setVisibility(View.GONE);for(int i=0;i<choiceButtons.size();i++){MaterialButton b=choiceButtons.get(i);b.setEnabled(true);if(i<q.choices.size()){b.setText(q.choices.get(i));b.setVisibility(View.VISIBLE);}else b.setVisibility(View.GONE);}startedAt=System.currentTimeMillis();if(q.hasAudio())playCurrent();}

    private void playCurrent(){WeeklyExamEngine.Item q=current();if(q==null)return;if(q.word!=null)audio.play(q.word);else if(q.expected!=null&&!q.expected.isEmpty())audio.speak(q.expected,1.0f);}
    private void gradeChoice(String answer){WeeklyExamEngine.Item q=current();if(q==null||graded)return;boolean ok=normalize(answer).equals(normalize(q.expected));grade(answer,ok?100:0,false);}
    private void gradeTyped(){WeeklyExamEngine.Item q=current();if(q==null||graded)return;String answer=input.getText()==null?"":input.getText().toString().trim();if(answer.isEmpty()){input.setError("先输入答案；不会可以点“记为0分”");return;}int score=typedScore(q,answer);grade(answer,score,false);}
    private int typedScore(WeeklyExamEngine.Item q,String answer){if(q==null)return 0;if(MasteryPassportEngine.ACTION_SPEAKING.equals(q.skillKey)||MasteryPassportEngine.ACTION_REAL_USE.equals(q.skillKey)||(MasteryPassportEngine.ACTION_GRAMMAR.equals(q.skillKey)&&wordCount(q.expected)>2))return ErrorCauseAnalyzer.analyzeSentence(q.expected,answer,words).score;return normalize(q.expected).equals(normalize(answer))?100:0;}

    private void grade(String user,int itemScore,boolean gaveUp){
        WeeklyExamEngine.Item q=current();if(q==null||graded)return;graded=true;int threshold=engine.passThreshold(q);boolean ok=!gaveUp&&itemScore>=threshold;int si=WeeklyExamEngine.skillIndex(q.skillKey);scoreSum[si]+=Math.max(0,Math.min(100,itemScore));scoreCount[si]++;long ms=Math.max(1,System.currentTimeMillis()-startedAt);
        progress.recordAuxiliaryResult("weekly_exam",ok,ms);recordLearningEvidence(q,user,itemScore,ok,ms);
        String detail;if(gaveUp)detail="这题按0分记录，正确表达已进入后续复习。";else if(q.type==WeeklyExamEngine.TYPE_CHOICE||itemScore==100)detail=ok?"✓ 正确":"✗ 正确答案："+q.expected;else{ErrorCauseAnalyzer.SentenceAnalysis a=ErrorCauseAnalyzer.analyzeSentence(q.expected,user,words);detail=(ok?"✓ 达到本周表达检查线":"△ 还没有达到检查线")+" · 匹配 "+itemScore+"%\n"+a.summary()+"\n参考表达："+q.expected;}
        if(!ok&&(q.type==WeeklyExamEngine.TYPE_CHOICE||itemScore==0)&&!gaveUp)detail+="\n正确答案："+q.expected;feedback.setText(detail);feedback.setTextColor(ContextCompat.getColor(requireContext(),ok?R.color.success:R.color.error));input.setEnabled(false);submit.setEnabled(false);mic.setEnabled(false);giveup.setVisibility(View.GONE);for(MaterialButton b:choiceButtons)b.setEnabled(false);next.setVisibility(View.VISIBLE);next.setText(index+1>=items.size()?"查看周测结果":"下一题 →");bar.setProgress((int)Math.round((index+1)*100.0/Math.max(1,items.size())));
    }

    private void recordLearningEvidence(WeeklyExamEngine.Item q,String user,int score,boolean ok,long ms){
        if(q.word!=null){int dim=MasteryPassportEngine.ACTION_LISTENING.equals(q.skillKey)?ProgressStore.DIM_LISTENING:(MasteryPassportEngine.ACTION_SPELLING.equals(q.skillKey)?ProgressStore.DIM_SPELLING:ProgressStore.DIM_MEANING);progress.recordEmbeddedDimensionResult(q.word.id,dim,ok,ms);if(!ok){String cause=MasteryPassportEngine.ACTION_LISTENING.equals(q.skillKey)?ErrorCause.LISTENING_CONFUSION:(MasteryPassportEngine.ACTION_SPELLING.equals(q.skillKey)?ErrorCauseAnalyzer.forTypedWord(q.expected,user,q.word,words):ErrorCause.MEANING_CONFUSION);progress.recordErrorCause(cause,q.word.id,"weekly_exam",q.expected,user,"每周实战考试 · "+q.skillLabel);}}
        if(MasteryPassportEngine.ACTION_GRAMMAR.equals(q.skillKey)){progress.recordGrammarResult(q.patternId==null||q.patternId.isEmpty()?"weekly_grammar":q.patternId,ok,ms);if(!ok)progress.recordErrorCause(ErrorCause.GRAMMAR,0,"weekly_exam",q.expected,user,"周测语法 · 匹配="+score);}
        if(MasteryPassportEngine.ACTION_SPEAKING.equals(q.skillKey)||MasteryPassportEngine.ACTION_REAL_USE.equals(q.skillKey)){ErrorCauseAnalyzer.SentenceAnalysis a=ErrorCauseAnalyzer.analyzeSentence(q.expected,user,words);int dim=MasteryPassportEngine.ACTION_SPEAKING.equals(q.skillKey)?SentenceFsrsRepository.DIM_SPEAKING:SentenceFsrsRepository.DIM_RECALL;SentenceFsrsRepository.recordDimension(requireContext(),"每周实战考试 · "+q.skillLabel,q.expected,q.prompt,dim,ok,score,null);if(!ok)progress.recordErrorCause(a.primaryCause(),0,"weekly_exam",q.expected,user,"周测"+q.skillLabel+" · 匹配="+score+" · "+a.summary());}
    }

    private void requestOrStartSpeech(){if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return;}startSpeech();}
    private void startSpeech(){WeeklyExamEngine.Item q=current();if(q==null||graded)return;if(!SpeechRecognizer.isRecognitionAvailable(requireContext())){feedback.setText("这台手机没有可用的意大利语语音识别。请直接输入你想说的句子再提交。");return;}if(recognizer!=null)recognizer.destroy();recognizer=SpeechRecognizer.createSpeechRecognizer(requireContext());recognizer.setRecognitionListener(new RecognitionListener(){public void onReadyForSpeech(Bundle b){mic.setText("🎙 正在听…");}public void onBeginningOfSpeech(){}public void onRmsChanged(float r){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){mic.setText("正在识别…");}public void onError(int e){mic.setText("🎙 再说一次");feedback.setText("没有识别清楚，可以重说或直接输入。 ");}public void onResults(Bundle b){mic.setText("🎙 说出来");ArrayList<String> xs=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);scoreSpeech(xs);}public void onPartialResults(Bundle b){}public void onEvent(int t,Bundle b){}});Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"it-IT");i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);recognizer.startListening(i);}
    private void scoreSpeech(List<String> xs){WeeklyExamEngine.Item q=current();if(q==null||graded)return;String best="";int score=-1;if(xs!=null)for(String s:xs){int v=typedScore(q,s);if(v>score){score=v;best=s;}}input.setText(best);grade(best,Math.max(0,score),false);}

    private void finishExam(){
        questionPanel.setVisibility(View.GONE);resultPanel.setVisibility(View.VISIBLE);int[] pct=new int[scoreSum.length];int overall=0;for(int i=0;i<pct.length;i++){pct[i]=scoreCount[i]==0?0:(int)Math.round(scoreSum[i]*1.0/scoreCount[i]);overall+=pct[i];}overall=(int)Math.round(overall*1.0/pct.length);
        MasteryPassportEngine.Snapshot post=MasteryPassportEngine.build(requireContext(),words,progress);int[] after=new int[pct.length];for(int i=0;i<after.length;i++){MasteryPassportEngine.Skill s=post.skill(WeeklyExamEngine.SKILL_KEYS[i]);after[i]=s==null?0:s.score;}progress.saveWeeklyExamResult(overall,pct,baseline,after);
        StringBuilder sb=new StringBuilder();sb.append("本周实战总分：").append(overall).append("%\n").append(overall>=80?"✅ 整体稳定，下一周继续推进并精准补短板。":(overall>=65?"△ 基础可以继续推进，但下周会自动加强弱项。":"🩹 本周漏洞较多，下周已自动减新词并提高补弱优先级。"));sb.append("\n\n六项实战成绩\n");for(int i=0;i<pct.length;i++){String key=WeeklyExamEngine.SKILL_KEYS[i];int delta=after[i]-baseline[i];sb.append(WeeklyExamEngine.skillLabel(key)).append(" ").append(pct[i]).append("% · 能力护照 ").append(baseline[i]).append(" → ").append(after[i]).append(delta==0?"":" ("+(delta>0?"+":"")+delta+")").append("\n");}sb.append("\n下周自动重点：").append(WeeklyExamEngine.skillLabel(progress.weeklyFocusPrimary())).append(" + ").append(WeeklyExamEngine.skillLabel(progress.weeklyFocusSecondary())).append("\n").append(progress.weeklyAdjustmentSummary()).append("\n\n答错的词会回到对应四维复习；错误句会进入个人错句本和句子FSRS。\n说明：这是App内部学习诊断，不是官方CEFR考试。 ");result.setText(sb.toString());status.setText("周测完成 · 下一轮从今天之后重新累计7个活跃学习日");
    }

    private String normalize(String s){return ErrorCauseAnalyzer.basic(s).replaceAll("\\s+"," ").trim();}
    private int wordCount(String s){String n=normalize(s);return n.isEmpty()?0:n.split("\\s+").length;}
    @Override public void onRequestPermissionsResult(int req,@NonNull String[] permissions,@NonNull int[] grants){super.onRequestPermissionsResult(req,permissions,grants);if(req==REQ_AUDIO&&grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)startSpeech();}
    @Override public void onDestroyView(){if(recognizer!=null)recognizer.destroy();if(audio!=null)audio.release();super.onDestroyView();}
}
