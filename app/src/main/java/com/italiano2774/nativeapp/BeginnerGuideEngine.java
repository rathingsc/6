package com.italiano2774.nativeapp;

/**
 * v5.0 beginner shell. It does not change the real learning algorithms; it only controls
 * how much complexity is exposed to the learner during the first seven active study days.
 */
public final class BeginnerGuideEngine {
    private BeginnerGuideEngine(){}

    public static boolean active(ProgressStore p){return p!=null&&p.firstWeekGuidanceActive();}
    public static int day(ProgressStore p){return p==null?8:p.firstWeekLearningDay();}

    public static String coachLine(ProgressStore p,DailySmartPlan plan,DailySmartTask next){
        if(p==null)return "只需要完成系统安排的下一步。";
        if(active(p)){
            switch(day(p)){
                case 1:return "第1天 · 先完成今天安排，不需要研究所有功能。先建立每天学习的习惯。";
                case 2:return "第2天 · 今天开始多听意大利语。先听懂，再追求说得快。";
                case 3:return "第3天 · 今天开始主动开口。说不完整也没关系，系统会保留真正的薄弱点。";
                case 4:return "第4天 · 做错的词和句子会自动回来。重新做对，比反复看答案更重要。";
                case 5:return "第5天 · 开始把学过的表达放进真实生活场景，目标是能把事情办成。";
                case 6:return "第6天 · 系统已经开始根据你的真实表现调整听力、拼写、口语和复习量。";
                default:return "第7天 · 完成今天后，完整周测和学习分析会接管后续节奏。你仍然只需要点“继续学习”。";
            }
        }
        if(plan!=null&&plan.recoveryMode)return "今天先把容易忘和反复错的内容处理掉，系统已经自动减少新内容。";
        if(next!=null)return "系统已经按今天的复习压力和薄弱项排好顺序。完成这一项，回来继续下一项即可。";
        return "今天的主任务已经完成。可以查看总结，也可以停止学习，让记忆休息和巩固。";
    }

    public static String practiceHint(ProgressStore p){
        if(!active(p))return "主课程已经自动安排复习。这里只在你想额外练某一项时使用。";
        return "第"+day(p)+"个学习日 · 系统会在每日计划里自动安排需要的训练；额外练习会逐日开放，不需要一次学会所有功能。";
    }

    public static boolean practiceUnlocked(ProgressStore p,int unlockDay){
        return !active(p)||day(p)>=Math.max(1,unlockDay);
    }

    public static String lockedLabel(String label,int unlockDay){return "🔒 "+label+" · 第"+unlockDay+"天开放";}
}
