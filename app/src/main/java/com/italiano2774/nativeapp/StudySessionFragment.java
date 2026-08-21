package com.italiano2774.nativeapp;

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
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudySessionFragment extends Fragment {
    private WordRepository repo;private ProgressStore progress;private AudioPlayer audio;private LocalDate date;private boolean adaptive=false,rescue=false,reviewOnly=false,smartMemory=false;private int maxCards=0;
    private final List<Word> session=new ArrayList<>();private int index=0,again=0,hard=0,good=0,easy=0,softEndAt=Integer.MAX_VALUE,lastExposedIndex=-1;
    private final List<Integer> recentRatings=new ArrayList<>();private final List<Long> recentResponseMs=new ArrayList<>();private final Map<Integer,Integer> smartRequeues=new HashMap<>();private long cardShownAt=0L;private String sessionKey="";private boolean restored=false;
    private TextView progressText,topic,word,ipa,pron,chinese,english,lemma,grammar,example,exampleZh,ratingHint,summary,resumeHint,fatigueHint,memoryStatus;
    private ProgressBar progressBar;private LinearLayout answerPanel,examplePanel,fatiguePanel;private GridLayout ratings;private MaterialButton showAnswer,fatigueShort,ratingAgain,ratingHard,ratingGood,ratingEasy;

    public static StudySessionFragment newInstance(LocalDate d){StudySessionFragment f=new StudySessionFragment();Bundle b=new Bundle();b.putString("date",d.toString());b.putBoolean("adaptive",false);f.setArguments(b);return f;}
    public static StudySessionFragment newAdaptiveInstance(){return newAdaptiveInstance(0);}
    public static StudySessionFragment newAdaptiveInstance(int maxCards){StudySessionFragment f=new StudySessionFragment();Bundle b=new Bundle();b.putString("date",LocalDate.now().toString());b.putBoolean("adaptive",true);b.putInt("maxCards",maxCards);f.setArguments(b);return f;}
    public static StudySessionFragment newRescueInstance(){StudySessionFragment f=new StudySessionFragment();Bundle b=new Bundle();b.putString("date",LocalDate.now().toString());b.putBoolean("rescue",true);b.putInt("maxCards",12);f.setArguments(b);return f;}
    public static StudySessionFragment newReviewInstance(){StudySessionFragment f=new StudySessionFragment();Bundle b=new Bundle();b.putString("date",LocalDate.now().toString());b.putBoolean("reviewOnly",true);f.setArguments(b);return f;}
    public static StudySessionFragment newSmartMemoryInstance(){StudySessionFragment f=new StudySessionFragment();Bundle b=new Bundle();b.putString("date",LocalDate.now().toString());b.putBoolean("smartMemory",true);f.setArguments(b);return f;}

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_study_session,container,false);repo=WordRepository.get(requireContext());progress=new ProgressStore(requireContext());audio=new AudioPlayer(requireContext(),progress);
        date=getArguments()!=null?LocalDate.parse(getArguments().getString("date",LocalDate.now().toString())):LocalDate.now();adaptive=getArguments()!=null&&getArguments().getBoolean("adaptive",false);rescue=getArguments()!=null&&getArguments().getBoolean("rescue",false);reviewOnly=getArguments()!=null&&getArguments().getBoolean("reviewOnly",false);smartMemory=getArguments()!=null&&getArguments().getBoolean("smartMemory",false);maxCards=getArguments()!=null?getArguments().getInt("maxCards",0):0;
        progressText=v.findViewById(R.id.text_session_progress);progressBar=v.findViewById(R.id.progress_session);topic=v.findViewById(R.id.text_session_topic);word=v.findViewById(R.id.text_session_word);ipa=v.findViewById(R.id.text_session_ipa);
        pron=v.findViewById(R.id.text_session_pron);chinese=v.findViewById(R.id.text_session_chinese);english=v.findViewById(R.id.text_session_english);lemma=v.findViewById(R.id.text_session_lemma);grammar=v.findViewById(R.id.text_session_grammar);example=v.findViewById(R.id.text_session_example);exampleZh=v.findViewById(R.id.text_session_example_zh);
        answerPanel=v.findViewById(R.id.panel_answer);examplePanel=v.findViewById(R.id.panel_example);ratings=v.findViewById(R.id.panel_ratings);ratingHint=v.findViewById(R.id.text_rating_hint);summary=v.findViewById(R.id.text_session_summary);showAnswer=v.findViewById(R.id.button_show_answer);memoryStatus=v.findViewById(R.id.text_memory_status);resumeHint=v.findViewById(R.id.text_resume_hint);fatiguePanel=v.findViewById(R.id.panel_fatigue);fatigueHint=v.findViewById(R.id.text_fatigue_hint);fatigueShort=v.findViewById(R.id.button_fatigue_short);
        v.findViewById(R.id.button_back_today).setOnClickListener(x->((MainActivity)requireActivity()).openToday(LocalDate.now()));
        v.findViewById(R.id.button_session_audio).setOnClickListener(x->{Word w=current();if(w!=null)audio.play(w);});
        v.findViewById(R.id.button_example_audio).setOnClickListener(x->{Word w=current();if(w!=null)audio.speak(w.example);});
        showAnswer.setOnClickListener(x->reveal());
        ratingAgain=v.findViewById(R.id.button_rating_again);ratingHard=v.findViewById(R.id.button_rating_hard);ratingGood=v.findViewById(R.id.button_rating_good);ratingEasy=v.findViewById(R.id.button_rating_easy);ratingAgain.setOnClickListener(x->rate(0));ratingHard.setOnClickListener(x->rate(1));ratingGood.setOnClickListener(x->rate(2));ratingEasy.setOnClickListener(x->rate(3));if(smartMemory){ratings.setColumnCount(3);ratings.setRowCount(1);ratingAgain.setText("忘记了");ratingHard.setText("有点模糊");ratingGood.setText("记住了");ratingEasy.setVisibility(View.GONE);ratingHint.setText("不用猜分数，按你刚才真实的回忆情况选择");}
        fatigueShort.setOnClickListener(x->{softEndAt=Math.min(session.size(),index+SessionQualityEngine.suggestedFinishAfter());fatigueHint.setText("已切换为轻量收尾：再完成最多 "+Math.max(0,softEndAt-index)+" 个就结束。");fatigueShort.setEnabled(false);});
        sessionKey=buildSessionKey();if(!restoreSession())buildSession();if(!session.isEmpty()&&!restored)progress.saveStudySession(sessionKey,session,index,again,hard,good,easy);showCard();return v;
    }

    private String buildSessionKey(){if(smartMemory)return "smart_"+date+"_"+progress.perDay();if(rescue)return "rescue_"+date;if(reviewOnly)return "review_"+date;if(adaptive)return "adaptive_"+date+"_"+progress.sessionMinutes();return "fixed_"+date+"_"+progress.perDay();}
    private String modeName(){return smartMemory?"smart_memory":(rescue?"rescue":(reviewOnly?"review":(adaptive?"adaptive":"fixed")));}
    private boolean restoreSession(){
        ProgressStore.StudyResumeState s=progress.loadStudySession(sessionKey);if(s==null)return false;
        for(Integer id:s.wordIds){Word w=repo.byId(id==null?0:id);if(w!=null)session.add(w);}if(session.isEmpty()){progress.clearStudySession(sessionKey);return false;}
        index=Math.min(Math.max(0,s.index),session.size());again=s.again;hard=s.hard;good=s.good;easy=s.easy;restored=index>0&&index<session.size();if(restored){resumeHint.setVisibility(View.VISIBLE);resumeHint.setText("已恢复上次进度 · 从第 "+(index+1)+" / "+session.size()+" 个继续");}return true;
    }
    private void buildSession(){
        if(smartMemory){session.addAll(repo.smartMemoryPlan(progress).words);}
        else if(rescue){session.addAll(repo.rescuePlan(progress));}
        else if(reviewOnly){List<Word> due=repo.reviewDue(progress,LocalDate.now());int cap=Math.min(due.size(),Math.max(12,progress.protectedReviewCap(repo.all())));session.addAll(due.subList(0,cap));}
        else if(adaptive){List<Word> adaptiveWords=repo.adaptivePlan(progress).words;int n=maxCards>0?Math.min(maxCards,adaptiveWords.size()):adaptiveWords.size();session.addAll(adaptiveWords.subList(0,n));}
        else{List<Word> today=repo.forDate(progress.startDate(),date,progress.perDay());for(Word w:today)if(progress.mastery(w.id)<4)session.add(w);}
    }

    private Word current(){return index>=0&&index<session.size()?session.get(index):null;}
    private void showCard(){
        answerPanel.setVisibility(View.GONE);ratings.setVisibility(View.GONE);ratingHint.setVisibility(View.GONE);summary.setVisibility(View.GONE);showAnswer.setVisibility(View.VISIBLE);
        if(session.isEmpty()){progress.clearStudySession(sessionKey);progressText.setText(smartMemory?"今天的智能背词已完成":(rescue?"今天没有需要抢救的高风险词":(reviewOnly?"今天没有到期复习":(adaptive?"今天的智能任务已经完成":"今天的新词已经全部达到4级+"))));progressBar.setProgress(100);topic.setText(smartMemory?"智能记忆":"今日完成");word.setText("🎉");ipa.setText("没有待学内容");pron.setVisibility(View.GONE);showAnswer.setVisibility(View.GONE);summary.setVisibility(View.VISIBLE);summary.setText("可以去“练习”继续处理听力、拼写、口语和易混词弱项。");return;}
        if(index>=session.size()||index>=softEndAt){
            boolean fatigueStop=index<session.size();progress.clearStudySession(sessionKey);int total=Math.min(index,session.size());progressText.setText(total+" / "+session.size()+(fatigueStop?" · 今日先到这里":" 完成"));progressBar.setProgress(fatigueStop?(int)Math.round(total*100.0/session.size()):100);int remainingReview=reviewOnly?progress.dueCount(repo.all(),LocalDate.now()):0;topic.setText(fatigueStop?"轻量收尾完成":(smartMemory?"智能背词完成":(rescue?"5分钟抢救完成":(reviewOnly?(remainingReview==0?"今日到期复习已清零":"本轮到期复习完成"):(adaptive?"智能任务完成":"学习完成")))));word.setText(fatigueStop?"今天先学到这里 ☕":"做得很好 🎉");ipa.setText(fatigueStop?"下次打开会重新安排未完成内容":"这一轮已经结束");pron.setVisibility(View.GONE);showAnswer.setVisibility(View.GONE);ratings.setVisibility(View.GONE);ratingHint.setVisibility(View.GONE);fatiguePanel.setVisibility(View.GONE);summary.setVisibility(View.VISIBLE);
            if(rescue&&!fatigueStop)progress.markAuxiliaryCompletion("rescue");summary.setText((smartMemory?("忘记 "+again+" · 模糊 "+hard+" · 记住 "+good):("不会 "+again+" · 模糊 "+hard+" · 会 "+good+" · 很熟 "+easy))+(reviewOnly?"\n当前仍有 "+remainingReview+" 个到期复习；复习压力高时系统会分批处理。":"")+(fatigueStop?"\n检测到学习疲劳后已提前收尾，不会把剩余内容判定为已完成。":"\n系统已经根据你的选择自动安排下次复习。"));return;
        }
        Word w=current();int pct=(int)Math.round(index*100.0/session.size());progressBar.setProgress(pct);progressText.setText((index+1)+" / "+session.size()+(smartMemory?" · 智能背词":(rescue?" · 5分钟抢救":(reviewOnly?" · 今日到期复习":(adaptive?" · 智能今日任务":" · 今天待学")))));topic.setText(smartMemory?("记忆度 "+progress.memoryRetrievability(w.id)+"% · #"+w.num):(w.level+" · #"+w.num+" · 弱项："+progress.weakestDimensionName(w.id)));word.setText(w.word);ipa.setText(w.ipa==null?"":w.ipa);
        pron.setText(w.zhPron==null?"":w.zhPron);pron.setVisibility(progress.shouldShowPronunciation(w.id,false)?View.VISIBLE:View.GONE);chinese.setText(safe(w.chinese,w.english));english.setText("英文参考："+w.english);if(smartMemory){java.time.LocalDate due=progress.nextDueDate(w.id);String dueText=due==null?"首次学习":(due.equals(java.time.LocalDate.now())?"今天":(due.equals(java.time.LocalDate.now().plusDays(1))?"明天":due.toString()));memoryStatus.setText("当前记忆度 "+progress.memoryRetrievability(w.id)+"% · 已学 "+progress.attempts(w.id)+"次 · 忘记 "+progress.wrongCount(w.id)+"次\n当前安排："+dueText+"复习，本次选择后会自动重新计算");memoryStatus.setVisibility(View.VISIBLE);}else memoryStatus.setVisibility(View.GONE);
        boolean hasLemma=w.lemma!=null&&!w.lemma.trim().isEmpty(),hasForm=w.formInfo!=null&&!w.formInfo.trim().isEmpty();if(hasLemma||hasForm){lemma.setVisibility(View.VISIBLE);String head=hasLemma?(w.lemma.equalsIgnoreCase(w.word)?("原形："+w.word):("词形："+w.word+" → "+w.lemma)):"词形提示";lemma.setText(head+(hasForm?"\n"+w.formInfo:""));}else lemma.setVisibility(View.GONE);
        String grammarText=ItalianGrammar.grammarPanel(w);grammar.setVisibility(grammarText.isEmpty()?View.GONE:View.VISIBLE);if(!grammarText.isEmpty())grammar.setText(grammarText);
        boolean hasExample=ExampleQuality.isUsable(w);examplePanel.setVisibility(hasExample?View.VISIBLE:View.GONE);if(hasExample){example.setText(w.example);exampleZh.setText(w.exampleZh==null?"":w.exampleZh);}
        if(lastExposedIndex!=index){progress.markWordExposure(w.id,modeName());lastExposedIndex=index;}cardShownAt=SystemClock.elapsedRealtime();if(index+1<session.size())audio.preload(session.get(index+1));
    }
    private String safe(String a,String b){return a==null||a.trim().isEmpty()?b:a;}
    private void reveal(){answerPanel.setVisibility(View.VISIBLE);ratings.setVisibility(View.VISIBLE);ratingHint.setVisibility(View.VISIBLE);showAnswer.setVisibility(View.GONE);Word w=current();if(w!=null&&progress.shouldShowPronunciation(w.id,true))pron.setVisibility(View.VISIBLE);}
    private void rate(int rating){
        Word w=current();if(w==null)return;long response=Math.max(0L,SystemClock.elapsedRealtime()-cardShownAt);if(smartMemory)progress.recordSmartWordRating(w.id,Math.min(2,rating),response);else progress.recordStudyRating(w.id,rating);if(rating==0)again++;else if(rating==1)hard++;else if(rating==2)good++;else easy++;recentRatings.add(rating);recentResponseMs.add(response);while(recentRatings.size()>8){recentRatings.remove(0);recentResponseMs.remove(0);}if(smartMemory&&rating==0&&smartRequeues.getOrDefault(w.id,0)<1){int insertAt=Math.min(session.size(),index+4);session.add(insertAt,w);smartRequeues.put(w.id,1);}index++;progress.saveStudySession(sessionKey,session,index,again,hard,good,easy);
        if(softEndAt==Integer.MAX_VALUE&&SessionQualityEngine.isFatigued(recentRatings,recentResponseMs)&&index<session.size()){fatiguePanel.setVisibility(View.VISIBLE);fatigueShort.setEnabled(true);fatigueHint.setText("最近几题错误变多或回答明显变慢。可以继续，也可以再做5个就结束。");Toast.makeText(requireContext(),"检测到学习疲劳，建议短一点收尾",Toast.LENGTH_SHORT).show();}
        showCard();
    }
    @Override public void onDestroyView(){if(audio!=null)audio.release();super.onDestroyView();}
}
