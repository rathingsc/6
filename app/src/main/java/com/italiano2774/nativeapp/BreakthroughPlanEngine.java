package com.italiano2774.nativeapp;

import android.content.Context;
import java.time.LocalDate;

/**
 * v4.8.0 keeps the three-day prescription progressive and uses the real-life task map for guided transfer.
 * The focus skill is locked for a completed three-day cycle, each day uses a
 * different retrieval/transfer route, and the learner can see baseline -> now.
 */
public final class BreakthroughPlanEngine {
    public static class Plan {
        public String targetLevel="A1";
        public String skillKey=MasteryPassportEngine.ACTION_MEANING;
        public String skillLabel="识义";
        public int currentScore,targetScore,gap,baselineScore,gain,cycleDay=1,completedCycles=0;
        public boolean waitingForTomorrow=false,cycleComplete=false;
        public String headline="",summary="",cycleStatus="";
        public String day1="",day2="",day3="";
        public String taskId="active",emoji="🎯",title="识义突破",subtitle="",action="active_recall",payload="8",buttonLabel="开始今日突破";
        public int minutes=5,priority=124;

        public String threeDayText(){
            return markDay(1,day1)+"\n"+markDay(2,day2)+"\n"+markDay(3,day3);
        }
        private String markDay(int day,String text){
            if(cycleComplete)return "✓ 第"+day+"天："+text;
            if(day<cycleDay)return "✓ 第"+day+"天："+text;
            if(day==cycleDay)return (waitingForTomorrow?"→":"▶")+" 第"+day+"天："+text;
            return "○ 第"+day+"天："+text;
        }
    }

    private BreakthroughPlanEngine(){}

    public static Plan build(Context context,WordRepository repo,ProgressStore progress){
        LocalDate today=LocalDate.now();
        MasteryPassportEngine.Snapshot passport=MasteryPassportEngine.build(context,repo,progress);
        String suggestedKey=passport.focusSkillKey==null?MasteryPassportEngine.ACTION_MEANING:passport.focusSkillKey;
        MasteryPassportEngine.Skill suggestedSkill=passport.skill(suggestedKey);
        int suggestedScore=suggestedSkill==null?passport.focusSkillScore:suggestedSkill.score;
        ProgressStore.BreakthroughState state=progress.syncBreakthroughCycle(suggestedKey,passport.targetLevel,suggestedScore,today);

        Plan out=fromState(passport,state);
        if(state.cycleComplete){
            out.cycleComplete=true;out.waitingForTomorrow=true;out.cycleDay=3;
            configurePhase(context,progress,out,3);
            decorate(out,"本轮3天已完成，明天重新评估六项能力并开启下一轮短板。",true);
            return out;
        }

        configurePhase(context,progress,out,state.phase);
        if(state.waitingToday){
            out.waitingForTomorrow=true;
            decorate(out,"今天这一阶段已经完成。为了让记忆真正间隔开，下一阶段明天再做，不在同一天连刷三关。",false);
            return out;
        }

        if(isTodayTaskComplete(out,progress,today)){
            progress.completeBreakthroughPhase(today);
            ProgressStore.BreakthroughState advanced=progress.breakthroughState(today);
            out.waitingForTomorrow=true;
            out.cycleComplete=advanced.cycleComplete;
            decorate(out,advanced.cycleComplete?"第3天迁移任务已完成。本轮结束，明天按最新能力重新选短板。":"今天这一阶段已经完成，明天自动进入第"+advanced.phase+"天。",advanced.cycleComplete);
            return out;
        }

        decorate(out,"本轮短板锁定3个完成日，不会因为单次分数波动频繁换目标。",false);
        return out;
    }

    private static Plan fromState(MasteryPassportEngine.Snapshot passport,ProgressStore.BreakthroughState state){
        Plan out=new Plan();out.targetLevel=state.targetLevel==null?passport.targetLevel:state.targetLevel;out.skillKey=state.skillKey==null||state.skillKey.isEmpty()?MasteryPassportEngine.ACTION_MEANING:state.skillKey;
        MasteryPassportEngine.Skill skill=passport.skill(out.skillKey);out.skillLabel=skill==null?labelFor(out.skillKey):skill.label;out.currentScore=skill==null?0:skill.score;out.targetScore=MasteryPassportEngine.targetFor(out.targetLevel,out.skillKey);out.gap=Math.max(0,out.targetScore-out.currentScore);out.baselineScore=state.baselineScore;out.gain=out.currentScore-out.baselineScore;out.cycleDay=Math.max(1,Math.min(3,state.phase));out.completedCycles=state.completedCycles;return out;
    }

