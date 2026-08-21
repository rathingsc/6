package com.italiano2774.nativeapp;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * v2.7.8 performance: the expensive dashboard calculation is never executed on
 * the main thread. A small cached snapshot is painted immediately while a fresh
 * snapshot is calculated in the background.
 */
public class TodayFragment extends Fragment {
    private static final ExecutorService DASHBOARD_EXECUTOR=Executors.newSingleThreadExecutor();
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static volatile DashboardResult LAST_RESULT;
    private static volatile long LAST_RESULT_AT;
    private static final long CACHE_MS=15_000L;

    private WordRepository repo;private ProgressStore progress;private LearningPathEngine pathEngine;private List<LearningPathNode> nodes;
    private TextView date,streak,score,scoreStage,pathCount,weakFocus,nextTask,nextSubtitle,pathHint,weekGoal,banner,dailyWordsSummary,reviewSummary,resumeStatus;private ProgressBar pathProgress;private LinearLayout pathContainer;private MaterialButton continueButton;private MaterialButtonToggleGroup timeGroup;
    private Future<?> refreshTask;private int refreshGeneration=0;private boolean skipFirstResume=true;

    private static class DashboardResult {
        LocalDate today;LearningScoreEngine.Summary scoreSummary;List<LearningPathNode> nodes;LearningPathNode next;
        int done,streak,due;String weakFocus,pathHint,weekGoal;int sessionMinutes;
    }

