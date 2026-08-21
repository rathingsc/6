package com.italiano2774.nativeapp;
public class LearningPathNode {
    public String id,emoji,title,subtitle,action,payload,reason="";public int minutes,priority;public boolean done;
    public LearningPathNode(String id,String emoji,String title,String subtitle,int minutes,String action,String payload,boolean done){this(id,emoji,title,subtitle,minutes,action,payload,done,0,"");}
    public LearningPathNode(String id,String emoji,String title,String subtitle,int minutes,String action,String payload,boolean done,int priority,String reason){this.id=id;this.emoji=emoji;this.title=title;this.subtitle=subtitle;this.minutes=minutes;this.action=action;this.payload=payload;this.done=done;this.priority=priority;this.reason=reason==null?"":reason;}
}
