package com.italiano2774.nativeapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.time.LocalDate;
import java.util.List;

/** v5.0.0 daily close-loop summary with four-track forgetting status and adaptive scheduling. */
public class DailySummaryFragment extends Fragment {
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){
        View v=i.inflate(R.layout.fragment_daily_summary,c,false);WordRepository repo=WordRepository.get(requireContext());ProgressStore p=new ProgressStore(requireContext());LocalDate today=LocalDate.now();
        List<Word> all=repo.all();List<Word> todayWords=repo.forDate(p.startDate(),today,p.perDay());int touched=0,strong=0;StringBuilder list=new StringBuilder();for(Word w:todayWords){int m=p.mastery(w.id);if(m>0)touched++;if(m>=4)strong++;if(list.length()<2200)list.append(m>=4?"✓ ":(m>0?"• ":"○ ")).append(w.word).append("  ").append(w.chinese==null?"":w.chinese).append("  · ").append(m).append("级\n");}
        int attempts=p.dailyAttempts(today),correct=p.dailyCorrect(today),accuracy=attempts==0?0:(int)Math.round(correct*100.0/attempts),minutes=(int)Math.round(p.dailyActiveSeconds(today)/60.0);int due=p.dueCount(all,today);int dueByTomorrow=p.dueCount(all,today.plusDays(1));
        ProgressStore.DashboardStats ds=p.dashboardStats(all,today);DailySmartPlan smart=new DailySmartPlanEngine(requireContext(),repo,p).build();ReviewForecast forecast=new ReviewForecastEngine(all,p).build();BreakthroughPlanEngine.Plan prescription=BreakthroughPlanEngine.build(requireContext(),repo,p);
        String dims="识义 "+ds.learnedDimensionPct[ProgressStore.DIM_MEANING]+"% · 听力 "+ds.learnedDimensionPct[ProgressStore.DIM_LISTENING]+"% · 拼写 "+ds.learnedDimensionPct[ProgressStore.DIM_SPELLING]+"% · 口语 "+ds.learnedDimensionPct[ProgressStore.DIM_SPEAKING]+"%";
        int speakingToday=Math.min(DailySpeakingChallengeEngine.DAILY_TARGET,p.dailyAuxiliaryAttempts("daily_speaking",today));
        int weakStoryToday=p.dailyAuxiliaryAttempts("weak_story",today);
        String dimDue="识义 "+p.dimensionDueCount(all,ProgressStore.DIM_MEANING,today)+" · 听力 "+p.dimensionDueCount(all,ProgressStore.DIM_LISTENING,today)+" · 拼写 "+p.dimensionDueCount(all,ProgressStore.DIM_SPELLING,today)+" · 口语 "+p.dimensionDueCount(all,ProgressStore.DIM_SPEAKING,today);
        ((TextView)v.findViewById(R.id.text_daily_summary_stats)).setText("智能计划："+smart.completed()+" / "+smart.tasks.size()+" 项 · 今日学习 "+minutes+" 分钟\n今日学习卡："+p.dailyCards(today)+" 张 · 练习 "+attempts+" 次 · 今日正确率 "+accuracy+"%\n每日5句开口："+speakingToday+" / 5 · 连续开口 "+p.dailySpeakingStreak()+" 天 · 弱词微短文 "+(weakStoryToday>0?"已完成":"未完成")+"\n真实生活任务："+LifeTaskEngine.completedStages(p)+" / 36关 · 已掌握 "+LifeTaskEngine.masteredTasks(p)+" / 12个场景\n当前到期复习："+due+" · 截至明天将到期："+dueByTomorrow+"\n四维今日到期："+dimDue+"\n明日按当前计划新增到期："+ds.tomorrowScheduled+" · 未来3天新增："+ds.nextThreeDaysScheduled+"\n错词："+ds.wrong+" · 顽固词："+ds.stubborn+" · 待修复错句："+p.pendingErrorRepairs()+" · 长期掌握："+ds.graduated);
        String weekly=p.weeklyExamCount()==0?("本轮周测进度 "+Math.min(7,p.weeklyExamActiveDaysSinceLast())+" / 7 个活跃学习日"):("上次周测 "+p.lastWeeklyExamScore()+"% · "+(p.weeklyAdjustmentActive()?p.weeklyAdjustmentSummary():"已开始累计下一轮活跃学习日"));
        ((TextView)v.findViewById(R.id.text_daily_summary_advice)).setText("四维掌握（已学词）\n"+dims+"\n\n每周实战诊断\n"+weekly+"\n\n三日突破处方\n"+prescription.headline+"\n"+prescription.threeDayText()+"\n\n未来7天\n"+forecast.advice+"\n\n明日建议\n"+tomorrowAdvice(p,ds,smart,dueByTomorrow));
        ((TextView)v.findViewById(R.id.text_daily_summary_words)).setText(list.length()==0?"今天没有课程顺序新词。":"今日课程词："+touched+" / "+todayWords.size()+" 已接触 · "+strong+" 个达到4级+\n\n"+list.toString().trim());
        v.findViewById(R.id.button_summary_words).setOnClickListener(x->((MainActivity)requireActivity()).openStudy(today));v.findViewById(R.id.button_summary_review).setOnClickListener(x->((MainActivity)requireActivity()).openReviewStudy());
        v.findViewById(R.id.button_summary_speaking).setOnClickListener(x->((MainActivity)requireActivity()).openDailySpeakingChallenge());
        v.findViewById(R.id.button_summary_weak_story).setOnClickListener(x->((MainActivity)requireActivity()).openWeakWordStory());
        v.findViewById(R.id.button_summary_life_tasks).setOnClickListener(x->((MainActivity)requireActivity()).openLifeTaskMap());
        com.google.android.material.button.MaterialButton breakthrough=v.findViewById(R.id.button_summary_breakthrough);breakthrough.setText("🎯 "+prescription.buttonLabel);breakthrough.setOnClickListener(x->openPrescription(prescription));breakthrough.setEnabled(!prescription.waitingForTomorrow&&!prescription.cycleComplete);
        View repair=v.findViewById(R.id.button_summary_repair);if(ds.wrong>0||ds.stubborn>0){repair.setVisibility(View.VISIBLE);repair.setOnClickListener(x->((MainActivity)requireActivity()).openWrongWordRepair());}else repair.setVisibility(View.GONE);
        com.google.android.material.button.MaterialButton evidence=v.findViewById(R.id.button_summary_error_evidence);evidence.setText("🧾 个人错句本 · 待修复 "+p.pendingErrorRepairs()+" 条");if(p.pendingErrorRepairs()>0){evidence.setVisibility(View.VISIBLE);evidence.setOnClickListener(x->((MainActivity)requireActivity()).openErrorEvidenceRepair());}else evidence.setVisibility(View.GONE);
        v.findViewById(R.id.button_summary_forecast).setOnClickListener(x->((MainActivity)requireActivity()).openReviewCalendar());
        com.google.android.material.button.MaterialButton weeklyExam=v.findViewById(R.id.button_summary_weekly_exam);weeklyExam.setText(p.weeklyExamDue()?"🧪 本周实战考试 · 已到期":"🧪 本周实战考试 · "+Math.min(7,p.weeklyExamActiveDaysSinceLast())+" / 7活跃日");weeklyExam.setOnClickListener(x->((MainActivity)requireActivity()).openWeeklyExam());
        v.findViewById(R.id.button_summary_passport).setOnClickListener(x->((MainActivity)requireActivity()).openMasteryPassport());
        v.findViewById(R.id.button_summary_back).setOnClickListener(x->((MainActivity)requireActivity()).openToday(today));return v;
    }

    private void openPrescription(BreakthroughPlanEngine.Plan plan){MainActivity a=(MainActivity)requireActivity();if("listen_speak".equals(plan.action))a.openListeningSpeaking();else if("intensive_listening".equals(plan.action))a.openIntensiveListening();else if("sentence_dictation".equals(plan.action))a.openSentenceDictation();else if("daily_speaking".equals(plan.action))a.openDailySpeakingChallenge();else if("grammar".equals(plan.action))a.openMicroGrammarLesson(plan.payload);else if("smart_cloze".equals(plan.action))a.openSmartCloze();else if("dialogue".equals(plan.action))a.openDialogueTraining(plan.payload);else if("life_task".equals(plan.action)){if(plan.payload==null||plan.payload.isEmpty())a.openLifeTaskMap();else a.openLifeTask(plan.payload);}else if("free_conversation".equals(plan.action))a.openFreeConversation(plan.payload);else if("writing".equals(plan.action))a.openWriting();else if("weak_word_story".equals(plan.action))a.openWeakWordStory();else if("active_recall".equals(plan.action)){int n=8;try{n=Integer.parseInt(plan.payload);}catch(Exception ignored){}a.openActiveRecall(n);}else a.openPractice();}

    private String tomorrowAdvice(ProgressStore p,ProgressStore.DashboardStats ds,DailySmartPlan smart,int dueByTomorrow){
        if(p.weeklyExamDue())return "本周实战考试已经到期。明天20分钟以上计划会优先安排周测；完成后系统会按六项结果自动调整下一周。";
        if(p.weeklyAdjustmentActive())return p.weeklyAdjustmentSummary()+" 明天按周测弱项和复习压力继续，不需要手工加量。";
        if(ds.reviewPressure>=2||ds.newQuota==0)return "先复习、再推进新内容。预计截至明天有 "+dueByTomorrow+" 个到期项；优先清理错题和最弱的"+dimName(ds.weakestDimension())+"。";
        if(ds.wrong>=6||ds.stubborn>=3)return "先做一轮错题回炉，再按智能计划继续。当前最弱项是"+dimName(ds.weakestDimension())+"，不要只重复认中文。";
        if(ds.weakestDimension()==ProgressStore.DIM_SPEAKING)return "明天优先完成每日5句开口，再做听说或情景会话。只看中文先说，减少“看得懂但说不出”的情况。";
        if(smart.completed()<smart.tasks.size())return "今天还有未完成任务，明天不需要补双倍；按新的智能计划继续即可，系统会自动顺延。";
        return "今天节奏正常。明天继续保持课程 + 间隔复习，并重点练"+dimName(ds.weakestDimension())+"。";
    }
    private String dimName(int d){return d==ProgressStore.DIM_LISTENING?"听力":(d==ProgressStore.DIM_SPELLING?"拼写":(d==ProgressStore.DIM_SPEAKING?"口语":"识义"));}
}