    public static TodayFragment newInstance(LocalDate d){TodayFragment f=new TodayFragment();Bundle b=new Bundle();b.putString("date",d.toString());f.setArguments(b);return f;}

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_today,container,false);repo=WordRepository.get(requireContext());progress=new ProgressStore(requireContext());pathEngine=new LearningPathEngine(requireContext(),repo,progress);
        date=v.findViewById(R.id.text_date);streak=v.findViewById(R.id.text_streak);score=v.findViewById(R.id.text_score);scoreStage=v.findViewById(R.id.text_score_stage);pathCount=v.findViewById(R.id.text_path_count);weakFocus=v.findViewById(R.id.text_weak_focus);nextTask=v.findViewById(R.id.text_next_task);nextSubtitle=v.findViewById(R.id.text_next_subtitle);pathHint=v.findViewById(R.id.text_path_hint);weekGoal=v.findViewById(R.id.text_week_goal);banner=v.findViewById(R.id.text_completion_banner);dailyWordsSummary=v.findViewById(R.id.text_daily_words_summary);reviewSummary=v.findViewById(R.id.text_review_summary);resumeStatus=v.findViewById(R.id.text_resume_status);pathProgress=v.findViewById(R.id.progress_path);pathContainer=v.findViewById(R.id.path_container);continueButton=v.findViewById(R.id.button_continue_path);timeGroup=v.findViewById(R.id.group_time_budget);
        ((TextView)v.findViewById(R.id.text_greeting)).setText(greeting());v.findViewById(R.id.button_daily_words).setOnClickListener(x->((MainActivity)requireActivity()).openStudy(LocalDate.now()));v.findViewById(R.id.button_review_due).setOnClickListener(x->((MainActivity)requireActivity()).openReviewStudy());v.findViewById(R.id.button_daily_summary).setOnClickListener(x->((MainActivity)requireActivity()).openDailySummary());v.findViewById(R.id.button_practice_hub).setOnClickListener(x->((MainActivity)requireActivity()).openPractice());v.findViewById(R.id.button_emergency).setOnClickListener(x->((MainActivity)requireActivity()).openEmergency());v.findViewById(R.id.button_rescue).setOnClickListener(x->((MainActivity)requireActivity()).openRescueMode());v.findViewById(R.id.button_weekly_report).setOnClickListener(x->((MainActivity)requireActivity()).openWeeklyReport());
        timeGroup.addOnButtonCheckedListener((group,checked,isChecked)->{if(!isChecked)return;int m=checked==R.id.button_time_5?5:(checked==R.id.button_time_15?15:(checked==R.id.button_time_60?60:30));if(progress.sessionMinutes()!=m){progress.setSessionMinutes(m);refreshAsync(true);}});setTimeSelection();
        showImmediateHeader();refreshDailyWordsSummary();refreshResumeStatus();
        DashboardResult cached=LAST_RESULT;if(cached!=null&&System.currentTimeMillis()-LAST_RESULT_AT<CACHE_MS&&cached.sessionMinutes==progress.sessionMinutes())applyResult(cached,false);
        else showLoadingState();
        refreshAsync(false);return v;
    }

    @Override public void onResume(){super.onResume();if(progress!=null){refreshDailyWordsSummary();refreshResumeStatus();}if(skipFirstResume){skipFirstResume=false;return;}if(progress!=null)refreshAsync(false);}
    @Override public void onDestroyView(){refreshGeneration++;if(refreshTask!=null)refreshTask.cancel(true);super.onDestroyView();}

    private String greeting(){int h=LocalTime.now().getHour();return h<11?"Buongiorno 👋":(h<18?"Buon pomeriggio 👋":"Buonasera 👋");}
    private String dimName(int d){switch(d){case ProgressStore.DIM_LISTENING:return "听力";case ProgressStore.DIM_SPELLING:return "拼写";case ProgressStore.DIM_SPEAKING:return "口语";default:return "识义";}}
    private void setTimeSelection(){int id=progress.sessionMinutes()==5?R.id.button_time_5:(progress.sessionMinutes()==15?R.id.button_time_15:(progress.sessionMinutes()==60?R.id.button_time_60:R.id.button_time_30));timeGroup.check(id);}
    private void showImmediateHeader(){LocalDate today=LocalDate.now();date.setText(today.format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE",Locale.CHINA)));}
    private void refreshDailyWordsSummary(){
        if(dailyWordsSummary==null||repo==null||progress==null)return;
        List<Word> todayWords=repo.forDate(progress.startDate(),LocalDate.now(),progress.perDay());
        int pending=0;for(Word w:todayWords)if(progress.mastery(w.id)<4)pending++;
        if(todayWords.isEmpty())dailyWordsSummary.setText("今天没有新的课程顺序单词 · 每日设置 "+progress.perDay()+" 个");
        else dailyWordsSummary.setText("每天 "+progress.perDay()+" 个 · 今日 "+todayWords.size()+" 个 · 待学 "+pending+" 个");
    }
    private void refreshResumeStatus(){
        if(resumeStatus==null||progress==null)return;LocalDate d=LocalDate.now();String fixedKey="fixed_"+d+"_"+progress.perDay(),reviewKey="review_"+d;ProgressStore.StudyResumeState fixed=progress.loadStudySession(fixedKey),review=progress.loadStudySession(reviewKey);String latestKey=progress.latestStudySessionKey();ProgressStore.StudyResumeState latest=(latestKey==null||latestKey.isEmpty())?null:progress.loadStudySession(latestKey);ProgressStore.StudyResumeState r=fixed!=null?fixed:(review!=null?review:latest);String key=fixed!=null?fixedKey:(review!=null?reviewKey:latestKey);if(r==null||r.index<=0||r.index>=r.wordIds.size()){resumeStatus.setVisibility(View.GONE);return;}String type=key!=null&&key.startsWith("fixed_")?"今日单词":(key!=null&&key.startsWith("review_")?"到期复习":(key!=null&&key.startsWith("rescue_")?"5分钟抢救":"智能学习"));resumeStatus.setText("↩ "+type+"有未完成进度 · 已完成 "+r.index+" / "+r.wordIds.size()+" · 点击对应入口会自动继续");resumeStatus.setVisibility(View.VISIBLE);
    }

    private void showLoadingState(){nextTask.setText("正在整理今天的学习路径…");nextSubtitle.setText("页面已经打开，复习压力和弱项统计正在后台计算");continueButton.setEnabled(false);pathHint.setText(progress.sessionMinutes()+"分钟 · "+progress.vocabularyRouteLabel()+" · "+progress.learningGoalLabel());}

    private void refreshAsync(boolean showRefreshing){
        final int generation=++refreshGeneration;final LocalDate today=LocalDate.now();final Context app=requireContext().getApplicationContext();showImmediateHeader();
        if(showRefreshing&&LAST_RESULT==null)showLoadingState();
        if(refreshTask!=null)refreshTask.cancel(true);
        refreshTask=DASHBOARD_EXECUTOR.submit(()->{
            try{
                ProgressStore.DashboardStats stats=progress.dashboardStats(repo.all(),today);if(Thread.currentThread().isInterrupted())return;
                DashboardResult r=new DashboardResult();r.today=today;r.sessionMinutes=progress.sessionMinutes();r.scoreSummary=LearningScoreEngine.calculate(stats,progress);r.streak=progress.activityStreak();r.due=stats.due;
                LearningPathEngine engine=new LearningPathEngine(app,repo,progress);r.nodes=engine.build(stats);r.done=engine.completed(r.nodes);r.next=engine.next(r.nodes);r.weakFocus="今日弱项："+dimName(stats.weakestDimension());
                r.pathHint=r.sessionMinutes+"分钟 · "+progress.vocabularyRouteLabel()+" · "+progress.learningGoalLabel()+" · "+stats.paceAdvice;
                r.weekGoal="连续学习 "+r.streak+" 天 · 本周已学习 "+progress.weekActiveDays()+" / 7 天\n路线 "+progress.vocabularyRouteLabel()+"："+stats.routeIntroduced+" / "+progress.vocabularyRouteLimit()+" · "+stats.reviewPressureAdvice+"\n本周实际前台学习 "+progress.weekActualMinutes(today)+" 分钟 · 完成 "+progress.weekCards()+" 张学习卡、"+progress.weekAttempts()+" 次练习";
                if(Thread.currentThread().isInterrupted())return;MAIN.post(()->{if(!isAdded()||getView()==null||generation!=refreshGeneration)return;LAST_RESULT=r;LAST_RESULT_AT=System.currentTimeMillis();applyResult(r,true);});
            }catch(Exception ignored){MAIN.post(()->{if(isAdded()&&getView()!=null&&generation==refreshGeneration){nextTask.setText("今天的路径暂时无法刷新");nextSubtitle.setText("请稍后再试，其他学习功能仍可使用");continueButton.setEnabled(true);continueButton.setText("去练习");continueButton.setOnClickListener(x->((MainActivity)requireActivity()).openPractice());}});}
        });
    }

    private void applyResult(DashboardResult r,boolean celebrate){
        if(r==null)return;nodes=r.nodes;continueButton.setEnabled(true);if(reviewSummary!=null)reviewSummary.setText(r.due==0?"今天到期复习已清零":"到期剩余 "+r.due+" 个 · 建议先清理高风险项");date.setText(r.today.format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE",Locale.CHINA)));streak.setText("🔥 "+r.streak+"天");
        score.setText(String.valueOf(r.scoreSummary.score));scoreStage.setText(r.scoreSummary.stage+" · 覆盖 "+r.scoreSummary.coverage+"%");pathCount.setText(r.done+" / "+r.nodes.size());pathProgress.setProgress(r.nodes.isEmpty()?0:(int)Math.round(r.done*100.0/r.nodes.size()));weakFocus.setText(r.weakFocus);pathHint.setText(r.pathHint);weekGoal.setText(r.weekGoal);
        if(r.next==null){nextTask.setText("🎉 今天的路径完成了");nextSubtitle.setText("可以自由复习，或者休息一下让记忆巩固。");continueButton.setText("去弱项练习");continueButton.setOnClickListener(x->((MainActivity)requireActivity()).openPractice());}
        else{nextTask.setText(r.next.emoji+"  "+r.next.title+" · 约"+r.next.minutes+"分钟");nextSubtitle.setText(r.next.subtitle+(r.next.reason.isEmpty()?"":"\n为什么安排："+r.next.reason));LearningPathNode n=r.next;continueButton.setText("继续学习 →");continueButton.setOnClickListener(x->openNode(n));}
        renderNodes();if(celebrate)showCompletionIfNeeded(r.done,r.today);
    }

    private void renderNodes(){
        pathContainer.removeAllViews();if(nodes==null)return;LearningPathNode first=pathEngine.next(nodes);
        for(int i=0;i<nodes.size();i++){LearningPathNode node=nodes.get(i);MaterialCardView card=new MaterialCardView(requireContext());card.setRadius(dp(16));card.setCardElevation(0);card.setCardBackgroundColor(ContextCompat.getColor(requireContext(),node.done?R.color.surface_success:R.color.surface));card.setStrokeColor(ContextCompat.getColor(requireContext(),node.done?R.color.level4:(first==node?R.color.blue:R.color.line)));card.setStrokeWidth(dp(first==node&&!node.done?2:1));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);cp.setMargins(dp(14),dp(4),dp(14),dp(4));pathContainer.addView(card,cp);
            LinearLayout row=new LinearLayout(requireContext());row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(android.view.Gravity.CENTER_VERTICAL);row.setPadding(dp(13),dp(11),dp(10),dp(11));card.addView(row,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
            TextView icon=new TextView(requireContext());icon.setText(node.done?"✓":node.emoji);icon.setTextSize(20);icon.setGravity(android.view.Gravity.CENTER);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(38),dp(38));row.addView(icon,ip);
            LinearLayout copy=new LinearLayout(requireContext());copy.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams cop=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);cop.setMargins(dp(8),0,dp(8),0);row.addView(copy,cop);
            TextView title=new TextView(requireContext());title.setText((i+1)+". "+node.title);title.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_primary));title.setTextSize(14);title.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);copy.addView(title);
            TextView sub=new TextView(requireContext());sub.setText(node.subtitle+" · "+node.minutes+"分钟"+(node.reason.isEmpty()?"":"\n"+node.reason));sub.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));sub.setTextSize(10);sub.setMaxLines(2);copy.addView(sub);
            MaterialButton b=new MaterialButton(requireContext());b.setAllCaps(false);b.setMinWidth(0);b.setText(node.done?"已完成":"开始");b.setEnabled(!node.done);b.setTextSize(11);b.setOnClickListener(v->openNode(node));row.addView(b,new LinearLayout.LayoutParams(dp(72),dp(40)));
        }
    }

    private void openNode(LearningPathNode n){MainActivity a=(MainActivity)requireActivity();switch(n.action){
        case "vocab":a.openAdaptiveStudy(Integer.parseInt(n.payload));break;
        case "practice":int d=Integer.parseInt(n.payload);a.openPracticeMode(d==ProgressStore.DIM_LISTENING?"listen":(d==ProgressStore.DIM_SPELLING?"spell":(d==ProgressStore.DIM_SPEAKING?"speaking":"zh")));break;
        case "grammar":a.openSentencePatterns(n.payload);break;
        case "mission":a.openMission(n.payload);break;
        case "shadow":a.openShadowing();break;
        case "reading":if(n.payload==null||n.payload.isEmpty())a.openReadingList();else a.openReading(n.payload);break;
        case "pronunciation":a.openPronunciation();break;
        case "sentence_dictation":a.openSentenceDictation();break;
        case "cloze":a.openSmartCloze();break;
        case "verb_center":a.openVerbCenter();break;
        case "word_family":a.openWordFamilies();break;
        case "grammar_map":a.openGrammarMap();break;
        case "listening_course":a.openListeningCourse();break;
        case "active_recall":a.openActiveRecall(n.payload==null||n.payload.isEmpty()?300:Integer.parseInt(n.payload));break;
        case "focus_mode":a.openFocusMode();break;
        case "intensive_listening":a.openIntensiveListening();break;
        case "stubborn":a.openStubbornWords();break;
        case "phrases":a.openPhrases();break;
        case "preposition":a.openPrepositions();break;
        case "past_tense":a.openPastTense();break;
        case "pronouns":a.openPronouns();break;
        case "family_train":a.openFamilyTraining();break;
        case "writing":a.openWriting();break;
        case "sentence_review":a.openSentenceReview();break;
        case "rescue":a.openRescueMode();break;
        case "dictation":a.openPracticeMode("dictation");break;
        case "confusion":a.openPracticeMode("confusion");break;
        case "dialogue":a.openDialogueTraining(n.payload);break;
        case "freechat":a.openFreeConversation(n.payload);break;
        default:a.openPractice();
    }}

    private void showCompletionIfNeeded(int done,LocalDate today){int prev=progress.pathCelebratedCount(today);if(prev<0){progress.setPathCelebratedCount(today,done);return;}if(done<=prev)return;progress.setPathCelebratedCount(today,done);banner.setText(done>=nodes.size()?"🎉 今日学习路径完成！":"✓ 完成一个学习节点 · 继续保持");banner.setVisibility(View.VISIBLE);banner.setAlpha(0f);banner.setScaleX(.94f);banner.setScaleY(.94f);banner.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(260).withEndAction(()->banner.animate().alpha(0f).setStartDelay(1800).setDuration(500).withEndAction(()->banner.setVisibility(View.GONE)).start()).start();}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