    private static void decorate(Plan out,String note,boolean complete){
        String change=(out.gain>0?"+":"")+out.gain;
        out.headline="🎯 "+out.targetLevel+" 三日突破 · "+out.skillLabel+" · "+(complete?"本轮完成":"第"+out.cycleDay+"/3天");
        out.cycleStatus="本轮起点 "+out.baselineScore+" → 当前 "+out.currentScore+"（"+change+"） · 目标 "+out.targetScore+" · 已完成 "+out.completedCycles+" 轮";
        out.summary=out.cycleStatus+"\n"+note+(out.gap>0?" 目前距阶段线还差 "+out.gap+" 分。":" 当前已达到这一项的阶段线，仍完成迁移日来验证能否真正用出来。");
        if(complete){out.buttonLabel="✓ 本轮已完成 · 明天重新评估";out.subtitle=out.cycleStatus+" · 明天开启新一轮";}
        else if(out.waitingForTomorrow){out.buttonLabel="✓ 今日突破已完成 · 明天继续";out.subtitle=out.cycleStatus+" · 今天不重复刷下一阶段";}
        else out.subtitle="第"+out.cycleDay+"天 · "+out.cycleStatus+" · "+phaseText(out,out.cycleDay);
    }

    private static void configurePhase(Context context,ProgressStore p,Plan out,int phase){
        configureRoadmap(context,p,out);
        int day=Math.max(1,Math.min(3,phase));out.cycleDay=day;
        if(MasteryPassportEngine.ACTION_LISTENING.equals(out.skillKey)){
            if(day==1)set(out,"listen","🎧","听力突破 · 第1天","listen_speak","",6,139,"开始第1天听力","听词 + 听句6题，先不看字幕");
            else if(day==2)set(out,"intensive","🎧✍️","听力突破 · 第2天","intensive_listening","",6,140,"开始第2天精听","正常速听写，再用慢速核对");
            else set(out,"scenario","🎭","听力突破 · 第3天","dialogue",scenarioId(context,p),5,141,"开始第3天迁移","真实会话先听对方，再回答");
        }else if(MasteryPassportEngine.ACTION_SPELLING.equals(out.skillKey)){
            if(day==1)set(out,"dictation","✍️","拼写突破 · 第1天","sentence_dictation","",6,139,"开始第1天听写","做6句听写，错字必须重写");
            else if(day==2)set(out,"active","🧠","拼写突破 · 第2天","active_recall","8",5,140,"开始第2天反推","中文反推意大利语，先回忆再看答案");
            else set(out,"writing","✍️","拼写突破 · 第3天","writing","",6,141,"开始第3天写作","写3句生活表达，把易错词真正用出来");
        }else if(MasteryPassportEngine.ACTION_SPEAKING.equals(out.skillKey)){
            if(day==1)set(out,"speak5","🗣","口语突破 · 第1天","daily_speaking","",5,143,"完成第1天5句","只看中文说5句，不提前看意大利语");
            else if(day==2)set(out,"shadow","🗣","口语突破 · 第2天","shadow","",7,144,"开始第2天跟读","三遍递进：双语跟读 → 中文提取 → 裸说迁移");
            else set(out,"freechat","🗣","口语突破 · 第3天","free_conversation",scenarioId(context,p),6,145,"开始第3天自由说","自由会话复述同类生活意图，不靠固定选项");
        }else if(MasteryPassportEngine.ACTION_GRAMMAR.equals(out.skillKey)){
            GrammarPoint gp=GrammarDiagnostics.recommended(p);String pattern=(gp==null||gp.practicePatternId==null)?"vorrei":gp.practicePatternId;
            if(day==1)set(out,"grammar","🧩","语法突破 · 第1天","grammar",pattern,4,137,"开始第1天微语法",(gp==null?"高频生活句型":gp.title)+"：看1条规则，马上做3题");
            else if(day==2)set(out,"cloze","🧩✍️","语法突破 · 第2天","smart_cloze","",5,138,"开始第2天完形","把同一结构放进无选项完形和句子判断");
            else set(out,"writing","✍️","语法突破 · 第3天","writing","",6,139,"开始第3天迁移","写生活表达，主动把句型放进真实语境");
        }else if(MasteryPassportEngine.ACTION_REAL_USE.equals(out.skillKey)){
            if(day==1)set(out,"life_task","🗺","真实使用突破 · 第1天","life_task",lifeTaskId(p),7,143,"开始第1天生活任务","完成任务地图中当前最值得推进的一关");
            else if(day==2)set(out,"weakstory","📚","真实使用突破 · 第2天","weak_word_story","",8,142,"开始第2天弱词微短文","把当前薄弱词放回已审校短文，读、听、挖空后只看中文复述");
            else set(out,"freechat","🗣","真实使用突破 · 第3天","free_conversation",scenarioId(context,p),6,143,"开始第3天自由会话","自由说同类场景，把词汇和语法一起调出来");
        }else{
            if(day==1)set(out,"active","🧠","识义突破 · 第1天","active_recall","8",5,135,"开始第1天回忆","中文→意大利语主动回忆8题");
            else if(day==2)set(out,"cloze","🧩","识义突破 · 第2天","smart_cloze","",5,136,"开始第2天语境提取","把词放进句子，不靠单独中文提示判断");
            else set(out,"freechat","🎭","识义突破 · 第3天","free_conversation",scenarioId(context,p),6,137,"开始第3天迁移","在真实场景里主动调出这些词，不只停留在认词");
        }
    }

