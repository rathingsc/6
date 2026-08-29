package com.italiano2774.nativeapp;

/**
 * v3.1 compact memory scheduler for the beginner-facing smart vocabulary flow.
 * Ratings are deliberately simple: 0 forgot, 1 fuzzy, 2 remembered.
 */
public final class SmartMemoryScheduler {
    private static final int[] REMEMBER_LADDER={1,3,7,15,30,60,120,240,480,960};
    private SmartMemoryScheduler() {}

    public static int nextInterval(int rating,int correctStreak,int currentLevel,int currentInterval){
        int r=Math.max(0,Math.min(2,rating));
        if(r==0)return 1;
        if(r==1)return currentLevel>=3?2:1;
        int success=Math.max(1,correctStreak+1);
        if(success<=REMEMBER_LADDER.length)return REMEMBER_LADDER[success-1];
        return Math.min(3650,Math.max(1,currentInterval)*2);
    }

    public static int nextMeaningLevel(int rating,int currentLevel){
        int r=Math.max(0,Math.min(2,rating));
        int cur=Math.max(0,Math.min(5,currentLevel));
        if(r==0)return Math.max(1,cur-1);
        if(r==1)return Math.max(1,cur);
        return Math.max(2,Math.min(5,cur+1));
    }
}
