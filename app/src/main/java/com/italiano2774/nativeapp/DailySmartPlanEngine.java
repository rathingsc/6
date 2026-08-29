package com.italiano2774.nativeapp;

import android.content.Context;
import java.time.LocalDate;
import java.util.List;

/**
 * v4.8.0 beginner-first daily planner with recovery, weekly diagnosis and a real-life task map.
 *
 * The planner still composes the audited modules already in the app, but it now reacts to
 * accumulated wrong/stubborn words. When review pressure rises, error repair takes a real
 * slot in today's finite time budget instead of simply adding more and more study modules.
 */
public class DailySmartPlanEngine {
    private final Context context;
    private final WordRepository words;
    private final ProgressStore progress;
    private final CourseCurriculumRepository curriculum;
    private final MemoryArticleRepository memoryArticles;

    public DailySmartPlanEngine(Context context,WordRepository words,ProgressStore progress){
        this.context=context.getApplicationContext();this.words=words;this.progress=progress;
        this.curriculum=CourseCurriculumRepository.get(context);this.memoryArticles=MemoryArticleRepository.get(context);
    }

    public DailySmartPlan build(){
        LocalDate today=LocalDate.now();List<Word> all=words.all();ProgressStore.DashboardStats stats=progress.dashboardStats(all,today);
        PersonalizedCourse personal=new PersonalizedCourseEngine(context,words,progress).buildOverview(stats);
        BreakthroughPlanEngine.Plan prescription=BreakthroughPlanEngine.build(context,words,progress);
        DailySmartPlan plan=new DailySmartPlan();int budget=progress.sessionMinutes();plan.targetMinutes=budget;plan.dueWords=stats.due;plan.tomorrowScheduledWords=stats.tomorrowScheduled;plan.newWords=stats.newQuota;plan.wrongWords=stats.wrong;plan.stubbornWords=stats.stubborn;plan.breakthroughFocus=prescription.headline;plan.breakthroughSummary=prescription.summary;
        int pendingEvidence=progress.pendingErrorRepairs();
        boolean weeklyExamDue=progress.weeklyExamDue();DailySmartTask weeklyExam=task("weekly_exam","🧪","每周实战考试","18题 · 六项能力混合检查 · 完成后自动调整接下来7天",15,"weekly_exam","",progress.weeklyExamCompletedToday(),170);
        DailySmartTask weeklyFocus=weeklyFocusTask(today);
        boolean recoveryNeeded=stats.wrong>=6||stats.stubborn>=3||stats.reviewPressure>=1||pendingEvidence>=2;
        boolean pressureProtection=stats.reviewPressure>=2||stats.newQuota==0;
        plan.recoveryMode=recoveryNeeded;
        String mode=pressureProtection?"复习保护模式":(recoveryNeeded?"学习 + 补漏模式":"正常推进模式");
        plan.focus=(weeklyExamDue?"🧪 已累计7个活跃学习日 · 本周实战考试到期\n":"")+"今日重点："+dimName(stats.weakestDimension())+" · "+mode+"\n"+prescription.headline+" · 待修复错句"+pendingEvidence+"条 · "+stats.paceAdvice+" · 明日已排"+stats.tomorrowScheduled+"个复习"+(progress.weeklyAdjustmentActive()?"\n📌 "+progress.weeklyAdjustmentSummary():"");

        int vocabMinutes=budget<=10?4:(budget<=20?6:(budget<=30?7:8));int vocabTarget=budget<=10?5:(budget<=20?8:(budget<=30?12:18));
        boolean vocabDone=progress.dailyCards(today)>=vocabTarget||(stats.due==0&&stats.newQuota==0);
        DailySmartTask vocab=task("vocab","🧠","复习 + 新词",stats.due+"个到期 · 明日已排"+stats.tomorrowScheduled+"个 · "+stats.newQuota+"个建议新词 · 四模式轮换",vocabMinutes,"smart_memory","",vocabDone,145+Math.min(35,stats.due));

        CourseUnit current=curriculum.current(progress);DailySmartTask course=null;
        if(current!=null){int lesson=curriculum.firstIncompleteLesson(current,progress);String lessonTitle=new CourseLessonEngine(words,progress).lessonTitle(current,lesson);String courseHint=pressureProtection?"复习压力偏高 · 今天只推进这一小关":"保持 A0→B1 主线不断档";course=task("course","🧭","循序课程",current.titleZh+" · "+lessonTitle+" · "+courseHint,budget<=10?6:7,"course_lesson",current.id+"|"+lesson,progress.courseTodayXp()>0,132);}

        int listenTarget=budget<=20?4:6;boolean listenDone=progress.dailyAuxiliaryAttempts("listen_speak",today)>=listenTarget;
        DailySmartTask listen=task("listen","🎧🗣","听力 + 开口","听句、听词、跟读、看中文开口",7,"listen_speak","",listenDone,(stats.weakestDimension()==ProgressStore.DIM_LISTENING||stats.weakestDimension()==ProgressStore.DIM_SPEAKING)?128:98);

        int spokenToday=Math.min(DailySpeakingChallengeEngine.DAILY_TARGET,progress.dailyAuxiliaryAttempts("daily_speaking",today));boolean speak5Done=spokenToday>=DailySpeakingChallengeEngine.DAILY_TARGET;
        DailySmartTask speak5=task("speak5","🗣","每日5句开口",spokenToday+" / 5 · 只看中文先说 · 错句自动回流复习",5,"daily_speaking","",speak5Done,stats.weakestDimension()==ProgressStore.DIM_SPEAKING?138:118);

        GrammarPoint gp=GrammarDiagnostics.recommended(progress);String pattern=(gp==null||gp.practicePatternId==null)?"vorrei":gp.practicePatternId;String grammarName=gp==null?"高频生活句型":gp.title;
        boolean grammarDone=progress.dailyAuxiliaryAttempts("pattern",today)>=3;
        DailySmartTask grammar=task("grammar","🧩","微语法3题",grammarName+" · 一个规则马上用",4,"grammar",pattern,grammarDone,progress.dueGrammarCount()>0?116:94);

        LifeTask nextLife=LifeTaskEngine.nextTask(progress);int nextLifeLevel=nextLife==null?1:LifeTaskEngine.nextLevel(progress,nextLife);boolean lifeDone=LifeTaskEngine.completedStages(progress)>=36||progress.dailyAuxiliaryCorrect("life_task",today)>=1;
        DailySmartTask lifeTask=task("life_task","🗺","真实生活任务",nextLife==null?"12个生活场景 × 3级":(nextLife.emoji+" "+nextLife.title+" · "+LifeTaskEngine.levelTitle(nextLifeLevel)+" · "+LifeTaskEngine.completedStages(progress)+"/36关"),7,"life_task",nextLife==null?"":nextLife.id,lifeDone,(MasteryPassportEngine.ACTION_REAL_USE.equals(prescription.skillKey)?132:102)+((progress.learningGoal()==ProgressStore.GOAL_LIFE||progress.learningGoal()==ProgressStore.GOAL_WORK)?14:0));

        int repairTarget=budget<=20?2:3;boolean repairDone=(stats.wrong==0&&stats.stubborn==0)||progress.dailyAuxiliaryAttempts("error_repair",today)>=repairTarget;
        DailySmartTask repair=task("repair","🩹","错题回炉","错词"+stats.wrong+"个 · 顽固词"+stats.stubborn+"个 · 主动回忆 → 拼写 → 例句",5,"error_repair","",repairDone,stats.reviewPressure>=2?140:(stats.stubborn>=8?126:104));
        int evidenceTarget=budget<=20?1:2;boolean evidenceDone=pendingEvidence==0||progress.dailyAuxiliaryCorrect("error_evidence_repair",today)>=Math.min(evidenceTarget,pendingEvidence);
        DailySmartTask evidenceRepair=task("evidence_repair","🧾","个人错句本","待修复"+pendingEvidence+"条 · 必须重新写对才移出 · 正确后进入句子FSRS",5,"error_evidence_repair","",evidenceDone,pendingEvidence>=6?144:(pendingEvidence>=2?130:106));
        DailySmartTask recoveryPrimary=evidenceRepair.priority>=repair.priority?evidenceRepair:repair;
        DailySmartTask recoverySecondary=recoveryPrimary==evidenceRepair?repair:evidenceRepair;

        DailySmartTask article=nextArticleTask(today);
        boolean weakStoryDone=progress.dailyAuxiliaryAttempts("weak_story",today)>=1;
        int weakStoryPriority=(stats.wrong>=6||stats.stubborn>=3||stats.due>=20)?126:102;
        DailySmartTask weakStory=task("weak_story","📚","今日弱词微短文","从当前薄弱/到期词筛选6～10个 · 读 → 听 → 挖空 → 中文复述",8,"weak_word_story","",weakStoryDone,weakStoryPriority);
        boolean activeDone=progress.dailyAuxiliaryAttempts("active_recall",today)>=3;
        DailySmartTask active=task("active","✍️","主动回忆","不给选项，从中文完整想出意大利语",6,"active_recall","300",activeDone,(stats.weakestDimension()==ProgressStore.DIM_MEANING||stats.weakestDimension()==ProgressStore.DIM_SPELLING)?102:78);
        boolean shadowDone=progress.dailyAuxiliaryAttempts("shadowing",today)>=1;
        DailySmartTask shadow=task("shadow","🗣","Shadowing 三遍训练室","双语跟读 → 中文提取 → 裸说迁移 · 可录音回听",7,"shadow","",shadowDone,stats.weakestDimension()==ProgressStore.DIM_SPEAKING?116:78);
        DailySmartTask breakthrough=BreakthroughPlanEngine.todayTask(prescription,progress,today);

        // Weekly exam day is a deliberate exception to the normal route. At 20 minutes or
        // above, the exam replaces the full course step instead of becoming another add-on.
        // Due vocabulary is still protected. A 10-minute preference only receives the banner.
        if(weeklyExamDue&&budget>=20){
            // Weekly exam day keeps a tiny vocabulary safety slot, then spends the rest on
            // the 18-question practical check. This stays inside the user's chosen budget.
            DailySmartTask examVocab=task("vocab","🧠","复习 + 新词",stats.due+"个到期 · 周测日只做高风险复习与少量新词",5,"smart_memory","",vocabDone,145+Math.min(35,stats.due));
            plan.tasks.add(examVocab);addIfFitsStrict(plan.tasks,weeklyExam,budget);
            if(budget>=30&&recoveryNeeded)addIfFitsStrict(plan.tasks,recoveryPrimary,budget);
            if(budget>=45&&course!=null)addIfFitsStrict(plan.tasks,course,budget);
            if(budget>=60)addIfFitsStrict(plan.tasks,speak5,budget);
            return plan;
        }

        // Stable core: one course step and vocabulary. The remaining time is now filled by the
        // next-checkpoint prescription first, then by generic practice. Optional slots must fit
        // the chosen 10/20/30/45/60 minute budget instead of growing without limit.
        if(vocab.priority>=coursePriority(course)){plan.tasks.add(vocab);if(course!=null)plan.tasks.add(course);}else{if(course!=null)plan.tasks.add(course);plan.tasks.add(vocab);}
        if(budget<=10)return plan;

        DailySmartTask firstSkill=listen.priority>=grammar.priority?listen:grammar;
        DailySmartTask secondSkill=listen.priority>=grammar.priority?grammar:listen;
        if(budget<=20){addUniqueByAction(plan.tasks,recoveryNeeded?recoveryPrimary:(weeklyFocus!=null?weeklyFocus:breakthrough));return plan;}

        if(recoveryNeeded)addIfFitsUniqueAction(plan.tasks,recoveryPrimary,budget);
        // Weekly diagnosis gets first claim on the generic practice slot. 30 minutes and
        // above still protects the daily speaking habit. The three-day prescription uses
        // whatever time remains, so diagnosis never silently makes the plan over budget.
        addIfFitsUniqueAction(plan.tasks,weeklyFocus,budget);
        addIfFitsUniqueAction(plan.tasks,speak5,budget);
        addIfFitsUniqueAction(plan.tasks,breakthrough,budget);
        if(MasteryPassportEngine.ACTION_SPEAKING.equals(prescription.skillKey))addIfFits(plan.tasks,firstSkill,budget);

        if(budget>=45){
            if(recoveryNeeded)addIfFits(plan.tasks,recoverySecondary,budget);
            addIfFitsUniqueAction(plan.tasks,weakStory,budget);
            addIfFits(plan.tasks,firstSkill,budget);
            addIfFits(plan.tasks,secondSkill,budget);
            addIfFitsUniqueAction(plan.tasks,lifeTask,budget);
            if(!pressureProtection&&article!=null)addIfFits(plan.tasks,article,budget);
        }
        if(budget>=60){
            if(pressureProtection)addIfFits(plan.tasks,active,budget);else if(article!=null)addIfFits(plan.tasks,article,budget);
            addIfFits(plan.tasks,active,budget);
            addIfFits(plan.tasks,shadow,budget);
        }
        return plan;
    }

