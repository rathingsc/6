package com.italiano2774.nativeapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * v3.5.1 three-level real-life conversation training.
 * Beginner: listen -> understand -> choose -> speak.
 * Intermediate: listen -> understand -> Chinese prompt -> speak.
 * Advanced: listen only -> free response -> conversation continues.
 */
public class DialogueTrainingFragment extends Fragment {
    private static final int REQ_AUDIO=791;
    private static final int PHASE_LISTEN=0,PHASE_UNDERSTAND=1,PHASE_CHOOSE=2,PHASE_SPEAK=3,PHASE_REPLY=4,PHASE_DONE=5;
    private static final int LEVEL_BEGINNER=1,LEVEL_INTERMEDIATE=2,LEVEL_ADVANCED=3;

    public static DialogueTrainingFragment newInstance(String id){
        DialogueTrainingFragment f=new DialogueTrainingFragment();
        Bundle b=new Bundle();
        b.putString("scenario",id==null?"":id);
        f.setArguments(b);
        return f;
    }
    public static DialogueTrainingFragment newInstance(String id,int level,String lifeTaskId){
        DialogueTrainingFragment f=new DialogueTrainingFragment();Bundle b=new Bundle();b.putString("scenario",id==null?"":id);b.putInt("level",Math.max(1,Math.min(3,level)));b.putString("lifeTask",lifeTaskId==null?"":lifeTaskId);f.setArguments(b);return f;
    }

