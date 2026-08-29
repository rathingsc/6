package com.italiano2774.nativeapp;

import java.util.List;

/** Maps existing three-level dialogue evidence into a 36-stage real-life mission map. */
public final class LifeTaskEngine {
    private LifeTaskEngine(){}
    public static int passLine(int level){return level<=1?80:(level==2?75:70);}
    public static int score(ProgressStore p,LifeTask task,int level){return p==null||task==null?0:p.dialogueScenarioBestScore(task.scenarioId,Math.max(1,Math.min(3,level)));}
    public static boolean passed(ProgressStore p,LifeTask task,int level){return score(p,task,level)>=passLine(level);}
    public static boolean unlocked(ProgressStore p,LifeTask task,int level){if(level<=1)return true;if(level==2)return passed(p,task,1);return passed(p,task,1)&&passed(p,task,2);}
    public static int completedStages(ProgressStore p,LifeTask task){int n=0;for(int level=1;level<=3;level++)if(passed(p,task,level))n++;return n;}
    public static int nextLevel(ProgressStore p,LifeTask task){for(int level=1;level<=3;level++)if(!passed(p,task,level))return level;return 4;}
    public static int completedStages(ProgressStore p){int n=0;for(LifeTask t:LifeTaskRepository.all())n+=completedStages(p,t);return n;}
    public static int masteredTasks(ProgressStore p){int n=0;for(LifeTask t:LifeTaskRepository.all())if(passed(p,t,3))n++;return n;}
    public static LifeTask nextTask(ProgressStore p){
        List<LifeTask> all=LifeTaskRepository.all();
        // First finish any task whose lower stages are already underway.
        for(LifeTask t:all){int next=nextLevel(p,t);if(next==2||next==3)return t;}
        // Otherwise move through the curated everyday-life order.
        for(LifeTask t:all)if(nextLevel(p,t)<=3)return t;
        return null;
    }
    public static String levelTitle(int level){return level<=1?"基础应对":(level==2?"独立表达":"真实实战");}
    public static String levelDetail(int level){return level<=1?"先听懂，再用选项辅助并开口":(level==2?"没有选项，只看中文自己组织意大利语":"不看字幕和中文，只听对方后自由回答");}
    public static String compactProgress(ProgressStore p,LifeTask task){
        return "基础 "+score(p,task,1)+"% · 独立 "+score(p,task,2)+"% · 实战 "+score(p,task,3)+"%";
    }
}