    private static void configureRoadmap(Context context,ProgressStore p,Plan out){
        if(MasteryPassportEngine.ACTION_LISTENING.equals(out.skillKey)){out.day1="听词 + 听句6题，先不看字幕";out.day2="正常速精听听写，再对照错误";out.day3="真实会话先听对方，再回答";}
        else if(MasteryPassportEngine.ACTION_SPELLING.equals(out.skillKey)){out.day1="句子听写，错字必须重写";out.day2="中文反推意大利语，不给选项";out.day3="写生活表达，把易错词用出来";}
        else if(MasteryPassportEngine.ACTION_SPEAKING.equals(out.skillKey)){out.day1="只看中文说5句";out.day2="Shadowing三遍递进，最后脱离字幕裸说";out.day3="自由会话，不靠固定答案";}
        else if(MasteryPassportEngine.ACTION_GRAMMAR.equals(out.skillKey)){GrammarPoint gp=GrammarDiagnostics.recommended(p);out.day1=(gp==null?"高频生活句型":gp.title)+"：规则后立刻做题";out.day2="用完形再次提取同一结构";out.day3="写生活表达，主动迁移句型";}
        else if(MasteryPassportEngine.ACTION_REAL_USE.equals(out.skillKey)){out.day1="推进真实生活任务地图当前关";out.day2="用当前薄弱词完成微短文读听挖空与复述";out.day3="自由会话整合词汇与语法";}
        else{out.day1="中文→意大利语主动回忆";out.day2="放进句子做语境提取";out.day3="真实场景主动调出这些词";}
    }

    private static void set(Plan out,String taskId,String emoji,String title,String action,String payload,int minutes,int priority,String button,String phaseText){out.taskId=taskId;out.emoji=emoji;out.title=title;out.action=action;out.payload=payload==null?"":payload;out.minutes=minutes;out.priority=priority;out.buttonLabel=button;}
    private static String phaseText(Plan out,int day){return day==1?out.day1:(day==2?out.day2:out.day3);}
    private static String scenarioId(Context context,ProgressStore p){PersonalizedCourse pc=new PersonalizedCourseEngine(context,WordRepository.get(context),p).buildOverview(p.dashboardStats(WordRepository.get(context).all(),LocalDate.now()));return pc.scenarioId==null?"":pc.scenarioId;}
    private static String lifeTaskId(ProgressStore p){LifeTask t=LifeTaskEngine.nextTask(p);return t==null?"":t.id;}

    private static boolean isTodayTaskComplete(Plan plan,ProgressStore p,LocalDate today){
        if("listen_speak".equals(plan.action))return p.dailyAuxiliaryAttempts("listen_speak",today)>=4;
        if("intensive_listening".equals(plan.action))return p.dailyAuxiliaryAttempts("intensive_listening",today)>=2;
        if("sentence_dictation".equals(plan.action))return p.dailyAuxiliaryAttempts("sentence_dictation",today)>=3;
        if("daily_speaking".equals(plan.action))return p.dailyAuxiliaryAttempts("daily_speaking",today)>=DailySpeakingChallengeEngine.DAILY_TARGET;
        if("shadow".equals(plan.action))return p.dailyAuxiliaryAttempts("shadowing",today)>=1;
        if("grammar".equals(plan.action))return p.dailyAuxiliaryAttempts("pattern",today)>=3;
        if("smart_cloze".equals(plan.action))return p.dailyAuxiliaryAttempts("cloze",today)>=3;
        if("dialogue".equals(plan.action))return p.dailyAuxiliaryAttempts("dialogue_scenario",today)>=1||p.dailyAuxiliaryAttempts("dialogue_speaking",today)>=3;
        if("life_task".equals(plan.action))return p.dailyAuxiliaryCorrect("life_task",today)>=1;
        if("free_conversation".equals(plan.action))return p.dailyAuxiliaryAttempts("freechat",today)>=2;
        if("writing".equals(plan.action))return p.dailyAuxiliaryAttempts("writing",today)>=1;
        if("weak_word_story".equals(plan.action))return p.dailyAuxiliaryAttempts("weak_story",today)>=1;
        return p.dailyAuxiliaryAttempts("active_recall",today)>=3;
    }

    public static DailySmartTask todayTask(Plan plan,ProgressStore p,LocalDate today){
        boolean done=plan.waitingForTomorrow||plan.cycleComplete||isTodayTaskComplete(plan,p,today);
        String sub=plan.subtitle;if(done&&plan.cycleComplete)sub="本轮3天已完成 · 明天重新评估六项能力";else if(done&&plan.waitingForTomorrow)sub="今天的突破已完成 · 下一阶段明天再做";
        return new DailySmartTask(plan.taskId,plan.emoji,plan.title,sub,plan.minutes,plan.action,plan.payload,done,plan.priority);
    }

    private static String labelFor(String key){if(MasteryPassportEngine.ACTION_LISTENING.equals(key))return "听力";if(MasteryPassportEngine.ACTION_SPELLING.equals(key))return "拼写";if(MasteryPassportEngine.ACTION_SPEAKING.equals(key))return "口语";if(MasteryPassportEngine.ACTION_GRAMMAR.equals(key))return "语法";if(MasteryPassportEngine.ACTION_REAL_USE.equals(key))return "真实使用";return "识义";}
}
