package com.italiano2774.nativeapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Vocabulary study session. v3.3 upgrades smart-memory review from two directions
 * to four channels: recognition, active recall, listening and in-sentence usage.
 */
public class StudySessionFragment extends Fragment {
    private WordRepository repo;
    private EnglishBridgeRepository bridgeRepo;
    private MemoryAidRepository memoryAidRepo;
    private ProgressStore progress;
    private IssueReportStore issueReports;
    private AudioPlayer audio;
    private LocalDate date;
    private boolean adaptive=false,rescue=false,reviewOnly=false,smartMemory=false,articleReview=false;
    private int maxCards=0,currentMode=SmartReviewModeEngine.MODE_IT_ZH;
    private int[] articleWordIds=new int[0];
    private String articleReviewLabel="";

    private final List<Word> session=new ArrayList<>();
    private int index=0,again=0,hard=0,good=0,easy=0,softEndAt=Integer.MAX_VALUE,lastExposedIndex=-1;
    private final List<Integer> recentRatings=new ArrayList<>();
    private final List<Long> recentResponseMs=new ArrayList<>();
    private final Map<Integer,Integer> smartRequeues=new HashMap<>();
    private long cardShownAt=0L;
    private String sessionKey="";
    private boolean restored=false,choiceAnswered=false,choiceCorrect=false;
    private int forcedRatingMax=-1;
    private final String[] choiceValues=new String[4];

    private TextView progressText,topic,word,ipa,answerWord,pron,chinese,english,englishBridge,morphologyHint,chunkHint,lemma,grammar,example,exampleZh,ratingHint,summary,resumeHint,fatigueHint,memoryStatus,choiceFeedback;
    private ProgressBar progressBar;
    private LinearLayout answerPanel,examplePanel,fatiguePanel,choicePanel;
    private GridLayout ratings;
    private MaterialButton showAnswer,sessionAudio,fatigueShort,ratingAgain,ratingHard,ratingGood,ratingEasy,reportIssue;
    private final MaterialButton[] choiceButtons=new MaterialButton[4];

