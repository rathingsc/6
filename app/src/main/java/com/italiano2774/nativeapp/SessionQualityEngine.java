package com.italiano2774.nativeapp;

import java.util.List;

/** Small deterministic rules for session pacing. No cloud/AI dependency. */
public final class SessionQualityEngine {
    private SessionQualityEngine(){}

    /**
     * Fatigue is intentionally conservative: only react after enough answers and
     * when both difficulty and slower responses point in the same direction.
     */
    public static boolean isFatigued(List<Integer> ratings,List<Long> responseMs){
        if(ratings==null||responseMs==null)return false;
        int n=Math.min(ratings.size(),responseMs.size());if(n<6)return false;
        int from=Math.max(0,n-8),hard=0;long total=0;int timed=0;
        for(int i=from;i<n;i++){
            int r=ratings.get(i)==null?2:ratings.get(i);if(r<=1)hard++;
            long ms=responseMs.get(i)==null?0L:Math.max(0L,responseMs.get(i));if(ms>0){total+=ms;timed++;}
        }
        int window=n-from;long avg=timed==0?0:total/timed;
        return hard>=Math.max(4,(int)Math.ceil(window*0.55)) || (hard>=3&&avg>=9000L);
    }

    public static int suggestedFinishAfter(){return 5;}
}