    private DailySmartTask weeklyFocusTask(LocalDate today){
        if(!progress.weeklyAdjustmentActive())return null;
        // The weakest skill gets roughly two thirds of the week; the second-weakest gets
        // every third day. This turns both diagnosis findings into real training priority.
        long since=Math.max(1,today.toEpochDay()-progress.lastWeeklyExamEpochDay());
        String primary=progress.weeklyFocusPrimary(),secondary=progress.weeklyFocusSecondary();
        String key=(since%3==0&&secondary!=null&&!secondary.isEmpty())?secondary:primary;
        String label=WeeklyExamEngine.skillLabel(key);String action="active_recall",payload="120";int minutes=6;boolean done=false;
        if(MasteryPassportEngine.ACTION_LISTENING.equals(key)){action="intensive_listening";done=progress.dailyAuxiliaryAttempts("intensive_listening",today)>=1;}
        else if(MasteryPassportEngine.ACTION_SPELLING.equals(key)){action="sentence_dictation";done=progress.dailyAuxiliaryAttempts("sentence_dictation",today)>=1;}
        else if(MasteryPassportEngine.ACTION_SPEAKING.equals(key)){if(since%2==0){action="shadow";minutes=7;done=progress.dailyAuxiliaryAttempts("shadowing",today)>=1;}else{action="daily_speaking";minutes=5;done=progress.dailyAuxiliaryAttempts("daily_speaking",today)>=DailySpeakingChallengeEngine.DAILY_TARGET;}}
        else if(MasteryPassportEngine.ACTION_GRAMMAR.equals(key)){action="smart_cloze";minutes=5;done=progress.dailyAuxiliaryAttempts("cloze",today)>=3;}
        else if(MasteryPassportEngine.ACTION_REAL_USE.equals(key)){long slot=Math.floorMod(since,3);if(slot==0){action="weak_word_story";payload="";minutes=8;done=progress.dailyAuxiliaryAttempts("weak_story",today)>=1;}else if(slot==1){LifeTask t=LifeTaskEngine.nextTask(progress);action="life_task";payload=t==null?"":t.id;minutes=7;done=LifeTaskEngine.completedStages(progress)>=36||progress.dailyAuxiliaryCorrect("life_task",today)>=1;}else{action="free_conversation";payload="";done=progress.dailyAuxiliaryAttempts("freechat",today)>=1;}}
        else done=progress.dailyAuxiliaryAttempts("active_recall",today)>=3;
        return task("week_focus","📌","周测补弱 · "+label,"最弱两项按 2:1 轮换 · 连续7天优先补 · 不突破总时长",minutes,action,payload,done,139);
    }

