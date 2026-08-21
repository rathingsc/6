package com.italiano2774.nativeapp;
import android.content.Context;
/** Measures real foreground time instead of estimating minutes from card counts. */
public final class StudyTimeTracker{
 private final ProgressStore progress;private long started=-1;public StudyTimeTracker(Context c){progress=new ProgressStore(c);}
 public void resume(){if(started<0)started=android.os.SystemClock.elapsedRealtime();}
 public void pause(){if(started<0)return;long sec=Math.max(0,(android.os.SystemClock.elapsedRealtime()-started)/1000);started=-1;if(sec>=2)progress.addActiveSeconds(sec);}
}
