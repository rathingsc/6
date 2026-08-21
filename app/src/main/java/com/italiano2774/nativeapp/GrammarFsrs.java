package com.italiano2774.nativeapp;
import android.content.Context;import java.time.LocalDate;
/** Small local scheduler for grammar points; SharedPreferences remains a compatibility mirror while Room stores the durable row. */
public final class GrammarFsrs{
 private GrammarFsrs(){}
 public static GrammarProgressEntity next(GrammarProgressEntity old,String id,boolean correct){GrammarProgressEntity x=old==null?new GrammarProgressEntity():old;x.grammarId=id;x.attempts++;if(correct)x.correct++;x.lastEpochDay=LocalDate.now().toEpochDay();if(correct){x.difficulty=Math.max(1,x.difficulty-.2);x.stability=Math.max(1,x.stability*(x.attempts<=2?1.8:1.5));x.intervalDays=Math.max(1,Math.min(120,(int)Math.round(x.stability)));}else{x.difficulty=Math.min(10,x.difficulty+.75);x.stability=Math.max(1,x.stability*.42);x.intervalDays=1;}x.dueEpochDay=LocalDate.now().plusDays(x.intervalDays).toEpochDay();x.updatedAt=System.currentTimeMillis();return x;}
 public static void record(Context c,String id,boolean correct){Context app=c.getApplicationContext();new Thread(()->{try{LearningStateDao d=LearningDatabase.get(app).learningStateDao();d.upsertGrammar(next(d.grammar(id),id,correct));}catch(Exception ignored){}}).start();}
}
