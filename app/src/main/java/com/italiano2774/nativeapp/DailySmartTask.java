package com.italiano2774.nativeapp;

/** One concrete item in the beginner-first daily smart plan. */
public class DailySmartTask {
    public final String id;
    public final String emoji;
    public final String title;
    public final String subtitle;
    public final int minutes;
    public final String action;
    public final String payload;
    public final boolean done;
    public final int priority;

    public DailySmartTask(String id,String emoji,String title,String subtitle,int minutes,String action,String payload,boolean done,int priority){
        this.id=id;this.emoji=emoji;this.title=title;this.subtitle=subtitle;this.minutes=minutes;this.action=action;this.payload=payload==null?"":payload;this.done=done;this.priority=priority;
    }
}
