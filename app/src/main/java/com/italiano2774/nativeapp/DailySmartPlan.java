package com.italiano2774.nativeapp;

import java.util.ArrayList;
import java.util.List;

/** Small immutable-ish view model for today's automatically composed study route. */
public class DailySmartPlan {
    public final List<DailySmartTask> tasks=new ArrayList<>();
    public int targetMinutes;
    public String focus="";
    public int dueWords;
    public int tomorrowScheduledWords;
    public int newWords;
    public int wrongWords;
    public int stubbornWords;
    public boolean recoveryMode;
    public String breakthroughFocus="";
    public String breakthroughSummary="";

    public int completed(){int n=0;for(DailySmartTask t:tasks)if(t.done)n++;return n;}
    public int estimatedMinutes(){int n=0;for(DailySmartTask t:tasks)n+=t.minutes;return n;}
    public int remainingMinutes(){int n=0;for(DailySmartTask t:tasks)if(!t.done)n+=t.minutes;return n;}
    public int progressPercent(){return tasks.isEmpty()?100:(int)Math.round(completed()*100.0/tasks.size());}
    public DailySmartTask next(){for(DailySmartTask t:tasks)if(!t.done)return t;return null;}
}