    private DialogueRepository repo;
    private ProgressStore progress;
    private AudioPlayer audio;
    private WordRepository words;
    private Spinner spinner;
    private MaterialButtonToggleGroup difficultyGroup;
    private TextView stats,path,turnProgress,stage,npc,npcZh,feedback,reply,speakPrompt,target;
    private LinearLayout choiceContainer,speakContainer;
    private final List<MaterialButton> choices=new ArrayList<>();
    private MaterialButton reveal,startAnswer,replay,slow,speak,hint,skipSpeak,next,restart;
    private DialogueScenario current;
    private int turnIndex=0,phase=PHASE_LISTEN,currentLevel=LEVEL_BEGINNER;
    private boolean choiceMistakeThisTurn=false,speechAttemptedThisTurn=false,speechPassedThisTurn=false,sessionFinished=false,turnFinalized=false;
    private int sessionChoiceFirstTry=0,sessionSpeechTurns=0,sessionSpeechPasses=0,sessionScoreTotal=0;
    private int turnSpeechBestScore=0,turnHintLevel=0,speechAttemptsThisTurn=0;
    private long startedAt=0L;
    private String lastRecognized="";
    private String lifeTaskId="";
    private SpeechRecognizer recognizer;
    private final Set<Integer> weakWordIds=new LinkedHashSet<>();

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_dialogue_training,container,false);
        repo=DialogueRepository.get(requireContext());
        progress=new ProgressStore(requireContext());
        audio=new AudioPlayer(requireContext(),progress);
        words=WordRepository.get(requireContext());
        spinner=v.findViewById(R.id.spinner_dialogue);
        difficultyGroup=v.findViewById(R.id.group_dialogue_difficulty);
        stats=v.findViewById(R.id.text_dialogue_stats);
        path=v.findViewById(R.id.text_dialogue_path);
        turnProgress=v.findViewById(R.id.text_dialogue_progress);
        stage=v.findViewById(R.id.text_dialogue_stage);
        npc=v.findViewById(R.id.text_dialogue_npc);
        npcZh=v.findViewById(R.id.text_dialogue_npc_zh);
        feedback=v.findViewById(R.id.text_dialogue_feedback);
        reply=v.findViewById(R.id.text_dialogue_reply);
        speakPrompt=v.findViewById(R.id.text_dialogue_speak_prompt);
        target=v.findViewById(R.id.text_dialogue_target);
        choiceContainer=v.findViewById(R.id.container_dialogue_choices);
        speakContainer=v.findViewById(R.id.container_dialogue_speak);
        reveal=v.findViewById(R.id.button_dialogue_reveal);
        startAnswer=v.findViewById(R.id.button_dialogue_start_answer);
        replay=v.findViewById(R.id.button_dialogue_replay);
        slow=v.findViewById(R.id.button_dialogue_slow);
        speak=v.findViewById(R.id.button_dialogue_speak);
        hint=v.findViewById(R.id.button_dialogue_hint);
        skipSpeak=v.findViewById(R.id.button_dialogue_skip_speak);
        next=v.findViewById(R.id.button_dialogue_next);
        restart=v.findViewById(R.id.button_dialogue_restart);
        choices.add(v.findViewById(R.id.button_dialogue_choice1));
        choices.add(v.findViewById(R.id.button_dialogue_choice2));
        choices.add(v.findViewById(R.id.button_dialogue_choice3));

        lifeTaskId=getArguments()==null?"":getArguments().getString("lifeTask","");
        v.findViewById(R.id.button_dialogue_back).setOnClickListener(x->{MainActivity a=(MainActivity)requireActivity();if(lifeTaskId.isEmpty())a.openScenarios();else a.openLifeTask(lifeTaskId);});
        replay.setOnClickListener(x->playNpc(false));
        slow.setOnClickListener(x->playNpc(true));
        reveal.setOnClickListener(x->revealMeaningOrAnswer());
        startAnswer.setOnClickListener(x->beginAnswer());
        speak.setOnClickListener(x->requestOrStartSpeech());
        hint.setOnClickListener(x->showTargetHint());
        skipSpeak.setOnClickListener(x->showReply(false));
        next.setOnClickListener(x->advance());
        restart.setOnClickListener(x->restartScenario());
        for(int i=0;i<choices.size();i++){
            final int idx=i;
            choices.get(i).setOnClickListener(x->choose(idx));
        }

        difficultyGroup.addOnButtonCheckedListener((group,checkedId,isChecked)->{
            if(!isChecked)return;
            int nextLevel=checkedId==R.id.button_dialogue_level_advanced?LEVEL_ADVANCED:
                    checkedId==R.id.button_dialogue_level_intermediate?LEVEL_INTERMEDIATE:LEVEL_BEGINNER;
            if(nextLevel!=currentLevel){currentLevel=nextLevel;restartScenario();}
        });

        List<String> names=new ArrayList<>();
        for(DialogueScenario d:repo.all())names.add(d.emoji+"  "+d.title+" · "+d.turns.size()+"轮");
        ArrayAdapter<String> adapter=new ArrayAdapter<>(requireContext(),R.layout.item_spinner_text,names);
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> p){}
            public void onItemSelected(android.widget.AdapterView<?> p,View view,int pos,long id){
                if(pos>=0&&pos<repo.all().size()){current=repo.all().get(pos);restartScenario();}
            }
        });
        int requestedLevel=getArguments()==null?LEVEL_BEGINNER:getArguments().getInt("level",LEVEL_BEGINNER);currentLevel=Math.max(LEVEL_BEGINNER,Math.min(LEVEL_ADVANCED,requestedLevel));difficultyGroup.check(currentLevel==LEVEL_ADVANCED?R.id.button_dialogue_level_advanced:(currentLevel==LEVEL_INTERMEDIATE?R.id.button_dialogue_level_intermediate:R.id.button_dialogue_level_beginner));
        String wanted=getArguments()==null?"":getArguments().getString("scenario","");
        if(!wanted.isEmpty())for(int n=0;n<repo.all().size();n++)if(repo.all().get(n).id.equals(wanted)){spinner.setSelection(n);break;}
        if(!lifeTaskId.isEmpty()){
            // A mission launch is a locked exam stage. Do not let the user switch to another
            // scenario/difficulty and accidentally credit that result to the original mission.
            spinner.setEnabled(false);
            v.findViewById(R.id.button_dialogue_level_beginner).setEnabled(false);
            v.findViewById(R.id.button_dialogue_level_intermediate).setEnabled(false);
            v.findViewById(R.id.button_dialogue_level_advanced).setEnabled(false);
        }
        return v;
    }

    private DialogueTurn currentTurn(){
        if(current==null||current.turns.isEmpty())return null;
        return current.turns.get(Math.max(0,Math.min(turnIndex,current.turns.size()-1)));
    }

    private String correctAnswer(DialogueTurn t){
        if(t==null||t.choices.isEmpty())return "";
        return t.choices.get(Math.max(0,Math.min(t.correct,t.choices.size()-1)));
    }

    private String levelLabel(){return currentLevel==LEVEL_ADVANCED?"高级":currentLevel==LEVEL_INTERMEDIATE?"中级":"初级";}
    private String levelKey(){return currentLevel==LEVEL_ADVANCED?"advanced":currentLevel==LEVEL_INTERMEDIATE?"intermediate":"beginner";}

    private void restartScenario(){
        stopRecognition();
        turnIndex=0;phase=PHASE_LISTEN;sessionFinished=false;sessionChoiceFirstTry=0;sessionSpeechTurns=0;sessionSpeechPasses=0;sessionScoreTotal=0;weakWordIds.clear();
        showTurn();refreshStats();
    }

    private void showTurn(){
        DialogueTurn t=currentTurn();if(t==null)return;
        stopRecognition();phase=PHASE_LISTEN;choiceMistakeThisTurn=false;speechAttemptedThisTurn=false;speechPassedThisTurn=false;sessionFinished=false;turnFinalized=false;turnSpeechBestScore=0;turnHintLevel=0;speechAttemptsThisTurn=0;lastRecognized="";
        turnProgress.setText(current.emoji+" "+current.title+" · "+levelLabel()+" · 第 "+(turnIndex+1)+" / "+current.turns.size()+" 轮");
        if(currentLevel==LEVEL_BEGINNER){
            path.setText("初级 · ①先听 → ②看懂 → ③三选一 → ④开口");
            reveal.setText("看懂这句 →");
            feedback.setText("先听一遍。听不清可以用慢速，再看字幕。");
        }else if(currentLevel==LEVEL_INTERMEDIATE){
            path.setText("中级 · ①先听 → ②看懂 → ③只看中文 → ④直接开口");
            reveal.setText("看懂这句 →");
            feedback.setText("这一档没有选项。听懂以后，只看中文自己组织意大利语。");
        }else{
            path.setText("高级 · ①只听 → ②自由回答 → ③继续对话");
            reveal.setText("听懂了 · 直接回答 →");
            feedback.setText("不显示字幕、不显示中文，也没有选项。听完后直接用自己的意大利语回答。");
        }
        stage.setText("① 先听对方");
        npc.setText("🔊 先听，不看字幕");npcZh.setText("");npcZh.setVisibility(View.GONE);
        feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));
        reply.setVisibility(View.GONE);choiceContainer.setVisibility(View.GONE);speakContainer.setVisibility(View.GONE);
        reveal.setVisibility(View.VISIBLE);startAnswer.setVisibility(View.GONE);next.setVisibility(View.GONE);replay.setVisibility(View.VISIBLE);slow.setVisibility(View.VISIBLE);restart.setVisibility(View.VISIBLE);
        resetChoiceStyles();
        npc.postDelayed(()->{if(isAdded()&&phase==PHASE_LISTEN)playNpc(false);},300);
    }

    private void playNpc(boolean slowMode){DialogueTurn t=currentTurn();if(t!=null&&audio!=null)audio.speak(t.npc,slowMode?0.70f:1.0f);}

    private void revealMeaningOrAnswer(){
        DialogueTurn t=currentTurn();if(t==null||phase!=PHASE_LISTEN)return;
        if(currentLevel==LEVEL_ADVANCED){
            reveal.setVisibility(View.GONE);
            showSpeakPhase();
            return;
        }
        phase=PHASE_UNDERSTAND;stage.setText("② 看懂这句");npc.setText(t.npc);npcZh.setText(t.npcZh);npcZh.setVisibility(View.VISIBLE);reveal.setVisibility(View.GONE);startAnswer.setVisibility(View.VISIBLE);
        startAnswer.setText(currentLevel==LEVEL_INTERMEDIATE?"只看中文 · 自己回答 →":"开始选择回答 →");
        feedback.setText(currentLevel==LEVEL_INTERMEDIATE?"确认意思后，不给选项，下一步只看中文回答。":"先确认自己听到的内容，再进入回答。");
        feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));
    }

    private void beginAnswer(){
        DialogueTurn t=currentTurn();if(t==null||phase!=PHASE_UNDERSTAND)return;
        startAnswer.setVisibility(View.GONE);
        if(currentLevel==LEVEL_INTERMEDIATE){showSpeakPhase();return;}
        phase=PHASE_CHOOSE;stage.setText("③ 选择自然回答");choiceContainer.setVisibility(View.VISIBLE);feedback.setText("选择这个场景里最自然的一句回答。");feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));startedAt=System.currentTimeMillis();
        for(int i=0;i<choices.size();i++){
            MaterialButton b=choices.get(i);b.setVisibility(i<t.choices.size()?View.VISIBLE:View.GONE);b.setEnabled(true);b.setText(i<t.choices.size()?t.choices.get(i):"");
        }
    }

    private void resetChoiceStyles(){
        for(MaterialButton b:choices){b.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.line)));b.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_primary));b.setEnabled(true);}
    }

    private void choose(int idx){
        if(phase!=PHASE_CHOOSE||currentLevel!=LEVEL_BEGINNER)return;DialogueTurn t=currentTurn();if(t==null||idx<0||idx>=t.choices.size())return;
        boolean ok=idx==t.correct;long ms=Math.max(1,System.currentTimeMillis()-startedAt);progress.recordAuxiliaryResult("dialogue",ok,ms);progress.recordAuxiliarySubskill("dialogue_beginner",ok,ms);
        MaterialButton picked=choices.get(idx);picked.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),ok?R.color.success:R.color.error)));picked.setTextColor(ContextCompat.getColor(requireContext(),ok?R.color.success:R.color.error));
        if(!ok){
            choiceMistakeThisTurn=true;recordTargetWords(ProgressStore.DIM_MEANING,false,ms);SentenceFsrsRepository.recordDimension(requireContext(),"真实场景 · "+current.title,correctAnswer(t),t.answerZh,SentenceFsrsRepository.DIM_MEANING,false,35,null);progress.recordErrorCause(ErrorCause.MEANING_CONFUSION,0,"dialogue",correctAnswer(t),t.choices.get(idx),current.title+" · 情景应答");feedback.setText("这句在当前场景不合适，再选一次。");feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.error));return;
        }
        if(!choiceMistakeThisTurn){sessionChoiceFirstTry++;recordTargetWords(ProgressStore.DIM_MEANING,true,ms);SentenceFsrsRepository.recordDimension(requireContext(),"真实场景 · "+current.title,correctAnswer(t),t.answerZh,SentenceFsrsRepository.DIM_MEANING,true,100,null);}
        for(MaterialButton b:choices)b.setEnabled(false);feedback.setText("✓ 回答合适。现在不看意大利语，自己说一遍。");feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.success));showSpeakPhase();
    }

    private void showSpeakPhase(){
        DialogueTurn t=currentTurn();if(t==null)return;phase=PHASE_SPEAK;stage.setText(currentLevel==LEVEL_ADVANCED?"② 自由回答":"④ 自己开口");choiceContainer.setVisibility(View.GONE);speakContainer.setVisibility(View.VISIBLE);
        if(currentLevel==LEVEL_ADVANCED){
            speakPrompt.setText("根据刚才听到的内容，用你自己的意大利语自然回答。");target.setText("高级模式不显示中文，也不要求逐字照搬标准答案。");hint.setText("需要提示");
        }else if(currentLevel==LEVEL_INTERMEDIATE){
            speakPrompt.setText(t.answerZh==null||t.answerZh.trim().isEmpty()?"把你的回答直接说出来":t.answerZh);target.setText("只看中文，自己组织意大利语。允许自然的同义表达。");hint.setText("看意大利语提示");
        }else{
            speakPrompt.setText(t.answerZh==null||t.answerZh.trim().isEmpty()?"把刚才的正确回答自己说出来":t.answerZh);target.setText("先自己想，不显示意大利语");hint.setText("看提示");
        }
        speak.setText("🎙 我来说");speak.setEnabled(true);hint.setEnabled(true);skipSpeak.setEnabled(true);
    }

    private void showTargetHint(){
        DialogueTurn t=currentTurn();if(t==null||phase!=PHASE_SPEAK)return;turnHintLevel++;
        String tip=t.tip==null||t.tip.trim().isEmpty()?"":("\n💡 "+t.tip);
        if(currentLevel==LEVEL_ADVANCED&&turnHintLevel==1){
            target.setText("提示1 · 对方的意思："+t.npcZh+"\n你可以表达："+t.answerZh+"\n再试着自己说，不显示意大利语。");
            hint.setText("再看参考表达");
        }else{
            target.setText("参考表达："+correctAnswer(t)+tip);
            hint.setText("已显示参考");
        }
    }

    private void requestOrStartSpeech(){
        if(phase!=PHASE_SPEAK)return;
        if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return;}
        startSpeechRecognition();
    }

    private void startSpeechRecognition(){
        DialogueTurn t=currentTurn();if(t==null)return;
        if(!SpeechRecognizer.isRecognitionAvailable(requireContext())){feedback.setText("这台设备没有可用的意大利语语音识别。可以点提示朗读，再选择“暂时跳过口语”。");feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));return;}
        stopRecognition();recognizer=SpeechRecognizer.createSpeechRecognizer(requireContext());
        recognizer.setRecognitionListener(new RecognitionListener(){
            public void onReadyForSpeech(Bundle b){speak.setText("正在听…");}
            public void onBeginningOfSpeech(){}
            public void onRmsChanged(float r){}
            public void onBufferReceived(byte[] b){}
            public void onEndOfSpeech(){speak.setText("正在识别…");}
            public void onError(int error){speak.setText("🎙 再说一次");feedback.setText("没有识别清楚。放慢一点，再说一次即可。");feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.error));}
            public void onResults(Bundle b){ArrayList<String> xs=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);scoreSpeech(xs);}
            public void onPartialResults(Bundle b){}
            public void onEvent(int eventType,Bundle params){}
        });
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"it-IT");i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"it-IT");i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Parla in italiano");startedAt=System.currentTimeMillis();recognizer.startListening(i);
    }

    private void scoreSpeech(List<String> hypotheses){
        if(phase!=PHASE_SPEAK)return;DialogueTurn t=currentTurn();if(t==null)return;
        String expected=correctAnswer(t),best="";int bestScore=0;boolean ok=false;ConversationResult bestEval=null;
        if(hypotheses!=null){
            if(currentLevel==LEVEL_BEGINNER){
                for(String h:hypotheses){int score=similarity(expected,h);if(score>bestScore){bestScore=score;best=h;}}
                ok=bestScore>=68;
            }else{
                FreeConversationEngine engine=new FreeConversationEngine();
                for(String h:hypotheses){ConversationResult r=engine.evaluate(t,h);if(bestEval==null||r.score>bestEval.score){bestEval=r;bestScore=r.score;best=h;}}
                ok=bestEval!=null&&bestEval.understood;
            }
        }
        long ms=Math.max(1,System.currentTimeMillis()-startedAt);speechAttemptsThisTurn++;lastRecognized=best;turnSpeechBestScore=Math.max(turnSpeechBestScore,bestScore);
        progress.recordAuxiliaryResult("dialogue_speaking",ok,ms);progress.recordAuxiliarySubskill("dialogue_"+levelKey()+"_speaking",ok,ms);SentenceFsrsRepository.recordDimension(requireContext(),"真实场景 · "+current.title,expected,t.answerZh,SentenceFsrsRepository.DIM_SPEAKING,ok,bestScore,null);recordTargetWords(ProgressStore.DIM_SPEAKING,ok,ms);
        if(!speechAttemptedThisTurn){speechAttemptedThisTurn=true;sessionSpeechTurns++;}
        if(ok&&!speechPassedThisTurn){speechPassedThisTurn=true;sessionSpeechPasses++;}
        if(!ok)progress.recordErrorCause(currentLevel==LEVEL_BEGINNER?ErrorCause.PRONUNCIATION:ErrorCause.RECALL,0,"dialogue_"+levelKey(),expected,best,current.title+" · 匹配/理解 "+bestScore+"%");
        speak.setText(ok?"✓ 已通过":"🎙 再说一次");
        if(ok){
            String grammar="";
            if(bestEval!=null&&!bestEval.grammarIssues.isEmpty()){GrammarIssue g=bestEval.grammarIssues.get(0);grammar="\n语法提醒："+g.title+" · "+g.suggestion;}
            target.setText("参考表达："+expected+(t.tip==null||t.tip.isEmpty()?"":"\n💡 "+t.tip));speak.setEnabled(false);feedback.setText("✓ 对方能理解 · "+(currentLevel==LEVEL_BEGINNER?"内容匹配 ":"场景理解 ")+bestScore+"%\n识别到："+best+grammar);feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.success));showReply(true);
        }else{
            String extra=currentLevel==LEVEL_BEGINNER?"\n标准句："+expected:"\n先换一种说法再试；需要时再点提示。";
            feedback.setText("再练一次 · "+(currentLevel==LEVEL_BEGINNER?"内容匹配 ":"场景理解 ")+bestScore+"%\n识别到："+(best.isEmpty()?"（没有完整识别）":best)+extra+"\n这句的薄弱词已经回流智能复习。");feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.error));
        }
    }

    private void finalizeTurnScore(){
        if(turnFinalized)return;turnFinalized=true;int points;
        if(currentLevel==LEVEL_BEGINNER){points=(choiceMistakeThisTurn?0:65)+(speechPassedThisTurn?35:0);}
        else if(currentLevel==LEVEL_INTERMEDIATE){points=speechAttemptedThisTurn?turnSpeechBestScore:0;if(speechPassedThisTurn)points=Math.max(points,70);points=Math.max(0,points-Math.min(15,turnHintLevel*8));}
        else{points=speechAttemptedThisTurn?turnSpeechBestScore:0;if(speechPassedThisTurn)points=Math.max(points,60);points=Math.max(0,points-Math.min(20,turnHintLevel*10));}
        sessionScoreTotal+=Math.max(0,Math.min(100,points));
    }

    private void showReply(boolean spoken){
        if(phase!=PHASE_SPEAK)return;DialogueTurn t=currentTurn();if(t==null)return;phase=PHASE_REPLY;stopRecognition();finalizeTurnScore();String expected=correctAnswer(t);
        speakContainer.setVisibility(View.GONE);npc.setText(t.npc);npcZh.setText(t.npcZh);npcZh.setVisibility(View.VISIBLE);
        String userLine=spoken&&!lastRecognized.trim().isEmpty()?lastRecognized:expected;
        String reference=(currentLevel==LEVEL_BEGINNER||userLine.equalsIgnoreCase(expected))?"":"\n参考表达："+expected;
        reply.setText("你："+userLine+reference+(t.answerZh==null||t.answerZh.isEmpty()?"":"\n"+t.answerZh)+"\n\n对方："+t.reply+"\n"+t.replyZh+(t.tip==null||t.tip.isEmpty()?"":"\n\n💡 "+t.tip));reply.setVisibility(View.VISIBLE);next.setVisibility(View.VISIBLE);next.setText(turnIndex+1>=current.turns.size()?"完成这个场景 ✓":"继续对话 →");stage.setText("✓ 完成这一轮");
        if(!spoken){feedback.setText("已显示参考回答。下一轮尽量先自己说出来。");feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));}
        audio.speak(t.reply,1.0f);
    }

    private void advance(){
        if(sessionFinished){if(!lifeTaskId.isEmpty()&&current!=null&&progress.dialogueScenarioBestScore(current.id,currentLevel)>=LifeTaskEngine.passLine(currentLevel)){((MainActivity)requireActivity()).openLifeTask(lifeTaskId);return;}restartScenario();return;}
        if(phase!=PHASE_REPLY||current==null)return;
        if(turnIndex+1>=current.turns.size()){finishScenario();return;}
        turnIndex++;showTurn();
    }

    private void finishScenario(){
        if(current==null)return;phase=PHASE_DONE;sessionFinished=true;int turns=Math.max(1,current.turns.size());int choicePct=(int)Math.round(sessionChoiceFirstTry*100.0/turns);int speechPct=sessionSpeechTurns==0?0:(int)Math.round(sessionSpeechPasses*100.0/sessionSpeechTurns);int score=(int)Math.round(sessionScoreTotal/(double)turns);progress.markDialogueScenarioCompletion(current.id,score,currentLevel);if(!lifeTaskId.isEmpty())progress.recordAuxiliaryResult("life_task",score>=LifeTaskEngine.passLine(currentLevel),Math.max(1,System.currentTimeMillis()-startedAt));refreshStats();
        stage.setText("场景完成 ✅");turnProgress.setText(current.emoji+" "+current.title+" · "+levelLabel()+" · "+turns+"轮完成");npc.setText("你已经完成一个真实生活小对话");npcZh.setVisibility(View.VISIBLE);
        if(currentLevel==LEVEL_BEGINNER)npcZh.setText("选择首答正确率 "+choicePct+"% · 开口通过率 "+speechPct+"% · 本次综合 "+score+"%");
        else npcZh.setText((currentLevel==LEVEL_INTERMEDIATE?"直接开口":"自由回答")+"通过率 "+speechPct+"% · 本次综合 "+score+"%");
        choiceContainer.setVisibility(View.GONE);speakContainer.setVisibility(View.GONE);reveal.setVisibility(View.GONE);startAnswer.setVisibility(View.GONE);replay.setVisibility(View.GONE);slow.setVisibility(View.GONE);reply.setVisibility(View.VISIBLE);reply.setText(weakWordIds.isEmpty()?"这次没有新增明显薄弱词。":"有 "+weakWordIds.size()+" 个词在理解或口语阶段暴露出薄弱点，已经回流智能复习。");
        int nextLevel=progress.dialogueScenarioRecommendedLevel(current.id);String recommendation=nextLevel>currentLevel?" 已达到升级建议，可以尝试“"+(nextLevel==LEVEL_ADVANCED?"高级自由回答":"中级只给中文")+"”。":"";int line=lifeTaskId.isEmpty()?80:LifeTaskEngine.passLine(currentLevel);boolean stagePassed=score>=line;feedback.setText((stagePassed?"这一关已经达到通过线。":"建议再练一轮，重点做到听懂后直接开口。")+recommendation+(lifeTaskId.isEmpty()?"":("\n生活任务本关目标："+line+"%。")));feedback.setTextColor(ContextCompat.getColor(requireContext(),stagePassed?R.color.success:R.color.blue));next.setVisibility(View.VISIBLE);next.setText(lifeTaskId.isEmpty()?"再练一次这个难度":(stagePassed?"返回任务 · 解锁下一关":"再练一次这一关"));Toast.makeText(requireContext(),"完成"+levelLabel()+"真实场景会话 ✓",Toast.LENGTH_SHORT).show();
    }

    private void refreshStats(){
        if(current==null)return;int done=progress.dialogueScenarioCompleted(current.id,currentLevel),best=progress.dialogueScenarioBestScore(current.id,currentLevel);int b=progress.dialogueScenarioBestScore(current.id,LEVEL_BEGINNER),m=progress.dialogueScenarioBestScore(current.id,LEVEL_INTERMEDIATE),a=progress.dialogueScenarioBestScore(current.id,LEVEL_ADVANCED);int rec=progress.dialogueScenarioRecommendedLevel(current.id);String bestText=done==0?levelLabel()+"尚未完成":(levelLabel()+"已完成 "+done+" 次 · 最佳 "+best+"%");stats.setText(bestText+"\n初级 "+b+"% · 中级 "+m+"% · 高级 "+a+"% · 建议："+(rec==LEVEL_ADVANCED?"高级自由回答":rec==LEVEL_INTERMEDIATE?"中级只给中文":"初级有选项"));
    }

    private void recordTargetWords(int dim,boolean ok,long ms){
        DialogueTurn t=currentTurn();if(t==null)return;Set<Integer> seen=new LinkedHashSet<>();String normalized=correctAnswer(t).toLowerCase(Locale.ITALIAN).replace('’','\'').replaceAll("[^\\p{L}' ]"," ");for(String token:normalized.split("\\s+")){if(token.isEmpty())continue;Word w=words.byWord(token);if(w==null||w.id<=0||seen.contains(w.id))continue;seen.add(w.id);progress.recordEmbeddedDimensionResult(w.id,dim,ok,ms);if(!ok)weakWordIds.add(w.id);if(seen.size()>=4)break;}
    }

    private int similarity(String a,String b){String x=normalize(a),y=normalize(b);if(x.isEmpty()||y.isEmpty())return 0;int max=Math.max(x.length(),y.length()),d=editDistance(x,y);return (int)Math.round(Math.max(0,1-d/(double)max)*100);}
    private String normalize(String s){String n=Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ITALIAN).replace('’','\'');return n.replaceAll("[^a-z0-9' ]"," ").replaceAll("\\s+"," ").trim();}
    private int editDistance(String a,String b){int[] prev=new int[b.length()+1],cur=new int[b.length()+1];for(int j=0;j<=b.length();j++)prev[j]=j;for(int i=1;i<=a.length();i++){cur[0]=i;for(int j=1;j<=b.length();j++){int cost=a.charAt(i-1)==b.charAt(j-1)?0:1;cur[j]=Math.min(Math.min(cur[j-1]+1,prev[j]+1),prev[j-1]+cost);}int[] tmp=prev;prev=cur;cur=tmp;}return prev[b.length()];}

    private void stopRecognition(){if(recognizer!=null){try{recognizer.cancel();}catch(Exception ignored){}try{recognizer.destroy();}catch(Exception ignored){}recognizer=null;}if(speak!=null&&phase==PHASE_SPEAK)speak.setText("🎙 我来说");}

    @Override public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,@NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode!=REQ_AUDIO)return;if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)startSpeechRecognition();else{feedback.setText("没有麦克风权限，仍可完成听力和理解；口语阶段可以暂时跳过。");feedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));}
    }

    @Override public void onDestroyView(){stopRecognition();if(audio!=null)audio.release();super.onDestroyView();}
}