    private DailySmartTask nextArticleTask(LocalDate today){
        for(MemoryArticle a:memoryArticles.all())for(int i=0;i<a.sections.size();i++){
            MemoryArticleSection s=a.sections.get(i);if(progress.memoryArticleSentenceStudyDone(s.id))continue;
            boolean doneToday=progress.dailyAuxiliaryAttempts("memory_article_sentence",today)>=1;
            return task("article","📖","十篇逐句背诵","第"+(articleNumber(a)+1)+"篇 · 第"+(i+1)+"节 · "+s.titleZh,8,"memory_article_sentence",a.id+"|"+i,doneToday,84);
        }
        return null;
    }

    private int articleNumber(MemoryArticle target){List<MemoryArticle> all=memoryArticles.all();for(int i=0;i<all.size();i++)if(all.get(i)==target||all.get(i).id.equals(target.id))return i;return 0;}
    private int coursePriority(DailySmartTask t){return t==null?-1:t.priority;}
    private void addUnique(List<DailySmartTask> list,DailySmartTask task){if(task==null)return;for(DailySmartTask x:list)if(x.id.equals(task.id))return;list.add(task);}
    private void addUniqueByAction(List<DailySmartTask> list,DailySmartTask task){if(task==null)return;for(DailySmartTask x:list)if(x.id.equals(task.id)||x.action.equals(task.action))return;list.add(task);}
    private void addIfFitsUniqueAction(List<DailySmartTask> list,DailySmartTask task,int budget){if(task==null)return;for(DailySmartTask x:list)if(x.id.equals(task.id)||x.action.equals(task.action))return;int used=0;for(DailySmartTask x:list)used+=x.minutes;if(used+task.minutes<=budget)list.add(task);}
    private void addIfFitsStrict(List<DailySmartTask> list,DailySmartTask task,int budget){if(task==null)return;for(DailySmartTask x:list)if(x.id.equals(task.id))return;int used=0;for(DailySmartTask x:list)used+=x.minutes;if(used+task.minutes<=budget)list.add(task);}
    private void addIfFits(List<DailySmartTask> list,DailySmartTask task,int budget){if(task==null)return;for(DailySmartTask x:list)if(x.id.equals(task.id))return;int used=0;for(DailySmartTask x:list)used+=x.minutes;if(used+task.minutes<=budget+1)list.add(task);}
    private DailySmartTask task(String id,String emoji,String title,String subtitle,int minutes,String action,String payload,boolean done,int priority){return new DailySmartTask(id,emoji,title,subtitle,minutes,action,payload,done,priority);}
    private String dimName(int d){return d==ProgressStore.DIM_LISTENING?"听力":(d==ProgressStore.DIM_SPELLING?"拼写":(d==ProgressStore.DIM_SPEAKING?"口语":"识义"));}
}