    public static StudySessionFragment newInstance(LocalDate d){StudySessionFragment f=new StudySessionFragment();Bundle b=new Bundle();b.putString("date",d.toString());b.putBoolean("adaptive",false);f.setArguments(b);return f;}
    public static StudySessionFragment newAdaptiveInstance(){return newAdaptiveInstance(0);}
    public static StudySessionFragment newAdaptiveInstance(int maxCards){StudySessionFragment f=new StudySessionFragment();Bundle b=new Bundle();b.putString("date",LocalDate.now().toString());b.putBoolean("adaptive",true);b.putInt("maxCards",maxCards);f.setArguments(b);return f;}
    public static StudySessionFragment newRescueInstance(){StudySessionFragment f=new StudySessionFragment();Bundle b=new Bundle();b.putString("date",LocalDate.now().toString());b.putBoolean("rescue",true);b.putInt("maxCards",12);f.setArguments(b);return f;}
    public static StudySessionFragment newReviewInstance(){StudySessionFragment f=new StudySessionFragment();Bundle b=new Bundle();b.putString("date",LocalDate.now().toString());b.putBoolean("reviewOnly",true);f.setArguments(b);return f;}
    public static StudySessionFragment newSmartMemoryInstance(){StudySessionFragment f=new StudySessionFragment();Bundle b=new Bundle();b.putString("date",LocalDate.now().toString());b.putBoolean("smartMemory",true);f.setArguments(b);return f;}
    public static StudySessionFragment newArticleReviewInstance(int[] wordIds,String label,int maxCards){StudySessionFragment f=new StudySessionFragment();Bundle b=new Bundle();b.putString("date",LocalDate.now().toString());b.putBoolean("smartMemory",true);b.putBoolean("articleReview",true);b.putIntArray("articleWordIds",wordIds==null?new int[0]:wordIds);b.putString("articleReviewLabel",label==null?"十篇通关":label);b.putInt("maxCards",maxCards);f.setArguments(b);return f;}

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_study_session,container,false);
        repo=WordRepository.get(requireContext());bridgeRepo=EnglishBridgeRepository.get(requireContext());memoryAidRepo=MemoryAidRepository.get(requireContext());progress=new ProgressStore(requireContext());issueReports=new IssueReportStore(requireContext());audio=new AudioPlayer(requireContext(),progress);
        date=getArguments()!=null?LocalDate.parse(getArguments().getString("date",LocalDate.now().toString())):LocalDate.now();adaptive=getArguments()!=null&&getArguments().getBoolean("adaptive",false);rescue=getArguments()!=null&&getArguments().getBoolean("rescue",false);reviewOnly=getArguments()!=null&&getArguments().getBoolean("reviewOnly",false);smartMemory=getArguments()!=null&&getArguments().getBoolean("smartMemory",false);articleReview=getArguments()!=null&&getArguments().getBoolean("articleReview",false);articleWordIds=getArguments()!=null?getArguments().getIntArray("articleWordIds"):null;if(articleWordIds==null)articleWordIds=new int[0];articleReviewLabel=getArguments()!=null?getArguments().getString("articleReviewLabel",""):"";maxCards=getArguments()!=null?getArguments().getInt("maxCards",0):0;

        progressText=v.findViewById(R.id.text_session_progress);progressBar=v.findViewById(R.id.progress_session);topic=v.findViewById(R.id.text_session_topic);word=v.findViewById(R.id.text_session_word);ipa=v.findViewById(R.id.text_session_ipa);
        answerWord=v.findViewById(R.id.text_session_answer_word);pron=v.findViewById(R.id.text_session_pron);chinese=v.findViewById(R.id.text_session_chinese);english=v.findViewById(R.id.text_session_english);englishBridge=v.findViewById(R.id.text_session_english_bridge);morphologyHint=v.findViewById(R.id.text_session_morphology);chunkHint=v.findViewById(R.id.text_session_chunk);lemma=v.findViewById(R.id.text_session_lemma);grammar=v.findViewById(R.id.text_session_grammar);example=v.findViewById(R.id.text_session_example);exampleZh=v.findViewById(R.id.text_session_example_zh);
        answerPanel=v.findViewById(R.id.panel_answer);examplePanel=v.findViewById(R.id.panel_example);ratings=v.findViewById(R.id.panel_ratings);ratingHint=v.findViewById(R.id.text_rating_hint);summary=v.findViewById(R.id.text_session_summary);showAnswer=v.findViewById(R.id.button_show_answer);memoryStatus=v.findViewById(R.id.text_memory_status);resumeHint=v.findViewById(R.id.text_resume_hint);fatiguePanel=v.findViewById(R.id.panel_fatigue);fatigueHint=v.findViewById(R.id.text_fatigue_hint);fatigueShort=v.findViewById(R.id.button_fatigue_short);
        choicePanel=v.findViewById(R.id.panel_smart_choices);choiceFeedback=v.findViewById(R.id.text_smart_choice_feedback);choiceButtons[0]=v.findViewById(R.id.button_smart_choice_1);choiceButtons[1]=v.findViewById(R.id.button_smart_choice_2);choiceButtons[2]=v.findViewById(R.id.button_smart_choice_3);choiceButtons[3]=v.findViewById(R.id.button_smart_choice_4);reportIssue=v.findViewById(R.id.button_report_word_issue);

        v.findViewById(R.id.button_back_today).setOnClickListener(x->((MainActivity)requireActivity()).openToday(LocalDate.now()));
        sessionAudio=v.findViewById(R.id.button_session_audio);sessionAudio.setOnClickListener(x->{Word w=current();if(w!=null)audio.play(w);});
        v.findViewById(R.id.button_example_audio).setOnClickListener(x->{Word w=current();if(w!=null&&w.example!=null&&!w.example.trim().isEmpty())audio.speak(w.example);});
        showAnswer.setOnClickListener(x->reveal());
        for(int i=0;i<choiceButtons.length;i++){final int ci=i;choiceButtons[i].setOnClickListener(x->answerChoice(ci));}
        reportIssue.setOnClickListener(x->showIssueDialog());

        ratingAgain=v.findViewById(R.id.button_rating_again);ratingHard=v.findViewById(R.id.button_rating_hard);ratingGood=v.findViewById(R.id.button_rating_good);ratingEasy=v.findViewById(R.id.button_rating_easy);
        ratingAgain.setOnClickListener(x->rate(0));ratingHard.setOnClickListener(x->rate(1));ratingGood.setOnClickListener(x->rate(2));ratingEasy.setOnClickListener(x->rate(3));
        if(channelReview()){ratings.setColumnCount(3);ratings.setRowCount(1);ratingAgain.setText("忘记了");ratingHard.setText("有点模糊");ratingGood.setText("记住了");ratingEasy.setVisibility(View.GONE);ratingHint.setText("按你刚才真实的回忆情况选择");}
        fatigueShort.setOnClickListener(x->{softEndAt=Math.min(session.size(),index+SessionQualityEngine.suggestedFinishAfter());fatigueHint.setText("已切换为轻量收尾：再完成最多 "+Math.max(0,softEndAt-index)+" 个就结束。");fatigueShort.setEnabled(false);});

        sessionKey=buildSessionKey();if(!restoreSession())buildSession();if(!session.isEmpty()&&!restored)progress.saveStudySession(sessionKey,session,index,again,hard,good,easy);showCard();return v;
    }

    private String buildSessionKey(){if(articleReview)return "memory_article_"+date+"_"+java.util.Arrays.hashCode(articleWordIds)+"_"+maxCards;if(smartMemory)return "smart_"+date+"_"+progress.perDay();if(rescue)return "rescue_"+date;if(reviewOnly)return "review_"+date;if(adaptive)return "adaptive_"+date+"_"+progress.sessionMinutes();return "fixed_"+date+"_"+progress.perDay();}
    private String modeName(){return articleReview?"memory_article":(smartMemory?"smart_memory":(rescue?"rescue":(reviewOnly?"review":(adaptive?"adaptive":"fixed"))));}
    /** v4.9 explicit due/rescue sessions also train the dimension that is actually due. */
    private boolean channelReview(){return smartMemory||reviewOnly||rescue;}

    private boolean restoreSession(){
        ProgressStore.StudyResumeState s=progress.loadStudySession(sessionKey);if(s==null)return false;
        for(Integer id:s.wordIds){Word w=repo.byId(id==null?0:id);if(w!=null)session.add(w);}if(session.isEmpty()){progress.clearStudySession(sessionKey);return false;}
        index=Math.min(Math.max(0,s.index),session.size());again=s.again;hard=s.hard;good=s.good;easy=s.easy;restored=index>0&&index<session.size();if(restored){resumeHint.setVisibility(View.VISIBLE);resumeHint.setText("已恢复上次进度 · 从第 "+(index+1)+" / "+session.size()+" 个继续");}return true;
    }

    private void buildSession(){
        if(articleReview){
            List<Word> targeted=new ArrayList<>();Set<Integer> seen=new HashSet<>();for(int id:articleWordIds){if(!seen.add(id))continue;Word w=repo.byId(id);if(w!=null)targeted.add(w);}
            Collections.sort(targeted,(a,b)->{int c=Integer.compare(progress.smartOverallPct(a.id),progress.smartOverallPct(b.id));if(c!=0)return c;c=Integer.compare(progress.wrongCount(b.id),progress.wrongCount(a.id));if(c!=0)return c;return Integer.compare(a.id,b.id);});
            int n=maxCards>0?Math.min(maxCards,targeted.size()):targeted.size();session.addAll(targeted.subList(0,n));
        }
        else if(smartMemory){session.addAll(repo.smartMemoryPlan(progress).words);}
        else if(rescue){session.addAll(repo.rescuePlan(progress));}
        else if(reviewOnly){List<Word> due=repo.reviewDue(progress,LocalDate.now());int cap=Math.min(due.size(),Math.max(12,progress.protectedReviewCap(repo.all())));session.addAll(due.subList(0,cap));}
        else if(adaptive){List<Word> adaptiveWords=repo.adaptivePlan(progress).words;int n=maxCards>0?Math.min(maxCards,adaptiveWords.size()):adaptiveWords.size();session.addAll(adaptiveWords.subList(0,n));}
        else{List<Word> today=repo.forDate(progress.startDate(),date,progress.perDay());for(Word w:today)if(progress.mastery(w.id)<4)session.add(w);}
    }

    private Word current(){return index>=0&&index<session.size()?session.get(index):null;}

    private void showCard(){
        resetCardUi();
        if(session.isEmpty()){finishEmpty();return;}
        if(index>=session.size()||index>=softEndAt){finishSession();return;}

        Word w=current();currentMode=channelReview()?SmartReviewModeEngine.choose(w,progress,repo):SmartReviewModeEngine.MODE_IT_ZH;
        int pct=(int)Math.round(index*100.0/session.size());progressBar.setProgress(pct);progressText.setText((index+1)+" / "+session.size()+(articleReview?" · 十篇通关定向复习":(smartMemory?" · 四模式智能背词":(rescue?" · 5分钟抢救":(reviewOnly?" · 今日到期复习":(adaptive?" · 智能今日任务":" · 今天待学"))))));
        configurePrompt(w);
        populateAnswerData(w);
        if(lastExposedIndex!=index){progress.markWordExposure(w.id,modeName());lastExposedIndex=index;}cardShownAt=SystemClock.elapsedRealtime();if(index+1<session.size())audio.preload(session.get(index+1));
    }

    private void resetCardUi(){
        answerPanel.setVisibility(View.GONE);ratings.setVisibility(View.GONE);ratingHint.setVisibility(View.GONE);summary.setVisibility(View.GONE);showAnswer.setVisibility(View.VISIBLE);answerWord.setVisibility(View.GONE);englishBridge.setVisibility(View.GONE);morphologyHint.setVisibility(View.GONE);chunkHint.setVisibility(View.GONE);choicePanel.setVisibility(View.GONE);choiceFeedback.setVisibility(View.GONE);reportIssue.setVisibility(View.VISIBLE);chinese.setVisibility(View.VISIBLE);memoryStatus.setVisibility(View.GONE);fatiguePanel.setVisibility(View.GONE);choiceAnswered=false;choiceCorrect=false;forcedRatingMax=-1;for(MaterialButton b:choiceButtons){b.setEnabled(true);b.setVisibility(View.VISIBLE);}
    }

    private void finishEmpty(){
        progress.clearStudySession(sessionKey);progressText.setText(articleReview?"本篇目标词已经没有可复习内容":(smartMemory?"今天的智能背词已完成":(rescue?"今天没有需要抢救的高风险词":(reviewOnly?"今天没有到期复习":(adaptive?"今天的智能任务已经完成":"今天的新词已经全部达到4级+")))));progressBar.setProgress(100);topic.setText(articleReview?(articleReviewLabel.isEmpty()?"十篇通关":articleReviewLabel):(smartMemory?"智能记忆":"今日完成"));word.setTextSize(36);word.setText("🎉");ipa.setText("没有待学内容");pron.setVisibility(View.GONE);sessionAudio.setVisibility(View.GONE);showAnswer.setVisibility(View.GONE);reportIssue.setVisibility(View.GONE);summary.setVisibility(View.VISIBLE);summary.setText("可以去“练习”继续处理听力、主动回忆、使用和易混词弱项。");
    }

    private void finishSession(){
        boolean fatigueStop=index<session.size();progress.clearStudySession(sessionKey);int total=Math.min(index,session.size());progressText.setText(total+" / "+session.size()+(fatigueStop?" · 今日先到这里":" 完成"));progressBar.setProgress(fatigueStop?(int)Math.round(total*100.0/session.size()):100);int remainingReview=reviewOnly?progress.dueCount(repo.all(),LocalDate.now()):0;topic.setText(fatigueStop?"轻量收尾完成":(articleReview?((articleReviewLabel.isEmpty()?"十篇通关":articleReviewLabel)+" · 定向复习完成"):(smartMemory?"四模式智能背词完成":(rescue?"5分钟抢救完成":(reviewOnly?(remainingReview==0?"今日到期复习已清零":"本轮到期复习完成"):(adaptive?"智能任务完成":"学习完成"))))));word.setTextSize(32);word.setText(fatigueStop?"今天先学到这里 ☕":"做得很好 🎉");ipa.setText(fatigueStop?"下次打开会重新安排未完成内容":"这一轮已经结束");pron.setVisibility(View.GONE);sessionAudio.setVisibility(View.GONE);showAnswer.setVisibility(View.GONE);reportIssue.setVisibility(View.GONE);ratings.setVisibility(View.GONE);ratingHint.setVisibility(View.GONE);choicePanel.setVisibility(View.GONE);fatiguePanel.setVisibility(View.GONE);summary.setVisibility(View.VISIBLE);
        if(rescue&&!fatigueStop)progress.markAuxiliaryCompletion("rescue");summary.setText((channelReview()?("忘记 "+again+" · 模糊 "+hard+" · 记住 "+good):("不会 "+again+" · 模糊 "+hard+" · 会 "+good+" · 很熟 "+easy))+(articleReview?"\n这些词仍按四维掌握度和遗忘间隔继续进入智能复习，不会因为读完文章就直接算掌握。":"")+(reviewOnly?"\n当前仍有 "+remainingReview+" 个到期复习；复习压力高时系统会分批处理。":"")+(fatigueStop?"\n检测到学习疲劳后已提前收尾，不会把剩余内容判定为已完成。":"\n系统已经根据四种能力表现安排下一次复习方式和时间。"));
    }

    private void configurePrompt(Word w){
        if(!channelReview()){topic.setText(w.level+" · #"+w.num+" · 弱项："+progress.weakestDimensionName(w.id));word.setTextSize(36);word.setText(w.word);ipa.setText(w.ipa==null?"":w.ipa);sessionAudio.setVisibility(View.VISIBLE);showAnswer.setText("显示答案");return;}
        String weakest=SmartReviewModeEngine.dimensionLabel(progress.weakestDimension(w.id));
        topic.setText(SmartReviewModeEngine.modeLabel(currentMode)+" · 当前弱项 "+weakest+" · #"+w.num);
        switch(currentMode){
            case SmartReviewModeEngine.MODE_ZH_IT:
                word.setTextSize(28);word.setText(safe(w.chinese,w.english));ipa.setText("先在脑中说出意大利语"+(isNoun(w)?"，名词请连冠词一起想":"")+"，再显示答案");sessionAudio.setVisibility(View.GONE);showAnswer.setText("显示意大利语答案");break;
            case SmartReviewModeEngine.MODE_LISTENING:
                word.setTextSize(30);word.setText("🔊 只听发音");ipa.setText("不要先看拼写，选择你听到的意大利语词");sessionAudio.setVisibility(View.VISIBLE);showAnswer.setText("想不起来 · 显示答案");setupChoices(w);word.postDelayed(()->{if(isAdded()&&current()==w)audio.play(w);},180);break;
            case SmartReviewModeEngine.MODE_CLOZE:
                word.setTextSize(25);word.setText(SmartReviewModeEngine.clozeSentence(w));ipa.setText((w.exampleZh==null||w.exampleZh.trim().isEmpty())?"选择最适合填入句子的词":w.exampleZh);sessionAudio.setVisibility(View.GONE);showAnswer.setText("想不起来 · 显示答案");setupChoices(w);break;
            default:
                word.setTextSize(36);word.setText(SmartReviewModeEngine.studyForm(w));ipa.setText(w.ipa==null?"":w.ipa);sessionAudio.setVisibility(View.VISIBLE);showAnswer.setText("显示答案");break;
        }
    }

    private void populateAnswerData(Word w){
        answerWord.setText(channelReview()?SmartReviewModeEngine.studyForm(w):w.word);pron.setText(w.zhPron==null?"":w.zhPron);pron.setVisibility(progress.shouldShowPronunciation(w.id,false)?View.VISIBLE:View.GONE);chinese.setText(safe(w.chinese,w.english));english.setText(w.english==null||w.english.trim().isEmpty()?"":"英文参考："+w.english);
        EnglishBridgeEntry bridge=bridgeRepo.forWord(w);if(bridge!=null){englishBridge.setText(bridge.displayText());englishBridge.setVisibility(View.VISIBLE);}else englishBridge.setVisibility(View.GONE);
        MemoryAidRepository.MorphologyHint mh=memoryAidRepo.morphologyFor(w);if(mh!=null){morphologyHint.setText(mh.displayText());morphologyHint.setVisibility(View.VISIBLE);}else morphologyHint.setVisibility(View.GONE);
        MemoryAidRepository.MemoryChunk mc=memoryAidRepo.chunkFor(w);if(mc!=null){chunkHint.setText(mc.displayText());chunkHint.setVisibility(View.VISIBLE);}else chunkHint.setVisibility(View.GONE);
        if(channelReview()){LocalDate due=progress.dimensionNextDueDate(w.id,SmartReviewModeEngine.dimensionForMode(currentMode));String dueText=due==null?"首次学习":(due.equals(LocalDate.now())?"今天":(due.equals(LocalDate.now().plusDays(1))?"明天":due.toString()));memoryStatus.setText("综合掌握 "+progress.smartOverallPct(w.id)+"%\n认词 "+progress.smartRecognitionPct(w.id)+"% · 主动回忆 "+progress.smartRecallPct(w.id)+"% · 听力 "+progress.smartListeningPct(w.id)+"% · 使用 "+progress.smartUsagePct(w.id)+"%\n本题轨道："+PersonalForgettingModel.dimensionLabel(SmartReviewModeEngine.dimensionForMode(currentMode))+" · 间隔 "+progress.dimensionIntervalDays(w.id,SmartReviewModeEngine.dimensionForMode(currentMode))+"天 · "+dueText+"复习");}
        boolean hasLemma=w.lemma!=null&&!w.lemma.trim().isEmpty(),hasForm=w.formInfo!=null&&!w.formInfo.trim().isEmpty();if(hasLemma||hasForm){lemma.setVisibility(View.VISIBLE);String head=hasLemma?(w.lemma.equalsIgnoreCase(w.word)?("原形："+w.word):("词形："+w.word+" → "+w.lemma)):"词形提示";String note=channelReview()?SmartReviewModeEngine.answerNote(w):"";lemma.setText(head+(hasForm?"\n"+w.formInfo:"")+(!note.isEmpty()?"\n"+note:""));}else if(channelReview()&&!SmartReviewModeEngine.answerNote(w).isEmpty()){lemma.setVisibility(View.VISIBLE);lemma.setText(SmartReviewModeEngine.answerNote(w));}else lemma.setVisibility(View.GONE);
        String grammarText=ItalianGrammar.grammarPanel(w);grammar.setVisibility(grammarText.isEmpty()?View.GONE:View.VISIBLE);if(!grammarText.isEmpty())grammar.setText(grammarText);
        boolean hasExample=ExampleQuality.isUsable(w);examplePanel.setVisibility(hasExample?View.VISIBLE:View.GONE);if(hasExample){example.setText(w.example);exampleZh.setText(w.exampleZh==null?"":w.exampleZh);}
    }

    private void setupChoices(Word target){
        choicePanel.setVisibility(View.VISIBLE);List<Word> pool=new ArrayList<>();String pos=target.partOfSpeech==null?"":target.partOfSpeech;for(Word x:repo.all()){if(x.id==target.id)continue;if(!pos.isEmpty()&&!pos.equalsIgnoreCase(x.partOfSpeech))continue;if(x.word==null||x.word.trim().isEmpty())continue;pool.add(x);}Collections.shuffle(pool,new Random(target.id*1009L+progress.attempts(target.id)*97L+currentMode));
        List<String> options=new ArrayList<>();options.add(target.word);Set<String> seen=new HashSet<>();seen.add(target.word.toLowerCase());for(Word x:pool){String value=x.word.trim();if(seen.add(value.toLowerCase()))options.add(value);if(options.size()==4)break;}if(options.size()<4){for(Word x:repo.all()){String value=x.word==null?"":x.word.trim();if(!value.isEmpty()&&seen.add(value.toLowerCase()))options.add(value);if(options.size()==4)break;}}
        Collections.shuffle(options,new Random(target.id*7919L+progress.attempts(target.id)*31L+currentMode));for(int i=0;i<4;i++){String value=i<options.size()?options.get(i):"—";choiceValues[i]=value;choiceButtons[i].setText(value);choiceButtons[i].setEnabled(!"—".equals(value));}
    }

    private void answerChoice(int choiceIndex){
        if(!channelReview()||choiceAnswered||choiceIndex<0||choiceIndex>=choiceValues.length)return;Word w=current();if(w==null)return;choiceAnswered=true;choiceCorrect=w.word.equalsIgnoreCase(choiceValues[choiceIndex]);forcedRatingMax=choiceCorrect?2:0;
        for(int i=0;i<choiceButtons.length;i++){choiceButtons[i].setEnabled(false);String raw=choiceValues[i];if(w.word.equalsIgnoreCase(raw))choiceButtons[i].setText("✓ "+raw);else if(i==choiceIndex&&!choiceCorrect)choiceButtons[i].setText("✗ "+raw);}
        choiceFeedback.setVisibility(View.VISIBLE);choiceFeedback.setText(choiceCorrect?"✓ 回答正确":"✗ 正确答案："+w.word);choiceFeedback.setTextColor(ContextCompat.getColor(requireContext(),choiceCorrect?R.color.success:R.color.error));revealAnswer(false);
    }

    private void reveal(){
        if(channelReview()&&(currentMode==SmartReviewModeEngine.MODE_LISTENING||currentMode==SmartReviewModeEngine.MODE_CLOZE)&&!choiceAnswered){choiceAnswered=true;choiceCorrect=false;forcedRatingMax=0;choiceFeedback.setVisibility(View.VISIBLE);choiceFeedback.setText("这次记为没有想起来 · 正确答案："+(current()==null?"":current().word));choiceFeedback.setTextColor(ContextCompat.getColor(requireContext(),R.color.error));for(MaterialButton b:choiceButtons)b.setEnabled(false);}
        revealAnswer(true);
    }

    private void revealAnswer(boolean manual){
        answerPanel.setVisibility(View.VISIBLE);ratings.setVisibility(View.VISIBLE);ratingHint.setVisibility(View.VISIBLE);showAnswer.setVisibility(View.GONE);Word w=current();if(w==null)return;answerWord.setVisibility(View.VISIBLE);chinese.setVisibility(View.VISIBLE);memoryStatus.setVisibility(channelReview()?View.VISIBLE:View.GONE);ipa.setText(w.ipa==null?"":w.ipa);sessionAudio.setVisibility(View.VISIBLE);if(progress.shouldShowPronunciation(w.id,true))pron.setVisibility(View.VISIBLE);
        if(channelReview()){String hint;switch(currentMode){case SmartReviewModeEngine.MODE_LISTENING:hint=choiceCorrect?"听音识词正确。再按真实熟悉程度决定复习间隔":"听音识词答错，本题会按“忘记了”记录并优先补听力";break;case SmartReviewModeEngine.MODE_CLOZE:hint=choiceCorrect?"句子使用正确。再按真实熟悉程度选择":"句子使用答错，本题会按“忘记了”记录并优先练使用";break;case SmartReviewModeEngine.MODE_ZH_IT:hint="刚才是中文 → 意大利语主动回忆，请按真实回忆程度选择";break;default:hint="刚才是意大利语 → 中文识义，请按真实回忆程度选择";}ratingHint.setText(hint);}
    }

    private void rate(int rating){
        Word w=current();if(w==null)return;long response=Math.max(0L,SystemClock.elapsedRealtime()-cardShownAt);int effective=rating;if(channelReview()){effective=Math.min(2,rating);if(forcedRatingMax>=0&&effective>forcedRatingMax){effective=forcedRatingMax;Toast.makeText(requireContext(),"本题实际答错，系统按“忘记了”记录",Toast.LENGTH_SHORT).show();}progress.recordSmartWordRating(w.id,effective,response,currentMode);}else progress.recordStudyRating(w.id,rating);
        int bucket=channelReview()?effective:rating;if(bucket==0)again++;else if(bucket==1)hard++;else if(bucket==2)good++;else easy++;recentRatings.add(bucket);recentResponseMs.add(response);while(recentRatings.size()>8){recentRatings.remove(0);recentResponseMs.remove(0);}if(channelReview()&&effective==0&&smartRequeues.getOrDefault(w.id,0)<1){int insertAt=Math.min(session.size(),index+4);session.add(insertAt,w);smartRequeues.put(w.id,1);}index++;progress.saveStudySession(sessionKey,session,index,again,hard,good,easy);
        if(softEndAt==Integer.MAX_VALUE&&SessionQualityEngine.isFatigued(recentRatings,recentResponseMs)&&index<session.size()){fatiguePanel.setVisibility(View.VISIBLE);fatigueShort.setEnabled(true);fatigueHint.setText("最近几题错误变多或回答明显变慢。可以继续，也可以再做5个就结束。");Toast.makeText(requireContext(),"检测到学习疲劳，建议短一点收尾",Toast.LENGTH_SHORT).show();}
        showCard();
    }

    private void showIssueDialog(){
        Word w=current();if(w==null)return;new AlertDialog.Builder(requireContext()).setTitle("这条哪里有问题？").setItems(IssueReportStore.CATEGORIES,(d,which)->{String category=IssueReportStore.CATEGORIES[Math.max(0,Math.min(which,IssueReportStore.CATEGORIES.length-1))];issueReports.add(w,category,channelReview()?SmartReviewModeEngine.modeLabel(currentMode):modeName());Toast.makeText(requireContext(),"已加入待检查清单："+category,Toast.LENGTH_SHORT).show();}).setNegativeButton("取消",null).show();
    }

    private boolean isNoun(Word w){return w!=null&&"noun".equalsIgnoreCase(w.partOfSpeech==null?"":w.partOfSpeech);}
    private String safe(String a,String b){return a==null||a.trim().isEmpty()?b:a;}

    @Override public void onDestroyView(){if(audio!=null)audio.release();super.onDestroyView();}
}
