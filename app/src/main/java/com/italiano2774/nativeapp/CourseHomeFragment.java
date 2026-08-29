package com.italiano2774.nativeapp;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Space;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

/** v5.0 primary screen: one obvious next action; advanced detail stays available on demand. */
public class CourseHomeFragment extends Fragment {
    private static final String ARG_UNIT="unit";
    private ProgressStore progress;private WordRepository words;private CourseCurriculumRepository curriculum;private CourseUnit unit;private MemoryArticleRepository memoryArticles;
    private LinearLayout path,dailyPlanContainer,homeDetails;private TextView stage,title,subtitle,daily,streak,xp,nextUnit,smartSummary,memoryArticleSummary,dailyPlanSummary,dailyPlanDetail,dailyNextTask,coachHint;private ProgressBar unitProgress,dailyPlanProgress;private MaterialButton continueButton,smartButton,memoryArticleButton,dailyPlanButton,detailsToggle;private MaterialButtonToggleGroup dailyPlanMinutes;private DailySmartPlan dailyPlan;private boolean detailsOpen;

    public static CourseHomeFragment newInstance(){return new CourseHomeFragment();}
    public static CourseHomeFragment newInstance(String unitId){CourseHomeFragment f=new CourseHomeFragment();Bundle b=new Bundle();b.putString(ARG_UNIT,unitId);f.setArguments(b);return f;}

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        View v=inflater.inflate(R.layout.fragment_course_home,container,false);progress=new ProgressStore(requireContext());words=WordRepository.get(requireContext());curriculum=CourseCurriculumRepository.get(requireContext());curriculum.migrateLegacyPositionIfNeeded(progress,words);
        stage=v.findViewById(R.id.text_course_stage);title=v.findViewById(R.id.text_course_unit_title);subtitle=v.findViewById(R.id.text_course_unit_subtitle);daily=v.findViewById(R.id.text_course_daily);streak=v.findViewById(R.id.text_course_streak);xp=v.findViewById(R.id.text_course_xp);nextUnit=v.findViewById(R.id.text_course_next_unit);unitProgress=v.findViewById(R.id.progress_course_unit);path=v.findViewById(R.id.container_course_path);continueButton=v.findViewById(R.id.button_course_continue);smartSummary=v.findViewById(R.id.text_smart_memory_summary);smartButton=v.findViewById(R.id.button_smart_memory);smartButton.setOnClickListener(x->((MainActivity)requireActivity()).openSmartMemory());memoryArticles=MemoryArticleRepository.get(requireContext());progress.migrateMemoryArticleExposureIfNeeded(memoryArticles);memoryArticleSummary=v.findViewById(R.id.text_memory_article_summary);memoryArticleButton=v.findViewById(R.id.button_memory_articles);memoryArticleButton.setOnClickListener(x->((MainActivity)requireActivity()).openMemoryArticles());
        dailyPlanSummary=v.findViewById(R.id.text_daily_plan_summary);dailyPlanDetail=v.findViewById(R.id.text_daily_plan_detail);dailyNextTask=v.findViewById(R.id.text_daily_next_task);coachHint=v.findViewById(R.id.text_daily_coach_hint);dailyPlanProgress=v.findViewById(R.id.progress_daily_plan);dailyPlanContainer=v.findViewById(R.id.container_daily_plan);dailyPlanButton=v.findViewById(R.id.button_daily_plan_start);dailyPlanMinutes=v.findViewById(R.id.group_daily_plan_minutes);homeDetails=v.findViewById(R.id.container_home_details);detailsToggle=v.findViewById(R.id.button_home_details_toggle);setDailyPlanTimeSelection();dailyPlanMinutes.addOnButtonCheckedListener((group,checkedId,isChecked)->{if(!isChecked)return;int minutes=checkedId==R.id.button_plan_10?10:(checkedId==R.id.button_plan_20?20:(checkedId==R.id.button_plan_30?30:(checkedId==R.id.button_plan_45?45:60)));if(progress.sessionMinutes()!=minutes){progress.setSessionMinutes(minutes);render();}});
        String requested=getArguments()==null?null:getArguments().getString(ARG_UNIT);detailsOpen=!progress.homeSimpleMode()||requested!=null;detailsToggle.setOnClickListener(x->{detailsOpen=!detailsOpen;applyDetailVisibility();});applyDetailVisibility();CourseUnit candidate=curriculum.byId(requested);unit=candidate!=null&&curriculum.isUnlocked(candidate,progress)?candidate:curriculum.current(progress);render();return v;
    }

    @Override public void onResume(){super.onResume();if(progress!=null&&unit!=null){CourseUnit current=curriculum.current(progress);String requested=getArguments()==null?null:getArguments().getString(ARG_UNIT);if(requested==null&&current!=null)unit=current;render();}}

    private void render(){if(unit==null)return;int done=curriculum.completedLessons(unit,progress);boolean complete=curriculum.isComplete(unit,progress);stage.setText(CourseCurriculumRepository.stageName(unit.stage)+" · 第"+unit.stageUnit+"单元");title.setText(unit.titleZh);CourseLessonEngine pathLabels=new CourseLessonEngine(words,progress);if(BeginnerGuideEngine.active(progress))subtitle.setText("第"+BeginnerGuideEngine.day(progress)+"个学习日 · 今天不用研究全部功能，只跟着“继续学习”往下走。");else subtitle.setText("当前单元 · "+pathLabels.pathSummary(unit)+" · 听、说、读、写会由系统自动穿插。");unitProgress.setProgress((int)Math.round(done*100.0/Math.max(1,unit.lessonCount)));streak.setText("🔥 "+progress.activityStreak()+"天");xp.setText("⭐ "+progress.courseXp());renderDailyPlan();long activeMin=Math.round(progress.dailyActiveSeconds(java.time.LocalDate.now())/60.0);int remain=dailyPlan==null?progress.sessionMinutes():dailyPlan.remainingMinutes();daily.setText("今天目标 "+progress.sessionMinutes()+" 分钟 · 已学习 "+activeMin+" 分钟 · 计划还剩约 "+remain+" 分钟");renderSmartMemory();renderMemoryArticles();renderPath(done,complete);
        CourseUnit next=curriculum.byIndex(unit.index+1);nextUnit.setText(next==null?"你已经走到完整课程的最后一站":"下一单元："+next.titleZh);
        int lesson=curriculum.firstIncompleteLesson(unit,progress);if(complete){CourseUnit c=curriculum.current(progress);boolean hasNext=c!=null&&c.index!=unit.index;continueButton.setText(hasNext?"继续下一单元":"复习这个单元");continueButton.setOnClickListener(x->{if(hasNext)((MainActivity)requireActivity()).openCourseUnit(c.id);else ((MainActivity)requireActivity()).openCourseLesson(unit.id,Math.max(0,unit.lessonCount-1));});}
        else{continueButton.setText(done>0?"继续学习 · 第"+(lesson+1)+"关":"开始第1关");continueButton.setOnClickListener(x->((MainActivity)requireActivity()).openCourseLesson(unit.id,lesson));}
    }


    private void applyDetailVisibility(){if(homeDetails==null||detailsToggle==null)return;homeDetails.setVisibility(detailsOpen?View.VISIBLE:View.GONE);detailsToggle.setText(detailsOpen?"收起今日详情 ▴":"查看今日详情 ▾");}

    private void setDailyPlanTimeSelection(){if(dailyPlanMinutes==null)return;int m=progress.sessionMinutes();int id=m<=10?R.id.button_plan_10:(m<=20?R.id.button_plan_20:(m<=30?R.id.button_plan_30:(m<=45?R.id.button_plan_45:R.id.button_plan_60)));dailyPlanMinutes.check(id);}

    private void renderDailyPlan(){
        setDailyPlanTimeSelection();dailyPlan=new DailySmartPlanEngine(requireContext(),words,progress).build();dailyPlanContainer.removeAllViews();DailySmartTask next=dailyPlan.next();int done=dailyPlan.completed(),total=dailyPlan.tasks.size(),remaining=dailyPlan.remainingMinutes();
        dailyPlanProgress.setProgress(dailyPlan.progressPercent());dailyPlanSummary.setText(done+" / "+total+"项完成 · 还剩约"+remaining+"分钟 · 今日目标"+dailyPlan.targetMinutes+"分钟");
        dailyNextTask.setText(next==null?"🎉 今天的主任务完成了":("下一步："+next.emoji+"  "+next.title+" · 约"+next.minutes+"分钟"));coachHint.setText(BeginnerGuideEngine.coachLine(progress,dailyPlan,next));
        dailyPlanDetail.setText(dailyPlan.focus+(dailyPlan.recoveryMode?"\n今天已自动优先处理反复出错和容易遗忘的内容。":"")+(dailyPlan.breakthroughSummary==null||dailyPlan.breakthroughSummary.isEmpty()?"":"\n连续补弱："+dailyPlan.breakthroughSummary));
        for(int i=0;i<dailyPlan.tasks.size();i++){
            DailySmartTask t=dailyPlan.tasks.get(i);LinearLayout row=new LinearLayout(requireContext());row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(2),dp(7),dp(2),dp(7));
            TextView icon=new TextView(requireContext());icon.setText(t.done?"✓":t.emoji);icon.setTextSize(18);icon.setGravity(Gravity.CENTER);row.addView(icon,new LinearLayout.LayoutParams(dp(34),dp(34)));
            LinearLayout copy=new LinearLayout(requireContext());copy.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);cp.setMargins(dp(7),0,dp(6),0);row.addView(copy,cp);
            TextView taskTitle=new TextView(requireContext());taskTitle.setText((i+1)+". "+t.title+(t==next?" · 下一项":""));taskTitle.setTextSize(13);taskTitle.setTextColor(ContextCompat.getColor(requireContext(),t==next?R.color.blue:R.color.text_primary));taskTitle.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);copy.addView(taskTitle);
            TextView taskSub=new TextView(requireContext());taskSub.setText(t.subtitle);taskSub.setTextSize(10);taskSub.setTextColor(ContextCompat.getColor(requireContext(),R.color.text_secondary));taskSub.setMaxLines(2);copy.addView(taskSub);
            TextView state=new TextView(requireContext());state.setText(t.done?"完成":"约"+t.minutes+"分");state.setTextSize(10);state.setTextColor(ContextCompat.getColor(requireContext(),t.done?R.color.success:R.color.text_secondary));row.addView(state);
            if(!t.done)row.setOnClickListener(x->openDailyTask(t));dailyPlanContainer.addView(row,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        if(next==null){dailyPlanButton.setText("查看今日总结 · 明日复习预告");dailyPlanButton.setEnabled(true);dailyPlanButton.setOnClickListener(x->((MainActivity)requireActivity()).openDailySummary());}else{dailyPlanButton.setEnabled(true);dailyPlanButton.setText("开始下一项 · "+next.title);dailyPlanButton.setOnClickListener(x->openDailyTask(next));}
    }

    private void openDailyTask(DailySmartTask task){if(task==null)return;MainActivity a=(MainActivity)requireActivity();switch(task.action){
        case "course_lesson":String[] c=task.payload.split("\\|",-1);if(c.length==2)try{a.openCourseLesson(c[0],Integer.parseInt(c[1]));}catch(Exception e){a.openCourseUnit(unit.id);}else a.openCourseUnit(unit.id);break;
        case "smart_memory":a.openSmartMemory();break;
        case "listen_speak":a.openListeningSpeaking();break;
        case "daily_speaking":a.openDailySpeakingChallenge();break;
        case "grammar":a.openMicroGrammarLesson(task.payload);break;
        case "dialogue":a.openDialogueTraining(task.payload);break;
        case "life_task":if(task.payload==null||task.payload.isEmpty())a.openLifeTaskMap();else a.openLifeTask(task.payload);break;
        case "memory_article_sentence":String[] m=task.payload.split("\\|",-1);if(m.length==2)try{a.openMemoryArticleSentenceStudy(m[0],Integer.parseInt(m[1]));}catch(Exception e){a.openMemoryArticles();}else a.openMemoryArticles();break;
        case "active_recall":int recall=300;try{recall=Integer.parseInt(task.payload);}catch(Exception ignored){}a.openActiveRecall(recall);break;
        case "sentence_dictation":a.openSentenceDictation();break;
        case "intensive_listening":a.openIntensiveListening();break;
        case "smart_cloze":a.openSmartCloze();break;
        case "free_conversation":a.openFreeConversation(task.payload);break;
        case "writing":a.openWriting();break;
        case "shadow":a.openShadowing();break;
        case "weak_word_story":a.openWeakWordStory();break;
        case "error_repair":a.openWrongWordRepair();break;
        case "error_evidence_repair":a.openErrorEvidenceRepair();break;
        case "weekly_exam":a.openWeeklyExam();break;
        default:a.openPractice();
    }}


    private void renderSmartMemory(){int due=0,unknown=0,mastered=0,route=Math.min(progress.vocabularyRouteLimit(),words.size());java.time.LocalDate today=java.time.LocalDate.now();java.util.List<Word> all=words.all();for(int i=0;i<all.size();i++){Word w=all.get(i);int m=progress.mastery(w.id);if(m>0&&progress.dueForReview(w.id,today))due++;if(progress.smartMastered(w.id))mastered++;if(i<route&&m==0)unknown++;}int fresh=Math.min(progress.perDay(),unknown);smartSummary.setText("今日复习 "+due+" · 新词 "+fresh+" · 四维掌握 "+mastered+"\n自动轮换：意→中 · 中→意 · 听音识词 · 句子填空；哪项弱就多练哪项。\n名词连冠词记 · 动词连原形/变位记 · 🇬🇧 英语近似词自动助记 · 词块继续保留");smartButton.setEnabled(due+fresh>0);smartButton.setText(due+fresh>0?"开始四模式智能背词":"今日已完成");}


    private void renderMemoryArticles(){java.util.List<Integer> ids=new java.util.ArrayList<>();for(int id=1;id<=Math.min(2000,words.size());id++)ids.add(id);int done=progress.memoryArticleCompletedTotal(memoryArticles),total=memoryArticles.totalSections(),encountered=progress.memoryArticleEncounteredCount(ids),recognized=progress.memoryArticleRecognizedCount(ids),mastered=progress.memoryArticleMasteredCount(ids);memoryArticleSummary.setText("已完成 "+done+" / "+total+" 小节 · 已遇到 "+encountered+" · 已认识 "+recognized+" · 真正掌握 "+mastered+"\n螺旋复现：每节40个新目标词，并主动带回12个旧词；新增逐句4轮背诵与正常/慢速朗读，最后继续四模式智能复习。");memoryArticleButton.setText(done>=total&&total>0?"十篇已完成 · 进入复习":"进入十篇通关");}

    private void renderPath(int done,boolean complete){path.removeAllViews();CourseLessonEngine labels=new CourseLessonEngine(words,progress);int firstIncomplete=Math.min(done,unit.lessonCount-1);for(int i=0;i<unit.lessonCount;i++){boolean migrated=unit.index<progress.courseUnlockedUnitIndex();boolean finished=migrated||progress.courseLessonDone(unit.id,i);boolean unlocked=finished||complete||i<=firstIncomplete;LinearLayout row=new LinearLayout(requireContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);rp.setMargins(dp(10),dp(5),dp(10),dp(5));path.addView(row,rp);
            Space left=new Space(requireContext()),right=new Space(requireContext());int offset=i%3==0?2:(i%3==1?3:1);row.addView(left,new LinearLayout.LayoutParams(0,dp(1),offset));LinearLayout nodeBox=new LinearLayout(requireContext());nodeBox.setOrientation(LinearLayout.VERTICAL);nodeBox.setGravity(Gravity.CENTER);row.addView(nodeBox,new LinearLayout.LayoutParams(dp(128),ViewGroup.LayoutParams.WRAP_CONTENT));row.addView(right,new LinearLayout.LayoutParams(0,dp(1),4-offset));
            MaterialButton b=new MaterialButton(requireContext());b.setAllCaps(false);b.setMinWidth(0);b.setCornerRadius(dp(34));b.setText(finished?"✓":(unlocked?labels.lessonEmoji(unit,i):"🔒"));b.setTextSize(20);b.setTextColor(ContextCompat.getColor(requireContext(),android.R.color.white));int bg=finished?R.color.green:(unlocked?R.color.blue:R.color.level0);b.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),bg)));b.setEnabled(unlocked);final int lesson=i;b.setOnClickListener(x->((MainActivity)requireActivity()).openCourseLesson(unit.id,lesson));nodeBox.addView(b,new LinearLayout.LayoutParams(dp(68),dp(68)));
            TextView label=new TextView(requireContext());label.setText(labels.lessonTitle(unit,i));label.setGravity(Gravity.CENTER);label.setTextSize(11);label.setTextColor(ContextCompat.getColor(requireContext(),unlocked?R.color.text_primary:R.color.text_secondary));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(128),ViewGroup.LayoutParams.WRAP_CONTENT);lp.topMargin=dp(4);nodeBox.addView(label,lp);
            if(i<unit.lessonCount-1){TextView line=new TextView(requireContext());line.setText("│");line.setGravity(Gravity.CENTER);line.setTextColor(ContextCompat.getColor(requireContext(),R.color.line));path.addView(line,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(18)));}
        }}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
