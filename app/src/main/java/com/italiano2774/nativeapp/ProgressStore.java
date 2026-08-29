package com.italiano2774.nativeapp;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Persistent learning state. v1.9 combines four-dimensional mastery with an
 * FSRS-inspired scheduler, adaptive new-word load, checkpoint exams and Room event logging.
 */
public class ProgressStore {
    public static final String DEFAULT_START="2026-08-15";
    public static final int DIM_MEANING=0, DIM_LISTENING=1, DIM_SPELLING=2, DIM_SPEAKING=3;
    public static final int PRON_ALWAYS=0, PRON_AUTO=1, PRON_TAP=2, PRON_NEVER=3;
    public static final int ROUTE_1000=1000, ROUTE_1600=1600, ROUTE_2774=2774;
    public static final int GOAL_LIFE=0, GOAL_WORK=1, GOAL_A2=2, GOAL_B1=3, GOAL_BALANCED=4;
    private final SharedPreferences p;
    private final Context context;

    public ProgressStore(Context c){context=c.getApplicationContext();p=context.getSharedPreferences("italiano2774_native",Context.MODE_PRIVATE);}
    private int clamp(int x,int lo,int hi){return Math.max(lo,Math.min(hi,x));}
    private int defaultInterval(int level){switch(level){case 1:return 1;case 2:return 3;case 3:return 7;case 4:return 14;case 5:return 30;default:return 1;}}
    private String dimPrefix(int dim){switch(dim){case DIM_LISTENING:return "dl_";case DIM_SPELLING:return "ds_";case DIM_SPEAKING:return "dp_";default:return "dm_";}}
    private String dimKey(int id,int dim){return dimPrefix(dim)+id;}
    private String dimScheduleKey(String field,int id,int dim){return "df_"+field+"_"+dim+"_"+id;}
    private String forgettingFactorKey(int dim){return "forget_factor_"+dim;}
    private String forgettingObservationKey(int dim){return "forget_obs_"+dim;}
    private boolean hasDimensionSchedule(int id,int dim){return p.contains(dimScheduleKey("due",id,dim))||p.contains(dimScheduleKey("last",id,dim))||p.contains(dimScheduleKey("s",id,dim));}
    private void ensureDimensionSchedules(int id){
        long legacyDue=p.getLong("due_"+id,Long.MIN_VALUE),legacyLast=p.getLong("last_"+id,Long.MIN_VALUE);int legacyIv=clamp(p.getInt("iv_"+id,mastery(id)>0?defaultInterval(mastery(id)):1),1,3650);float legacyS=p.getFloat("fs_s_"+id,Math.max(1f,legacyIv)),legacyD=p.getFloat("fs_d_"+id,5.5f);SharedPreferences.Editor e=p.edit();boolean changed=false;
        for(int dim=0;dim<4;dim++){if(dimensionLevel(id,dim)<=0||hasDimensionSchedule(id,dim))continue;changed=true;e.putInt(dimScheduleKey("iv",id,dim),legacyIv).putFloat(dimScheduleKey("s",id,dim),legacyS).putFloat(dimScheduleKey("d",id,dim),legacyD);if(legacyDue!=Long.MIN_VALUE)e.putLong(dimScheduleKey("due",id,dim),legacyDue);if(legacyLast!=Long.MIN_VALUE)e.putLong(dimScheduleKey("last",id,dim),legacyLast);}
        if(changed)e.apply();
    }

    // ---------- Overall + four-dimensional mastery ----------
    public int mastery(int id){return clamp(p.getInt("m_"+id,0),0,5);}
    public int dimensionLevel(int id,int dim){String k=dimKey(id,dim);return clamp(p.contains(k)?p.getInt(k,0):mastery(id),0,5);}
    public int meaningLevel(int id){return dimensionLevel(id,DIM_MEANING);}
    public int listeningLevel(int id){return dimensionLevel(id,DIM_LISTENING);}
    public int spellingLevel(int id){return dimensionLevel(id,DIM_SPELLING);}
    public int speakingLevel(int id){return dimensionLevel(id,DIM_SPEAKING);}

    public void setMastery(int id,int v){
        int level=clamp(v,0,5);SharedPreferences.Editor e=p.edit().putInt("m_"+id,level)
                .putInt(dimKey(id,DIM_MEANING),level).putInt(dimKey(id,DIM_LISTENING),level)
                .putInt(dimKey(id,DIM_SPELLING),level).putInt(dimKey(id,DIM_SPEAKING),level);
        if(level==0){
            e.remove("due_"+id).remove("iv_"+id).remove("fs_s_"+id).remove("fs_d_"+id).remove("last_"+id);
            for(int dim=0;dim<4;dim++)for(String field:new String[]{"due","iv","s","d","last"})e.remove(dimScheduleKey(field,id,dim));
        }else{
            int iv=defaultInterval(level);long today=LocalDate.now().toEpochDay(),due=today+iv;e.putInt("iv_"+id,iv).putLong("due_"+id,due).putFloat("fs_s_"+id,Math.max(1f,iv)).putFloat("fs_d_"+id,5.5f);
            for(int dim=0;dim<4;dim++)e.putInt(dimScheduleKey("iv",id,dim),iv).putLong(dimScheduleKey("due",id,dim),due).putFloat(dimScheduleKey("s",id,dim),Math.max(1f,iv)).putFloat(dimScheduleKey("d",id,dim),5.5f);
        }
        e.apply();recordDailyCard();mirrorWordState(id);
    }

    public void setDimensions(int id,int meaning,int listening,int spelling,int speaking){
        p.edit().putInt(dimKey(id,DIM_MEANING),clamp(meaning,0,5)).putInt(dimKey(id,DIM_LISTENING),clamp(listening,0,5))
                .putInt(dimKey(id,DIM_SPELLING),clamp(spelling,0,5)).putInt(dimKey(id,DIM_SPEAKING),clamp(speaking,0,5)).apply();
        recomputeOverall(id);mirrorWordState(id);
    }

    private void recomputeOverall(int id){
        int m=meaningLevel(id),l=listeningLevel(id),s=spellingLevel(id),sp=speakingLevel(id);
        int overall=clamp((int)Math.round(m*0.40+l*0.25+s*0.20+sp*0.15),0,5);
        p.edit().putInt("m_"+id,overall).apply();
    }

    public int weakestDimension(int id){
        int[] v={meaningLevel(id),listeningLevel(id),spellingLevel(id),speakingLevel(id)};int idx=0;for(int i=1;i<v.length;i++)if(v[i]<v[idx])idx=i;return idx;
    }
    public String weakestDimensionName(int id){switch(weakestDimension(id)){case DIM_LISTENING:return "听力";case DIM_SPELLING:return "拼写";case DIM_SPEAKING:return "口语";default:return "识义";}}
    public boolean b1Ready(int id){return meaningLevel(id)>=4&&listeningLevel(id)>=3&&spellingLevel(id)>=2&&speakingLevel(id)>=2;}
    public int dimensionAverage(List<Word> words,int dim){if(words.isEmpty())return 0;long n=0;for(Word w:words)n+=dimensionLevel(w.id,dim);return (int)Math.round(n*100.0/(words.size()*5.0));}

    /** Records one exercise in one dimension and updates adaptive scheduling once. */
    public void recordDimensionResult(int id,int dim,boolean correct,long responseMs){recordDimensionResults(id,new int[]{dim},correct,responseMs);}
    /** Records a word dimension from an embedded sentence/listening task without double-counting the same UI question in daily totals. */
    public void recordEmbeddedDimensionResult(int id,int dim,boolean correct,long responseMs){recordEmbeddedDimensionResults(id,new int[]{dim},correct,responseMs);}
    /** Updates several dimensions from one embedded question while advancing the adaptive schedule only once. */
    public void recordEmbeddedDimensionResults(int id,int[] dims,boolean correct,long responseMs){
        SharedPreferences.Editor e=p.edit();
        for(int dim:dims){int cur=dimensionLevel(id,dim),next;if(correct){next=Math.min(5,cur+(responseMs>0&&responseMs>9000?0:1));if(next==0)next=1;}else next=Math.max(cur>0?1:0,cur-1);e.putInt(dimKey(id,dim),next);}e.apply();
        recomputeOverall(id);updateAdaptiveSchedule(id,dims,correct,responseMs);if(!correct)p.edit().remove("grad_"+id).apply();
        for(int dim:dims)LearningDatabase.log(context,"word_embedded",String.valueOf(id),dim,correct,responseMs,"listen_speak");mirrorWordState(id);
    }
    public void recordDimensionResults(int id,int[] dims,boolean correct,long responseMs){
        SharedPreferences.Editor e=p.edit();
        for(int dim:dims){int cur=dimensionLevel(id,dim),next;if(correct){next=Math.min(5,cur+(responseMs>0&&responseMs>9000?0:1));if(next==0)next=1;}else next=Math.max(cur>0?1:0,cur-1);e.putInt(dimKey(id,dim),next);}e.apply();
        recomputeOverall(id);updateAdaptiveSchedule(id,dims,correct,responseMs);if(!correct)p.edit().remove("grad_"+id).apply();recordDailyPractice(correct,responseMs);
        for(int dim:dims)LearningDatabase.log(context,"word",String.valueOf(id),dim,correct,responseMs,"dimension");mirrorWordState(id);
    }

    /** Backwards-compatible generic exercise = meaning recognition. */
    public void recordWrong(int id){recordDimensionResult(id,DIM_MEANING,false,0L);}
    public void recordCorrect(int id){recordDimensionResult(id,DIM_MEANING,true,0L);}
    public void recordPracticeResult(int id,boolean correct,long responseMs){recordDimensionResult(id,DIM_MEANING,correct,responseMs);}

    private void updateAdaptiveSchedule(int id,boolean correct,long responseMs){updateAdaptiveSchedule(id,new int[]{DIM_MEANING},correct,responseMs);}

    /** v4.9: one UI answer can provide evidence for several dimensions; each dimension advances exactly once. */
    private void updateAdaptiveSchedule(int id,int[] dims,boolean correct,long responseMs){
        int att=attempts(id)+1,cor=correctAnswers(id)+(correct?1:0);
        long oldAvg=avgResponseMs(id),avg=responseMs>0?(oldAvg<=0?responseMs:Math.round(oldAvg*0.75+responseMs*0.25)):oldAvg;
        int quality=!correct?1:(responseMs>0&&responseMs>9000?2:(responseMs>0&&responseMs<3000?4:3));
        java.util.LinkedHashSet<Integer> unique=new java.util.LinkedHashSet<>();for(int dim:dims)unique.add(clamp(dim,0,3));
        for(Integer dim:unique)updateDimensionSchedule(id,dim,correct,quality);
        LocalDate today=LocalDate.now();SharedPreferences.Editor e=p.edit().putInt("att_"+id,att).putInt("cor_"+id,cor).putLong("last_"+id,today.toEpochDay()).putLong("avgms_"+id,avg);
        if(correct)e.putInt("w_"+id,Math.max(0,wrongCount(id)-1)).putInt("ok_"+id,correctStreak(id)+1);else e.putInt("w_"+id,wrongCount(id)+1).putInt("ok_"+id,0);e.apply();
        syncAggregateSchedule(id);
    }

    private void updateDimensionSchedule(int id,int dim,boolean correct,int quality){
        ensureDimensionSchedules(id);LocalDate today=LocalDate.now(),last=dimensionLastReviewed(id,dim);int elapsed=last==null?0:(int)Math.max(0,java.time.temporal.ChronoUnit.DAYS.between(last,today));
        double stability=dimensionMemoryStability(id,dim),difficulty=dimensionMemoryDifficulty(id,dim),beforeR=stability>0?FsrsScheduler.retrievability(stability,elapsed):(dimensionLevel(id,dim)>0?0.80:0.0);
        FsrsScheduler.Result fs=FsrsScheduler.schedule(stability,difficulty,quality,elapsed,desiredRetention());
        double factor=PersonalForgettingModel.updateFactor(forgettingFactor(dim),correct,beforeR,quality);int interval=PersonalForgettingModel.scaledInterval(fs.intervalDays,factor,quality);
        SharedPreferences.Editor e=p.edit().putFloat(dimScheduleKey("s",id,dim),(float)fs.stability).putFloat(dimScheduleKey("d",id,dim),(float)fs.difficulty).putInt(dimScheduleKey("iv",id,dim),interval).putLong(dimScheduleKey("due",id,dim),today.plusDays(interval).toEpochDay()).putLong(dimScheduleKey("last",id,dim),today.toEpochDay()).putFloat(forgettingFactorKey(dim),(float)factor).putInt(forgettingObservationKey(dim),forgettingObservationCount(dim)+1);e.apply();
    }

    private void syncAggregateSchedule(int id){
        long earliest=Long.MAX_VALUE;int minIv=Integer.MAX_VALUE;double weakS=Double.MAX_VALUE,hardD=0;boolean any=false;
        for(int dim=0;dim<4;dim++){if(dimensionLevel(id,dim)<=0)continue;long due=dimensionDueEpochDay(id,dim);if(due!=Long.MIN_VALUE){earliest=Math.min(earliest,due);any=true;}minIv=Math.min(minIv,dimensionIntervalDays(id,dim));double st=dimensionMemoryStability(id,dim);if(st>0)weakS=Math.min(weakS,st);hardD=Math.max(hardD,dimensionMemoryDifficulty(id,dim));}
        SharedPreferences.Editor e=p.edit();if(any)e.putLong("due_"+id,earliest);if(minIv!=Integer.MAX_VALUE)e.putInt("iv_"+id,minIv);if(weakS!=Double.MAX_VALUE)e.putFloat("fs_s_"+id,(float)weakS);if(hardD>0)e.putFloat("fs_d_"+id,(float)hardD);e.apply();
    }

    public double desiredRetention(){return Math.max(0.80,Math.min(0.97,p.getFloat("fs_retention",0.90f)));}
    public void setDesiredRetention(double value){p.edit().putFloat("fs_retention",(float)Math.max(0.80,Math.min(0.97,value))).apply();}
    public double forgettingFactor(int dim){return PersonalForgettingModel.clampFactor(p.getFloat(forgettingFactorKey(clamp(dim,0,3)),1.0f));}
    public int forgettingObservationCount(int dim){return Math.max(0,p.getInt(forgettingObservationKey(clamp(dim,0,3)),0));}
    public String forgettingSpeedLabel(int dim){return PersonalForgettingModel.speedLabel(forgettingFactor(dim),forgettingObservationCount(dim));}
    public int dimensionIntervalDays(int id,int dim){String k=dimScheduleKey("iv",id,dim);return clamp(p.contains(k)?p.getInt(k,1):p.getInt("iv_"+id,mastery(id)>0?defaultInterval(mastery(id)):1),1,3650);}
    public long dimensionDueEpochDay(int id,int dim){String k=dimScheduleKey("due",id,dim);return p.contains(k)?p.getLong(k,Long.MIN_VALUE):p.getLong("due_"+id,Long.MIN_VALUE);}
    public LocalDate dimensionNextDueDate(int id,int dim){long d=dimensionDueEpochDay(id,dim);return d==Long.MIN_VALUE?null:LocalDate.ofEpochDay(d);}
    public LocalDate dimensionLastReviewed(int id,int dim){String k=dimScheduleKey("last",id,dim);long d=p.contains(k)?p.getLong(k,Long.MIN_VALUE):p.getLong("last_"+id,Long.MIN_VALUE);return d==Long.MIN_VALUE?null:LocalDate.ofEpochDay(d);}
    public double dimensionMemoryStability(int id,int dim){String k=dimScheduleKey("s",id,dim);double v=p.contains(k)?p.getFloat(k,0f):p.getFloat("fs_s_"+id,0f);return Double.isFinite(v)?Math.max(0.0,Math.min(3650.0,v)):0.0;}
    public double dimensionMemoryDifficulty(int id,int dim){String k=dimScheduleKey("d",id,dim);double v=p.contains(k)?p.getFloat(k,0f):p.getFloat("fs_d_"+id,0f);return Double.isFinite(v)?Math.max(0.0,Math.min(10.0,v)):0.0;}
    public int dimensionRetrievability(int id,int dim){LocalDate last=dimensionLastReviewed(id,dim);double st=dimensionMemoryStability(id,dim);if(last==null||st<=0)return dimensionLevel(id,dim)>0?80:0;int elapsed=(int)Math.max(0,java.time.temporal.ChronoUnit.DAYS.between(last,LocalDate.now()));return (int)Math.round(FsrsScheduler.retrievability(st,elapsed)*100.0);}
    public boolean dimensionDueForReview(int id,int dim,LocalDate date){if(dimensionLevel(id,dim)<=0)return false;long due=dimensionDueEpochDay(id,dim);return due==Long.MIN_VALUE||due<=date.toEpochDay();}
    public int priorityReviewDimension(int id,LocalDate date){
        int best=-1,bestRisk=Integer.MIN_VALUE;long today=date.toEpochDay();for(int dim=0;dim<4;dim++){if(dimensionLevel(id,dim)<=0)continue;long due=dimensionDueEpochDay(id,dim);if(due==Long.MIN_VALUE||due<=today){long late=due==Long.MIN_VALUE?1:Math.max(0,today-due);int risk=(int)Math.min(80,late*6)+(100-dimensionRetrievability(id,dim))+Math.max(0,4-dimensionLevel(id,dim))*8;if(risk>bestRisk){bestRisk=risk;best=dim;}}}
        return best>=0?best:weakestDimension(id);
    }
    public int dimensionDueCount(List<Word> words,int dim,LocalDate date){int n=0;for(Word w:words)if(dimensionDueForReview(w.id,dim,date))n++;return n;}
    public int dimensionAverageInterval(List<Word> words,int dim){long total=0;int n=0;for(Word w:words)if(dimensionLevel(w.id,dim)>0){total+=dimensionIntervalDays(w.id,dim);n++;}return n==0?0:(int)Math.round(total/(double)n);}
    public int dimensionAverageRetrievability(List<Word> words,int dim){long total=0;int n=0;for(Word w:words)if(dimensionLevel(w.id,dim)>0){total+=dimensionRetrievability(w.id,dim);n++;}return n==0?0:(int)Math.round(total/(double)n);}
    public double memoryStability(int id){double min=Double.MAX_VALUE;for(int dim=0;dim<4;dim++)if(dimensionLevel(id,dim)>0){double v=dimensionMemoryStability(id,dim);if(v>0)min=Math.min(min,v);}return min==Double.MAX_VALUE?0:min;}
    public double memoryDifficulty(int id){double max=0;for(int dim=0;dim<4;dim++)if(dimensionLevel(id,dim)>0)max=Math.max(max,dimensionMemoryDifficulty(id,dim));return max;}
    public int memoryRetrievability(int id){int min=101;for(int dim=0;dim<4;dim++)if(dimensionLevel(id,dim)>0)min=Math.min(min,dimensionRetrievability(id,dim));return min==101?(mastery(id)>0?80:0):min;}

    /** One-card study rating mainly trains meaning recall. */
    public void recordStudyRating(int id,int rating){
        rating=clamp(rating,0,3);int cur=meaningLevel(id),target;
        if(rating==0)target=Math.max(1,cur-1);else if(rating==1)target=Math.max(1,cur);else if(rating==2)target=Math.max(3,Math.min(5,cur+1));else target=Math.max(4,Math.min(5,cur+1));
        SharedPreferences.Editor base=p.edit().putInt(dimKey(id,DIM_MEANING),target);
        for(int dim=1;dim<4;dim++)if(!p.contains(dimKey(id,dim)))base.putInt(dimKey(id,dim),0);base.apply();recomputeOverall(id);if(mastery(id)==0)p.edit().putInt("m_"+id,1).apply();
        boolean correct=rating>=2;long ms=rating==3?2200:(rating==2?4500:10000);updateAdaptiveSchedule(id,new int[]{DIM_MEANING},correct,ms);if(!correct)p.edit().remove("grad_"+id).apply();recordDailyCard();LearningDatabase.log(context,"study_rating",String.valueOf(id),DIM_MEANING,correct,ms,"rating="+rating);mirrorWordState(id);
    }

    /** v3.1 three-choice smart vocabulary rating: forgot / fuzzy / remembered. */
    public void recordSmartWordRating(int id,int rating,long responseMs){recordSmartWordRating(id,rating,responseMs,SmartReviewModeEngine.MODE_IT_ZH);}
    /** Backwards compatibility for v3.1.8 callers. */
    public void recordSmartWordRating(int id,int rating,long responseMs,boolean activeOutputRecall){recordSmartWordRating(id,rating,responseMs,activeOutputRecall?SmartReviewModeEngine.MODE_ZH_IT:SmartReviewModeEngine.MODE_IT_ZH);}
    /**
     * v3.3 records the exact channel used by the smart-memory card.
     * The four dimensions are learner-facing as: recognition / active recall / listening / usage.
     */
    public void recordSmartWordRating(int id,int rating,long responseMs,int reviewMode){
        rating=clamp(rating,0,2);int dim=SmartReviewModeEngine.dimensionForMode(reviewMode);int cur=dimensionLevel(id,dim),target=SmartMemoryScheduler.nextMeaningLevel(rating,cur);
        SharedPreferences.Editor base=p.edit().putInt(dimKey(id,dim),target);for(int d=0;d<4;d++)if(!p.contains(dimKey(id,d)))base.putInt(dimKey(id,d),d==dim?target:0);base.apply();recomputeOverall(id);if(mastery(id)==0)p.edit().putInt("m_"+id,1).apply();
        long sample=responseMs>0?responseMs:(rating==2?3500:(rating==1?7000:10000));int quality=rating==0?1:(rating==1?2:(sample>0&&sample<3000?4:3));boolean correct=rating==2;
        int att=attempts(id)+1,cor=correctAnswers(id)+(correct?1:0);long oldAvg=avgResponseMs(id),avg=oldAvg<=0?sample:Math.round(oldAvg*0.75+sample*0.25);int oldStreak=correctStreak(id);
        updateDimensionSchedule(id,dim,correct,quality);syncAggregateSchedule(id);int nextInterval=dimensionIntervalDays(id,dim);LocalDate today=LocalDate.now();SharedPreferences.Editor e=p.edit().putInt("att_"+id,att).putInt("cor_"+id,cor).putLong("last_"+id,today.toEpochDay()).putLong("avgms_"+id,avg);
        if(rating==2)e.putInt("w_"+id,Math.max(0,wrongCount(id)-1)).putInt("ok_"+id,oldStreak+1);else if(rating==1){e.putInt("ok_"+id,0).remove("grad_"+id);}else{e.putInt("w_"+id,wrongCount(id)+1).putInt("ok_"+id,0).remove("grad_"+id);}e.apply();
        recordDailyCard();LearningDatabase.log(context,"smart_memory",String.valueOf(id),dim,correct,sample,"rating="+rating+",dimInterval="+nextInterval+",factor="+String.format(java.util.Locale.US,"%.2f",forgettingFactor(dim))+",mode="+SmartReviewModeEngine.modeLabel(reviewMode));mirrorWordState(id);
    }

    /** Percentages shown in v3.3 smart-memory cards. */
    public int smartRecognitionPct(int id){return meaningLevel(id)*20;}
    public int smartRecallPct(int id){return spellingLevel(id)*20;}
    public int smartListeningPct(int id){return listeningLevel(id)*20;}
    public int smartUsagePct(int id){return speakingLevel(id)*20;}
    public int smartOverallPct(int id){return (int)Math.round((smartRecognitionPct(id)+smartRecallPct(id)+smartListeningPct(id)+smartUsagePct(id))/4.0);}
    public boolean smartMastered(int id){return meaningLevel(id)>=4&&spellingLevel(id)>=3&&listeningLevel(id)>=3&&speakingLevel(id)>=3;}

    // ---------- Scheduling + metrics ----------
    public int intervalDays(int id){int min=Integer.MAX_VALUE;for(int dim=0;dim<4;dim++)if(dimensionLevel(id,dim)>0)min=Math.min(min,dimensionIntervalDays(id,dim));return min==Integer.MAX_VALUE?clamp(p.getInt("iv_"+id,mastery(id)>0?defaultInterval(mastery(id)):1),1,3650):min;}
    public long dueEpochDay(int id){long min=Long.MAX_VALUE;boolean any=false;for(int dim=0;dim<4;dim++)if(dimensionLevel(id,dim)>0){long due=dimensionDueEpochDay(id,dim);if(due!=Long.MIN_VALUE){min=Math.min(min,due);any=true;}}return any?min:p.getLong("due_"+id,Long.MIN_VALUE);}
    public LocalDate nextDueDate(int id){long d=dueEpochDay(id);return d==Long.MIN_VALUE?null:LocalDate.ofEpochDay(d);}
    public boolean dueForReview(int id,LocalDate date){if(mastery(id)<=0)return false;for(int dim=0;dim<4;dim++)if(dimensionDueForReview(id,dim,date))return true;return false;}
    public int wrongCount(int id){return Math.max(0,p.getInt("w_"+id,0));}
    public int correctStreak(int id){return Math.max(0,p.getInt("ok_"+id,0));}
    public int attempts(int id){return Math.max(0,p.getInt("att_"+id,0));}
    public int correctAnswers(int id){return Math.max(0,Math.min(attempts(id),p.getInt("cor_"+id,0)));}
    public long avgResponseMs(int id){return Math.max(0L,p.getLong("avgms_"+id,0L));}
    public LocalDate lastReviewed(int id){long d=p.getLong("last_"+id,Long.MIN_VALUE);return d==Long.MIN_VALUE?null:LocalDate.ofEpochDay(d);}
    public int wrongTotal(List<Word> words){int n=0;for(Word w:words)if(wrongCount(w.id)>0)n++;return n;}
    public int dueCount(List<Word> words,LocalDate date){int n=0;for(Word w:words)if(dueForReview(w.id,date))n++;return n;}

    /**
     * v2.7.8 dashboard snapshot. The Today screen used to scan all 2774 words
     * many times in succession (score, due, wrong, stubborn, route, weak dimension,
     * pressure, pace). This combines the hot metrics in one pass.
     */
    public static class DashboardStats {
        public int totalWords,due,wrong,stubborn,graduated,totalIntroduced,routeIntroduced,routeUnknown,learned;
        public int sevenDayAccuracy,sevenDayAttempts,newQuota,reviewPressure,reviewCap;
        public int tomorrowScheduled,nextThreeDaysScheduled,forecastPressure;
        public final int[] learnedDimensionPct=new int[4];
        public final int[] allDimensionPct=new int[4];
        public String paceAdvice="",reviewPressureAdvice="";
        public int weakestDimension(){int w=0;for(int i=1;i<4;i++)if(learnedDimensionPct[i]<learnedDimensionPct[w])w=i;return w;}
    }

    public DashboardStats dashboardStats(List<Word> words,LocalDate date){
        DashboardStats s=new DashboardStats();if(words==null||words.isEmpty())return s;s.totalWords=words.size();
        final long day=date.toEpochDay();final int routeLimit=Math.min(vocabularyRouteLimit(),words.size());long[] learnedSum=new long[4],allSum=new long[4];
        for(int i=0;i<words.size();i++){
            Word w=words.get(i);int id=w.id,m=mastery(id);int meaning=dimensionLevel(id,DIM_MEANING),listening=dimensionLevel(id,DIM_LISTENING),spelling=dimensionLevel(id,DIM_SPELLING),speaking=dimensionLevel(id,DIM_SPEAKING);int[] dims={meaning,listening,spelling,speaking};
            for(int d=0;d<4;d++)allSum[d]+=dims[d];
            if(m>0){s.totalIntroduced++;s.learned++;for(int d=0;d<4;d++)learnedSum[d]+=dims[d];long due=dueEpochDay(id);if(due==Long.MIN_VALUE||due<=day)s.due++;else{if(due==day+1)s.tomorrowScheduled++;if(due<=day+3)s.nextThreeDaysScheduled++;}}
            int wc=wrongCount(id);if(wc>0)s.wrong++;
            int att=attempts(id),cor=correctAnswers(id);int acc=att==0?100:(int)Math.round(cor*100.0/att);int weakest=Math.min(Math.min(meaning,listening),Math.min(spelling,speaking));if(wc>=3||(att>=6&&acc<65)||(att>=5&&m>0&&weakest<=1))s.stubborn++;if(isGraduated(id))s.graduated++;
            if(i<routeLimit){if(m>0)s.routeIntroduced++;else s.routeUnknown++;}
        }
        double allDen=words.size()*5.0;for(int d=0;d<4;d++){s.allDimensionPct[d]=(int)Math.round(allSum[d]*100.0/allDen);s.learnedDimensionPct[d]=s.learned==0?0:(int)Math.round(learnedSum[d]*100.0/(s.learned*5.0));}
        int a=0,c=0;LocalDate now=LocalDate.now();for(int i=0;i<7;i++){a+=dailyAttempts(now.minusDays(i));c+=dailyCorrect(now.minusDays(i));}s.sevenDayAttempts=a;s.sevenDayAccuracy=a==0?85:(int)Math.round(c*100.0/a);
        int smCap=sessionMinutes();int baseCap=ReviewForecastEngine.baseCapacity(smCap);
        if(s.routeUnknown==0)s.newQuota=0;else{
            int base=smCap<=10?5:(smCap<=20?10:(smCap<=30?15:(smCap<=45?20:25)));
            if((s.sevenDayAttempts>=12&&s.sevenDayAccuracy<65)||s.due>=90||s.stubborn>=45)s.newQuota=0;else{
                double factor=s.sevenDayAccuracy<70?0.45:(s.sevenDayAccuracy<80?0.70:(s.sevenDayAccuracy>=92?1.18:1.0));if(s.due>60)factor*=0.50;else if(s.due>35)factor*=0.70;else if(s.due>20)factor*=0.86;if(s.wrong>25)factor*=0.80;if(s.stubborn>20)factor*=0.82;
                int nearLoad=s.due+s.nextThreeDaysScheduled;if(s.tomorrowScheduled>=baseCap||nearLoad>=Math.round(baseCap*2.2))s.forecastPressure=2;else if(s.tomorrowScheduled>=Math.round(baseCap*0.70)||nearLoad>=Math.round(baseCap*1.45))s.forecastPressure=1;else s.forecastPressure=0;
                if(s.forecastPressure>=2)factor*=0.62;else if(s.forecastPressure==1)factor*=0.82;
                int q=Math.max(1,Math.min(smCap>=60?30:25,(int)Math.round(base*factor)));int tomorrowHeadroom=Math.max(0,baseCap-s.tomorrowScheduled);q=Math.min(q,tomorrowHeadroom);s.newQuota=Math.min(s.routeUnknown,q);
            }
        }
        if(s.newQuota>0&&weeklyAdjustmentActive()){int before=s.newQuota;s.newQuota=Math.min(s.routeUnknown,Math.max(1,(int)Math.round(s.newQuota*weeklyNewWordPercent()/100.0)));if(s.newQuota<before)s.paceAdvice="周测后自动减负 · 新词 "+s.newQuota;}
        if(s.due>=90||s.wrong>=45||s.stubborn>=30)s.reviewPressure=3;else if(s.due>=55||s.wrong>=28||s.stubborn>=18)s.reviewPressure=2;else if(s.due>=28||s.wrong>=15||s.stubborn>=8)s.reviewPressure=1;else s.reviewPressure=0;
        s.reviewCap=s.reviewPressure>=3?Math.min(baseCap,42):(s.reviewPressure==2?Math.min(baseCap,48):baseCap);
        if(s.newQuota==0&&s.tomorrowScheduled>=baseCap)s.paceAdvice="明日复习已接近上限 · 今天暂停新词";else if(s.newQuota==0)s.paceAdvice="暂停新词 · 先清理复习压力";else if(s.forecastPressure>=2)s.paceAdvice="未来3天复习偏多 · 新词自动降到 "+s.newQuota;else if(s.forecastPressure==1)s.paceAdvice="提前给明后天减负 · 新词 "+s.newQuota;else if(s.due>35)s.paceAdvice="复习优先 · 新词 "+s.newQuota;else if(s.stubborn>20)s.paceAdvice="顽固词偏多 · 新词 "+s.newQuota;else if(s.sevenDayAccuracy>=92)s.paceAdvice="状态稳定 · 新词 "+s.newQuota;else s.paceAdvice="平衡推进 · 新词 "+s.newQuota;
        if(weeklyAdjustmentActive()&&s.newQuota>0&&weeklyNewWordPercent()<100)s.paceAdvice="周测减负中 · 新词 "+s.newQuota+" · 优先"+WeeklyExamEngine.skillLabel(weeklyFocusPrimary());
        if(s.reviewPressure>=3)s.reviewPressureAdvice="复习压力很高 · 今天只取最高风险约"+s.reviewCap+"项，其余自动顺延";else if(s.reviewPressure==2)s.reviewPressureAdvice="复习偏多 · 已启用压力保护";else if(s.reviewPressure==1)s.reviewPressureAdvice="有少量复习积压 · 优先清理高风险项";else s.reviewPressureAdvice="复习压力正常";
        return s;
    }
    public boolean favorite(int id){return p.getBoolean("f_"+id,false);}
    public void setFavorite(int id,boolean v){p.edit().putBoolean("f_"+id,v).apply();mirrorWordState(id);}

    // ---------- Confusable-word tracking ----------
    private String confusionKey(int a,int b){int x=Math.min(a,b),y=Math.max(a,b);return "conf_"+x+"_"+y;}
    public void recordConfusion(int a,int b){if(a<=0||b<=0||a==b)return;String k=confusionKey(a,b);p.edit().putInt(k,p.getInt(k,0)+1).apply();}
    public int confusionScore(int a,int b){return p.getInt(confusionKey(a,b),0);}
    public List<int[]> learnedConfusions(){
        List<int[]> out=new ArrayList<>();for(Map.Entry<String,?> en:p.getAll().entrySet()){String k=en.getKey();if(!k.startsWith("conf_"))continue;try{String[] s=k.substring(5).split("_");int score=(Integer)en.getValue();out.add(new int[]{Integer.parseInt(s[0]),Integer.parseInt(s[1]),score});}catch(Exception ignored){}}
        out.sort((a,b)->Integer.compare(b[2],a[2]));return out;
    }

    // ---------- Placement test ----------
    public boolean placementCompleted(){return p.getBoolean("placement_done",false);}
    public int placementKnownEstimate(){return p.getInt("placement_known",0);}
    public void applyPlacement(List<Word> words,int knownCount,List<Integer> strongSampleIds){
        int n=Math.min(Math.max(0,knownCount),words.size());SharedPreferences.Editor e=p.edit();
        for(int i=0;i<n;i++){Word w=words.get(i);if(mastery(w.id)==0){e.putInt("m_"+w.id,2).putInt(dimKey(w.id,DIM_MEANING),3).putInt(dimKey(w.id,DIM_LISTENING),1).putInt(dimKey(w.id,DIM_SPELLING),1).putInt(dimKey(w.id,DIM_SPEAKING),1).putInt("iv_"+w.id,7).putLong("due_"+w.id,LocalDate.now().plusDays(7).toEpochDay());}}
        for(Integer id:strongSampleIds){e.putInt("m_"+id,3).putInt(dimKey(id,DIM_MEANING),4).putInt(dimKey(id,DIM_LISTENING),2).putInt(dimKey(id,DIM_SPELLING),2).putInt(dimKey(id,DIM_SPEAKING),1);}
        e.putBoolean("placement_done",true).putInt("placement_known",n).apply();for(int i=0;i<n;i++)mirrorWordState(words.get(i).id);for(Integer id:strongSampleIds)mirrorWordState(id);
    }
    public void clearPlacement(){p.edit().remove("placement_done").remove("placement_known").apply();}

    // ---------- Time budget + pronunciation ----------
    public int sessionMinutes(){int v=p.getInt("session_minutes",20);if(v==5)return 10;if(v==15)return 20;return (v==10||v==20||v==30||v==45||v==60)?v:20;}
    public void setSessionMinutes(int m){if(m!=10&&m!=20&&m!=30&&m!=45&&m!=60)m=20;p.edit().putInt("session_minutes",m).apply();}
    public int pronunciationMode(){return clamp(p.getInt("pron_mode",PRON_AUTO),PRON_ALWAYS,PRON_NEVER);}
    public void setPronunciationMode(int m){p.edit().putInt("pron_mode",clamp(m,PRON_ALWAYS,PRON_NEVER)).apply();}
    public boolean shouldShowPronunciation(int id,boolean expandedOrAnswer){int mode=pronunciationMode();if(mode==PRON_ALWAYS)return true;if(mode==PRON_NEVER)return false;if(mode==PRON_TAP)return expandedOrAnswer;return mastery(id)<2;}

    // ---------- v2.9 experience preferences ----------
    public int fontScaleMode(){return clamp(p.getInt("font_scale_mode",1),0,3);}
    public void setFontScaleMode(int mode){p.edit().putInt("font_scale_mode",clamp(mode,0,3)).apply();}
    public float fontScale(){switch(fontScaleMode()){case 0:return 0.90f;case 2:return 1.15f;case 3:return 1.30f;default:return 1.00f;}}
    public int audioSpeedMode(){return clamp(p.getInt("audio_speed_mode",1),0,2);}
    public void setAudioSpeedMode(int mode){p.edit().putInt("audio_speed_mode",clamp(mode,0,2)).apply();}
    public float audioSpeed(){switch(audioSpeedMode()){case 0:return 0.75f;case 2:return 1.15f;default:return 1.00f;}}
    public boolean onboardingCompleted(){return p.getBoolean("onboarding_done",false);}
    public void markOnboardingCompleted(){p.edit().putBoolean("onboarding_done",true).apply();}

    // ---------- v5.0 beginner-first home ----------
    public boolean homeSimpleMode(){return p.getBoolean("home_simple_mode",true);}
    public void setHomeSimpleMode(boolean simple){p.edit().putBoolean("home_simple_mode",simple).apply();}
    public int activeDaysBeforeToday(){LocalDate today=LocalDate.now();int n=0;for(int i=1;i<=365;i++)if(dailyActivity(today.minusDays(i))>0)n++;return n;}
    public boolean firstWeekGuidanceActive(){
        // Existing users should never be forced back into the seven-day beginner shell merely
        // because old daily-stat history was trimmed from a backup. Course progress or a prior
        // weekly exam is sufficient evidence that the learner already knows the app.
        if(courseUnlockedUnitIndex()>=3||courseXp()>=120||weeklyExamCount()>0)return false;
        return activeDaysBeforeToday()<7;
    }
    public int firstWeekLearningDay(){return firstWeekGuidanceActive()?Math.min(7,activeDaysBeforeToday()+1):8;}

    public boolean hasLearningHistory(){
        if(placementCompleted())return true;
        for(Map.Entry<String,?> en:p.getAll().entrySet()){String k=en.getKey();Object v=en.getValue();if(k.startsWith("m_")&&v instanceof Integer&&((Integer)v)>0)return true;if((k.startsWith("day_cards_")||k.startsWith("day_att_"))&&v instanceof Integer&&((Integer)v)>0)return true;}
        return false;
    }

    // ---------- v3.0 guided course path ----------
    private String courseLessonKey(String unitId,int lesson){return "course_done_"+unitId+"_"+lesson;}
    public boolean courseInitialized(){return p.getBoolean("course_v3_initialized",false);}
    public void initializeCourseProgress(int unlockedUnit){p.edit().putBoolean("course_v3_initialized",true).putInt("course_unlocked_unit",Math.max(0,Math.min(97,unlockedUnit))).apply();}
    public int courseUnlockedUnitIndex(){return Math.max(0,Math.min(97,p.getInt("course_unlocked_unit",0)));}
    public void advanceCourseUnlockedUnit(int target){int safe=Math.max(0,Math.min(97,target)),current=courseUnlockedUnitIndex();if(!courseInitialized()||safe>current)p.edit().putBoolean("course_v3_initialized",true).putInt("course_unlocked_unit",Math.max(current,safe)).apply();}
    public boolean courseLessonDone(String unitId,int lesson){return p.getBoolean(courseLessonKey(unitId,lesson),false);}
    public int courseCompletedLessons(String unitId){int n=0;for(int i=0;i<8;i++)if(courseLessonDone(unitId,i))n++;return n;}
    public void markCourseLessonComplete(String unitId,int unitIndex,int lesson,int lessonCount,int xp){
        if(unitId==null||unitId.isEmpty())return;SharedPreferences.Editor e=p.edit().putBoolean(courseLessonKey(unitId,lesson),true);
        boolean all=true;for(int i=0;i<lessonCount;i++)if(i!=lesson&&!courseLessonDone(unitId,i)){all=false;break;}
        if(all&&unitIndex>=courseUnlockedUnitIndex()&&unitIndex<97)e.putInt("course_unlocked_unit",unitIndex+1);
        int add=Math.max(0,xp);e.putInt("course_xp",courseXp()+add).putInt(dayKey("course_xp_",LocalDate.now()),courseTodayXp()+add).apply();
    }
    public int courseXp(){return Math.max(0,p.getInt("course_xp",0));}
    public int courseTodayXp(){return Math.max(0,p.getInt(dayKey("course_xp_",LocalDate.now()),0));}
    public void saveCourseResume(String unitId,int lesson,int question,int correct,int wrong){p.edit().putString("course_resume_unit",unitId==null?"":unitId).putInt("course_resume_lesson",lesson).putInt("course_resume_q",Math.max(0,question)).putInt("course_resume_correct",Math.max(0,correct)).putInt("course_resume_wrong",Math.max(0,wrong)).putLong("course_resume_at",System.currentTimeMillis()).apply();}
    public boolean hasCourseResume(String unitId,int lesson){return unitId!=null&&unitId.equals(p.getString("course_resume_unit",""))&&lesson==p.getInt("course_resume_lesson",-1)&&System.currentTimeMillis()-p.getLong("course_resume_at",0L)<36L*60L*60L*1000L;}
    public int courseResumeQuestion(){return Math.max(0,p.getInt("course_resume_q",0));}
    public int courseResumeCorrect(){return Math.max(0,p.getInt("course_resume_correct",0));}
    public int courseResumeWrong(){return Math.max(0,p.getInt("course_resume_wrong",0));}
    public void clearCourseResume(){p.edit().remove("course_resume_unit").remove("course_resume_lesson").remove("course_resume_q").remove("course_resume_correct").remove("course_resume_wrong").remove("course_resume_at").apply();}

    // Compact cross-session exposure memory. Due/stubborn reviews may still repeat, but
    // ordinary weak/new items are deferred so the same word is not shown again immediately.
    public void markWordExposure(int id,String mode){if(id<=0)return;String today=LocalDate.now().toString();String savedDay=p.getString("recent_exposure_day","");String raw=today.equals(savedDay)?p.getString("recent_exposure_ids",""):"";List<String> ids=new ArrayList<>();if(raw!=null&&!raw.isEmpty())for(String x:raw.split(","))if(!x.isEmpty()&&!x.equals(String.valueOf(id)))ids.add(x);ids.add(String.valueOf(id));while(ids.size()>40)ids.remove(0);p.edit().putString("recent_exposure_day",today).putString("recent_exposure_ids",String.join(",",ids)).apply();}
    public Set<Integer> recentWordIds(){Set<Integer> out=new HashSet<>();if(!LocalDate.now().toString().equals(p.getString("recent_exposure_day","")))return out;String raw=p.getString("recent_exposure_ids","");if(raw==null||raw.isEmpty())return out;for(String x:raw.split(","))try{out.add(Integer.parseInt(x));}catch(Exception ignored){}return out;}

    public static class StudyResumeState{public final List<Integer> wordIds=new ArrayList<>();public int index,again,hard,good,easy;public long savedAt;}
    private String sessionPrefKey(String key){return "study_resume_"+(key==null?"default":key.replaceAll("[^A-Za-z0-9_-]","_"));}
    public void saveStudySession(String key,List<Word> words,int index,int again,int hard,int good,int easy){try{JSONObject o=new JSONObject();JSONArray a=new JSONArray();for(Word w:words)a.put(w.id);o.put("ids",a);o.put("index",Math.max(0,index));o.put("again",Math.max(0,again));o.put("hard",Math.max(0,hard));o.put("good",Math.max(0,good));o.put("easy",Math.max(0,easy));o.put("savedAt",System.currentTimeMillis());p.edit().putString(sessionPrefKey(key),o.toString()).putString("last_study_resume_key",key==null?"":key).apply();}catch(Exception ignored){}}
    public StudyResumeState loadStudySession(String key){String raw=p.getString(sessionPrefKey(key),"");if(raw==null||raw.isEmpty())return null;try{JSONObject o=new JSONObject(raw);long saved=o.optLong("savedAt",0L);if(saved<=0||System.currentTimeMillis()-saved>36L*60L*60L*1000L){clearStudySession(key);return null;}StudyResumeState s=new StudyResumeState();JSONArray a=o.optJSONArray("ids");if(a==null||a.length()==0)return null;for(int i=0;i<a.length();i++)s.wordIds.add(a.getInt(i));s.index=Math.max(0,o.optInt("index",0));s.again=Math.max(0,o.optInt("again",0));s.hard=Math.max(0,o.optInt("hard",0));s.good=Math.max(0,o.optInt("good",0));s.easy=Math.max(0,o.optInt("easy",0));s.savedAt=saved;return s;}catch(Exception e){clearStudySession(key);return null;}}
    public String latestStudySessionKey(){return p.getString("last_study_resume_key","");}
    public void clearStudySession(String key){SharedPreferences.Editor e=p.edit().remove(sessionPrefKey(key));if((key==null?"":key).equals(latestStudySessionKey()))e.remove("last_study_resume_key");e.apply();}

    /** Repairs impossible legacy preference values without deleting valid study history. */
    public int repairCorruptState(List<Word> words){int fixes=0;SharedPreferences.Editor e=p.edit();for(Word w:words){int id=w.id;if(p.contains("m_"+id)){int raw=p.getInt("m_"+id,0),v=clamp(raw,0,5);if(raw!=v){e.putInt("m_"+id,v);fixes++;}}for(int dim=0;dim<4;dim++){String k=dimKey(id,dim);if(p.contains(k)){int raw=p.getInt(k,0),v=clamp(raw,0,5);if(raw!=v){e.putInt(k,v);fixes++;}}String div=dimScheduleKey("iv",id,dim);if(p.contains(div)){int raw=p.getInt(div,1),v=clamp(raw,1,3650);if(raw!=v){e.putInt(div,v);fixes++;}}String ds=dimScheduleKey("s",id,dim);if(p.contains(ds)){float raw=p.getFloat(ds,0f),v=Float.isFinite(raw)?Math.max(0f,Math.min(3650f,raw)):0f;if(raw!=v){e.putFloat(ds,v);fixes++;}}String dd=dimScheduleKey("d",id,dim);if(p.contains(dd)){float raw=p.getFloat(dd,5.5f),v=Float.isFinite(raw)?Math.max(1f,Math.min(10f,raw)):5.5f;if(raw!=v){e.putFloat(dd,v);fixes++;}}}String wk="w_"+id;if(p.contains(wk)&&p.getInt(wk,0)<0){e.putInt(wk,0);fixes++;}String ak="att_"+id;if(p.contains(ak)&&p.getInt(ak,0)<0){e.putInt(ak,0);fixes++;}String ck="cor_"+id;if(p.contains(ck)){int a=Math.max(0,p.getInt(ak,0)),raw=p.getInt(ck,0),v=Math.max(0,Math.min(a,raw));if(raw!=v){e.putInt(ck,v);fixes++;}}String ik="iv_"+id;if(p.contains(ik)){int raw=p.getInt(ik,1),v=clamp(raw,1,3650);if(raw!=v){e.putInt(ik,v);fixes++;}}String sk="fs_s_"+id;if(p.contains(sk)){float raw=p.getFloat(sk,0f),v=Float.isFinite(raw)?Math.max(0f,Math.min(3650f,raw)):0f;if(raw!=v){e.putFloat(sk,v);fixes++;}}String dk="fs_d_"+id;if(p.contains(dk)){float raw=p.getFloat(dk,5.5f),v=Float.isFinite(raw)?Math.max(1f,Math.min(10f,raw)):5.5f;if(raw!=v){e.putFloat(dk,v);fixes++;}}}
        for(int dim=0;dim<4;dim++){String fk=forgettingFactorKey(dim);if(p.contains(fk)){float raw=p.getFloat(fk,1f),v=(float)PersonalForgettingModel.clampFactor(Float.isFinite(raw)?raw:1f);if(raw!=v){e.putFloat(fk,v);fixes++;}}String ok=forgettingObservationKey(dim);if(p.contains(ok)&&p.getInt(ok,0)<0){e.putInt(ok,0);fixes++;}}int rawPer=p.getInt("perDay",20),safePer=Math.max(1,Math.min(200,rawPer));if(rawPer!=safePer){e.putInt("perDay",safePer);fixes++;}e.apply();return fixes;}

    // ---------- Daily statistics ----------
    private String dayKey(String prefix,LocalDate d){return prefix+d.toString();}
    private void recordDailyCard(){LocalDate d=LocalDate.now();p.edit().putInt(dayKey("day_cards_",d),dailyCards(d)+1).apply();mirrorDailyState(d);}
    private void recordDailyPractice(boolean correct,long responseMs){LocalDate d=LocalDate.now();SharedPreferences.Editor e=p.edit().putInt(dayKey("day_att_",d),dailyAttempts(d)+1);if(correct)e.putInt(dayKey("day_cor_",d),dailyCorrect(d)+1);if(responseMs>0)e.putLong(dayKey("day_ms_",d),dailyResponseMs(d)+responseMs);e.apply();mirrorDailyState(d);}
    public int dailyCards(LocalDate d){return p.getInt(dayKey("day_cards_",d),0);}
    public int dailyAttempts(LocalDate d){return p.getInt(dayKey("day_att_",d),0);}
    public int dailyCorrect(LocalDate d){return p.getInt(dayKey("day_cor_",d),0);}
    public long dailyResponseMs(LocalDate d){return p.getLong(dayKey("day_ms_",d),0L);}
    public long dailyActiveSeconds(LocalDate d){return p.getLong(dayKey("day_active_sec_",d),0L);}
    public void addActiveSeconds(long seconds){if(seconds<=0)return;LocalDate d=LocalDate.now();p.edit().putLong(dayKey("day_active_sec_",d),dailyActiveSeconds(d)+seconds).apply();mirrorDailyState(d);}
    public int weekActualMinutes(LocalDate anyDay){LocalDate monday=anyDay.minusDays(anyDay.getDayOfWeek().getValue()-1L);long sec=0;for(int i=0;i<7;i++)sec+=dailyActiveSeconds(monday.plusDays(i));return (int)Math.round(sec/60.0);}
    public int dailyActivity(LocalDate d){return dailyCards(d)+dailyAttempts(d);}
    public int dailyAccuracy(LocalDate d){int a=dailyAttempts(d);return a==0?0:(int)Math.round(dailyCorrect(d)*100.0/a);}
    public long dailyAvgResponseMs(LocalDate d){int a=dailyAttempts(d);return a==0?0:dailyResponseMs(d)/a;}
    public int sevenDayAccuracy(){int a=0,c=0;LocalDate d=LocalDate.now();for(int i=0;i<7;i++){a+=dailyAttempts(d.minusDays(i));c+=dailyCorrect(d.minusDays(i));}return a==0?85:(int)Math.round(c*100.0/a);}
    public int sevenDayAttempts(){int a=0;LocalDate d=LocalDate.now();for(int i=0;i<7;i++)a+=dailyAttempts(d.minusDays(i));return a;}
    public int recommendedNewWords(List<Word> words){return dashboardStats(words,LocalDate.now()).newQuota;}
    /** A word graduates from normal daily pressure only after broad, durable mastery. */
    public boolean isGraduated(int id){
        boolean saved=p.getBoolean("grad_"+id,false);
        if(saved){if(wrongCount(id)>0||mastery(id)<4){p.edit().remove("grad_"+id).apply();return false;}return true;}
        boolean ready=mastery(id)>=5&&meaningLevel(id)>=4&&listeningLevel(id)>=4&&spellingLevel(id)>=4&&speakingLevel(id)>=3&&intervalDays(id)>=30&&memoryRetrievability(id)>=95&&wrongCount(id)==0&&correctStreak(id)>=3;
        if(ready)p.edit().putBoolean("grad_"+id,true).apply();return ready;
    }
    /** Stubborn words are repeatedly wrong or remain weak after enough encounters. */
    public boolean isStubborn(int id){int a=attempts(id);int acc=a==0?100:(int)Math.round(correctAnswers(id)*100.0/a);int weakest=Math.min(Math.min(meaningLevel(id),listeningLevel(id)),Math.min(spellingLevel(id),speakingLevel(id)));return wrongCount(id)>=3||(a>=6&&acc<65)||(a>=5&&mastery(id)>0&&weakest<=1);}
    public int graduatedCount(List<Word> words){int n=0;for(Word w:words)if(isGraduated(w.id))n++;return n;}
    public int stubbornCount(List<Word> words){int n=0;for(Word w:words)if(isStubborn(w.id))n++;return n;}
    public String paceAdvice(List<Word> words){int q=recommendedNewWords(words),acc=sevenDayAccuracy(),due=dueCount(words,LocalDate.now()),stub=stubbornCount(words);if(q==0)return "暂停新词 · 先清理复习压力";if(due>35)return "复习优先 · 新词 "+q; if(stub>20)return "顽固词偏多 · 新词 "+q; if(acc>=92)return "状态稳定 · 新词 "+q; return "平衡推进 · 新词 "+q;}
    public int activityStreak(){LocalDate d=LocalDate.now();int n=0;if(dailyActivity(d)==0)d=d.minusDays(1);for(int i=0;i<365;i++){if(dailyActivity(d)>0){n++;d=d.minusDays(1);}else break;}return n;}
    public int weekActiveDays(){LocalDate today=LocalDate.now(),monday=today.minusDays(today.getDayOfWeek().getValue()-1L);int n=0;for(LocalDate d=monday;!d.isAfter(today);d=d.plusDays(1))if(dailyActivity(d)>0)n++;return n;}
    public int weekAttempts(){LocalDate today=LocalDate.now(),monday=today.minusDays(today.getDayOfWeek().getValue()-1L);int n=0;for(LocalDate d=monday;!d.isAfter(today);d=d.plusDays(1))n+=dailyAttempts(d);return n;}
    public int weekCards(){LocalDate today=LocalDate.now(),monday=today.minusDays(today.getDayOfWeek().getValue()-1L);int n=0;for(LocalDate d=monday;!d.isAfter(today);d=d.plusDays(1))n+=dailyCards(d);return n;}
    public int pathCelebratedCount(LocalDate d){return p.getInt(dayKey("path_done_",d),-1);}
    public void setPathCelebratedCount(LocalDate d,int count){p.edit().putInt(dayKey("path_done_",d),Math.max(0,count)).apply();}
    public int totalAttempts(List<Word> words){int n=0;for(Word w:words)n+=attempts(w.id);return n;}
    public int totalCorrect(List<Word> words){int n=0;for(Word w:words)n+=correctAnswers(w.id);return n;}
    public int totalAccuracy(List<Word> words){int a=totalAttempts(words);return a==0?0:(int)Math.round(totalCorrect(words)*100.0/a);}

    // ---------- Sentence-pattern / dialogue practice ----------
    private String auxKey(String type,String suffix){return "aux_"+type+"_"+suffix;}
    public void recordAuxiliaryResult(String type,boolean correct,long responseMs){
        int att=auxiliaryAttempts(type)+1,cor=auxiliaryCorrect(type)+(correct?1:0);LocalDate today=LocalDate.now();
        SharedPreferences.Editor e=p.edit().putInt(auxKey(type,"att"),att).putInt(auxKey(type,"cor"),cor)
                .putInt(dayKey("auxday_"+type+"_att_",today),dailyAuxiliaryAttempts(type,today)+1);
        if(correct)e.putInt(dayKey("auxday_"+type+"_cor_",today),dailyAuxiliaryCorrect(type,today)+1);e.apply();
        recordDailyPractice(correct,responseMs);LearningDatabase.log(context,"aux",type,-1,correct,responseMs,"");LearningDatabase.mirrorSkill(context,type,correct,responseMs);
    }
    public void recordAuxiliarySubskill(String type,boolean correct,long responseMs){
        int att=auxiliaryAttempts(type)+1,cor=auxiliaryCorrect(type)+(correct?1:0);p.edit().putInt(auxKey(type,"att"),att).putInt(auxKey(type,"cor"),cor).apply();LearningDatabase.mirrorSkill(context,type,correct,responseMs);
    }
    public int auxiliaryAttempts(String type){return p.getInt(auxKey(type,"att"),0);}
    public int auxiliaryCorrect(String type){return p.getInt(auxKey(type,"cor"),0);}
    public int auxiliaryAccuracy(String type){int a=auxiliaryAttempts(type);return a==0?0:(int)Math.round(auxiliaryCorrect(type)*100.0/a);}
    public int dailyAuxiliaryAttempts(String type,LocalDate d){return p.getInt(dayKey("auxday_"+type+"_att_",d),0);}
    public int dailyAuxiliaryCorrect(String type,LocalDate d){return p.getInt(dayKey("auxday_"+type+"_cor_",d),0);}
    /** v4.0.0 streak: a day counts only after all five active-speaking prompts are completed. */
    public int dailySpeakingStreak(){LocalDate d=LocalDate.now();if(dailyAuxiliaryAttempts("daily_speaking",d)<DailySpeakingChallengeEngine.DAILY_TARGET)d=d.minusDays(1);int n=0;for(int i=0;i<365;i++){if(dailyAuxiliaryAttempts("daily_speaking",d)>=DailySpeakingChallengeEngine.DAILY_TARGET){n++;d=d.minusDays(1);}else break;}return n;}
    public void markAuxiliaryCompletion(String type){LocalDate today=LocalDate.now();p.edit().putInt(auxKey(type,"att"),auxiliaryAttempts(type)+1).putInt(auxKey(type,"cor"),auxiliaryCorrect(type)+1).putInt(dayKey("auxday_"+type+"_att_",today),dailyAuxiliaryAttempts(type,today)+1).putInt(dayKey("auxday_"+type+"_cor_",today),dailyAuxiliaryCorrect(type,today)+1).apply();}

    // ---------- v4.3 three-day breakthrough cycle ----------
    /**
     * A breakthrough focus is intentionally locked for three completed study days.
     * Small score fluctuations must not make the prescription jump between skills.
     */
    public static class BreakthroughState {
        public String skillKey="",targetLevel="A1";
        public int phase=1,baselineScore=0,completedCycles=0;
        public long startDay=Long.MIN_VALUE,lastCompletedDay=Long.MIN_VALUE;
        public boolean waitingToday=false,cycleComplete=false;
    }
    private static final String BT_SKILL="bt_skill",BT_LEVEL="bt_level",BT_PHASE="bt_phase",BT_BASE="bt_base",BT_START="bt_start",BT_LAST="bt_last",BT_CYCLES="bt_cycles";
    private void beginBreakthroughCycle(String skillKey,String targetLevel,int baselineScore,LocalDate today,int completedCycles){
        p.edit().putString(BT_SKILL,skillKey==null?"":skillKey).putString(BT_LEVEL,targetLevel==null?"A1":targetLevel)
                .putInt(BT_PHASE,1).putInt(BT_BASE,clamp(baselineScore,0,100)).putLong(BT_START,today.toEpochDay())
                .remove(BT_LAST).putInt(BT_CYCLES,Math.max(0,completedCycles)).apply();
    }
    public BreakthroughState syncBreakthroughCycle(String suggestedSkill,String targetLevel,int baselineScore,LocalDate today){
        if(today==null)today=LocalDate.now();String level=targetLevel==null?"A1":targetLevel;String skill=suggestedSkill==null?"":suggestedSkill;
        String savedSkill=p.getString(BT_SKILL,"");String savedLevel=p.getString(BT_LEVEL,"");int phase=p.getInt(BT_PHASE,0);int cycles=Math.max(0,p.getInt(BT_CYCLES,0));long last=p.getLong(BT_LAST,Long.MIN_VALUE);
        boolean levelChanged=!savedLevel.equals(level);boolean missing=savedSkill.isEmpty()||phase<1;boolean finishedBeforeToday=phase>3&&last!=today.toEpochDay();
        if(missing||levelChanged||finishedBeforeToday)beginBreakthroughCycle(skill,level,baselineScore,today,cycles);
        return breakthroughState(today);
    }
    public BreakthroughState breakthroughState(LocalDate today){
        if(today==null)today=LocalDate.now();BreakthroughState s=new BreakthroughState();s.skillKey=p.getString(BT_SKILL,"");s.targetLevel=p.getString(BT_LEVEL,"A1");s.phase=Math.max(1,p.getInt(BT_PHASE,1));s.baselineScore=clamp(p.getInt(BT_BASE,0),0,100);s.completedCycles=Math.max(0,p.getInt(BT_CYCLES,0));s.startDay=p.getLong(BT_START,Long.MIN_VALUE);s.lastCompletedDay=p.getLong(BT_LAST,Long.MIN_VALUE);s.waitingToday=s.lastCompletedDay==today.toEpochDay();s.cycleComplete=s.phase>3;return s;
    }
    public void completeBreakthroughPhase(LocalDate today){
        if(today==null)today=LocalDate.now();BreakthroughState s=breakthroughState(today);if(s.lastCompletedDay==today.toEpochDay()||s.phase>3)return;int next=s.phase+1;SharedPreferences.Editor e=p.edit().putInt(BT_PHASE,next).putLong(BT_LAST,today.toEpochDay());if(s.phase==3)e.putInt(BT_CYCLES,s.completedCycles+1);e.apply();
    }

    // ---------- v3.5.1 real-life scenario conversation progress ----------
    private String dialogueScenarioKey(String id,String suffix){return "dialogue_scenario_"+(id==null?"":id)+"_"+suffix;}
    private String dialogueDifficultyName(int level){return level>=3?"advanced":level==2?"intermediate":"beginner";}
    private String dialogueDifficultyKey(String id,int level,String suffix){return dialogueScenarioKey(id,dialogueDifficultyName(level)+"_"+suffix);}
    public int dialogueScenarioCompleted(String id){return Math.max(0,p.getInt(dialogueScenarioKey(id,"done"),0));}
    public int dialogueScenarioBestScore(String id){return Math.max(0,Math.min(100,p.getInt(dialogueScenarioKey(id,"best"),0)));}
    public int dialogueScenarioCompleted(String id,int level){String k=dialogueDifficultyKey(id,level,"done");if(level==1&&!p.contains(k))return dialogueScenarioCompleted(id);return Math.max(0,p.getInt(k,0));}
    public int dialogueScenarioBestScore(String id,int level){String k=dialogueDifficultyKey(id,level,"best");if(level==1&&!p.contains(k))return dialogueScenarioBestScore(id);return Math.max(0,Math.min(100,p.getInt(k,0)));}
    public int dialogueScenarioRecommendedLevel(String id){
        if(dialogueScenarioBestScore(id,2)>=75)return 3;
        if(dialogueScenarioBestScore(id,1)>=80)return 2;
        return 1;
    }
    public void markDialogueScenarioCompletion(String id,int score){markDialogueScenarioCompletion(id,score,1);}
    public void markDialogueScenarioCompletion(String id,int score,int level){
        if(id==null||id.trim().isEmpty())return;int safe=Math.max(0,Math.min(100,score));int lv=Math.max(1,Math.min(3,level));
        p.edit()
            .putInt(dialogueScenarioKey(id,"done"),dialogueScenarioCompleted(id)+1)
            .putInt(dialogueScenarioKey(id,"best"),Math.max(dialogueScenarioBestScore(id),safe))
            .putInt(dialogueDifficultyKey(id,lv,"done"),dialogueScenarioCompleted(id,lv)+1)
            .putInt(dialogueDifficultyKey(id,lv,"best"),Math.max(dialogueScenarioBestScore(id,lv),safe))
            .apply();
        markAuxiliaryCompletion("dialogue_scenario");
    }

    // ---------- Grammar weakness diagnosis ----------
    private String grammarKey(String id,String suffix){return "gram_"+id+"_"+suffix;}
    public void recordGrammarResult(String id,boolean correct,long responseMs){
        if(id==null||id.trim().isEmpty())return;int att=grammarAttempts(id)+1,cor=grammarCorrect(id)+(correct?1:0);
        p.edit().putInt(grammarKey(id,"att"),att).putInt(grammarKey(id,"cor"),cor).putLong(grammarKey(id,"last"),LocalDate.now().toEpochDay()).apply();
        LearningDatabase.log(context,"grammar",id,-1,correct,responseMs,"");GrammarFsrs.record(context,id,correct);updateGrammarScheduleMirror(id,correct);if(!correct)recordErrorCause(ErrorCause.GRAMMAR,0,"grammar",id,"","");else{String top=topErrorCause();if(ErrorCause.GRAMMAR.equals(top)||ErrorCause.WORD_FORM.equals(top)||ErrorCause.ARTICLE_GENDER.equals(top))markAuxiliaryCompletion("error_repair");}
    }
    public void recordPatternResult(String patternId,boolean correct,long responseMs){recordAuxiliaryResult("pattern",correct,responseMs);recordGrammarResult(patternId,correct,responseMs);}
    public int grammarAttempts(String id){return p.getInt(grammarKey(id,"att"),0);}
    public int grammarCorrect(String id){return p.getInt(grammarKey(id,"cor"),0);}
    public int grammarMistakes(String id){return Math.max(0,grammarAttempts(id)-grammarCorrect(id));}
    public int grammarAccuracy(String id){int a=grammarAttempts(id);return a==0?0:(int)Math.round(grammarCorrect(id)*100.0/a);}
    private void updateGrammarScheduleMirror(String id,boolean correct){int iv=p.getInt(grammarKey(id,"iv"),0);double st=Double.longBitsToDouble(p.getLong(grammarKey(id,"st"),Double.doubleToRawLongBits(1.0)));double diff=Double.longBitsToDouble(p.getLong(grammarKey(id,"diff"),Double.doubleToRawLongBits(5.0)));if(correct){diff=Math.max(1.0,diff-.2);st=Math.max(1.0,st*(iv<=1?1.8:1.5));iv=Math.max(1,Math.min(120,(int)Math.round(st)));}else{diff=Math.min(10.0,diff+.75);st=Math.max(1.0,st*.42);iv=1;}p.edit().putInt(grammarKey(id,"iv"),iv).putLong(grammarKey(id,"due"),LocalDate.now().plusDays(iv).toEpochDay()).putLong(grammarKey(id,"st"),Double.doubleToRawLongBits(st)).putLong(grammarKey(id,"diff"),Double.doubleToRawLongBits(diff)).apply();}
    public int grammarIntervalDays(String id){return p.getInt(grammarKey(id,"iv"),0);}
    public long grammarDueEpochDay(String id){return p.getLong(grammarKey(id,"due"),Long.MIN_VALUE);}
    public boolean grammarDue(String id){long d=grammarDueEpochDay(id);return d!=Long.MIN_VALUE&&d<=LocalDate.now().toEpochDay();}
    public int dueGrammarCount(){int n=0;for(GrammarPoint g:GrammarDiagnostics.all())if(grammarDue(g.id))n++;return n;}
    public double grammarStability(String id){return Double.longBitsToDouble(p.getLong(grammarKey(id,"st"),Double.doubleToRawLongBits(1.0)));}
    public double grammarDifficulty(String id){return Double.longBitsToDouble(p.getLong(grammarKey(id,"diff"),Double.doubleToRawLongBits(5.0)));}
    public GrammarProgressEntity snapshotGrammarState(String id){GrammarProgressEntity x=new GrammarProgressEntity();x.grammarId=id;x.attempts=grammarAttempts(id);x.correct=grammarCorrect(id);x.lastEpochDay=p.getLong(grammarKey(id,"last"),Long.MIN_VALUE);x.dueEpochDay=grammarDueEpochDay(id);x.intervalDays=grammarIntervalDays(id);x.stability=grammarStability(id);x.difficulty=grammarDifficulty(id);x.updatedAt=System.currentTimeMillis();return x;}

    // ---------- CEFR checkpoint exams ----------
    public void saveExamScore(String level,int score){if(level==null)return;String k="exam_"+level.toUpperCase(java.util.Locale.ROOT);int best=Math.max(score,p.getInt(k+"_best",0));p.edit().putInt(k+"_last",score).putInt(k+"_best",best).putLong(k+"_day",LocalDate.now().toEpochDay()).apply();LearningDatabase.log(context,"exam",level,-1,score>=70,0,"score="+score);}
    public int lastExamScore(String level){return p.getInt("exam_"+level.toUpperCase(java.util.Locale.ROOT)+"_last",0);}
    public int bestExamScore(String level){return p.getInt("exam_"+level.toUpperCase(java.util.Locale.ROOT)+"_best",0);}


    // ---------- v4.5 seven-active-day practical exam ----------
    private static final String WEEK_EXAM_DAY="week_exam_last_day",WEEK_EXAM_SCORE="week_exam_score",WEEK_EXAM_COUNT="week_exam_count",WEEK_FOCUS_1="week_focus_1",WEEK_FOCUS_2="week_focus_2",WEEK_FOCUS_UNTIL="week_focus_until",WEEK_BASELINE_DAY="week_baseline_day";
    private String weekSkillKey(String prefix,String skill){return prefix+(skill==null?MasteryPassportEngine.ACTION_MEANING:skill);}
    public int weeklyExamCount(){return Math.max(0,p.getInt(WEEK_EXAM_COUNT,0));}
    public long lastWeeklyExamEpochDay(){return p.getLong(WEEK_EXAM_DAY,Long.MIN_VALUE);}
    public int lastWeeklyExamScore(){return clamp(p.getInt(WEEK_EXAM_SCORE,0),0,100);}
    public boolean weeklyExamCompletedToday(){return lastWeeklyExamEpochDay()==LocalDate.now().toEpochDay();}
    public int weeklyExamActiveDaysSinceLast(){
        LocalDate today=LocalDate.now();long last=lastWeeklyExamEpochDay();LocalDate start=last==Long.MIN_VALUE?startDate():LocalDate.ofEpochDay(last).plusDays(1);if(start.isAfter(today))return 0;int n=0,guard=0;for(LocalDate d=start;!d.isAfter(today)&&guard++<3660;d=d.plusDays(1))if(dailyActivity(d)>0)n++;return n;
    }
    public boolean weeklyExamDue(){return !weeklyExamCompletedToday()&&weeklyExamActiveDaysSinceLast()>=7;}
    public int weeklyExamSkillScore(String skill){return clamp(p.getInt(weekSkillKey("week_exam_skill_",skill),0),0,100);}
    public int weeklyExamBeforeScore(String skill){return clamp(p.getInt(weekSkillKey("week_exam_before_",skill),0),0,100);}
    public int weeklyExamAfterScore(String skill){return clamp(p.getInt(weekSkillKey("week_exam_after_",skill),0),0,100);}
    public int weeklyCycleBaselineScore(String skill,int fallback){return clamp(p.getInt(weekSkillKey("week_baseline_",skill),fallback),0,100);}
    public String weeklyFocusPrimary(){return p.getString(WEEK_FOCUS_1,MasteryPassportEngine.ACTION_MEANING);}
    public String weeklyFocusSecondary(){return p.getString(WEEK_FOCUS_2,MasteryPassportEngine.ACTION_LISTENING);}
    public boolean weeklyAdjustmentActive(){long until=p.getLong(WEEK_FOCUS_UNTIL,Long.MIN_VALUE);return weeklyExamCount()>0&&until!=Long.MIN_VALUE&&LocalDate.now().toEpochDay()<=until;}
    public int weeklyNewWordPercent(){if(!weeklyAdjustmentActive())return 100;int score=lastWeeklyExamScore();return score<55?55:(score<65?65:(score<75?80:100));}
    public String weeklyAdjustmentSummary(){if(!weeklyAdjustmentActive())return "当前没有周测减负；每日计划按复习压力和三日处方正常调度。";int pct=weeklyNewWordPercent();String reduce=pct>=100?"新词量保持正常":"新词上限临时调整为常规的约"+pct+"%";return "接下来7天："+reduce+"；优先补 "+WeeklyExamEngine.skillLabel(weeklyFocusPrimary())+" + "+WeeklyExamEngine.skillLabel(weeklyFocusSecondary())+"。";}
    public int weeklyExamDelta(String skill){return weeklyExamAfterScore(skill)-weeklyExamBeforeScore(skill);}
    public void saveWeeklyExamResult(int overall,int[] skillScores,int[] baselineScores,int[] afterScores){
        String[] keys=WeeklyExamEngine.SKILL_KEYS;SharedPreferences.Editor e=p.edit();LocalDate today=LocalDate.now();e.putLong(WEEK_EXAM_DAY,today.toEpochDay()).putInt(WEEK_EXAM_SCORE,clamp(overall,0,100)).putInt(WEEK_EXAM_COUNT,weeklyExamCount()+1).putLong(WEEK_FOCUS_UNTIL,today.plusDays(7).toEpochDay());
        int first=-1,second=-1,firstScore=101,secondScore=101;for(int i=0;i<keys.length;i++){int sc=skillScores!=null&&i<skillScores.length?clamp(skillScores[i],0,100):0;int before=baselineScores!=null&&i<baselineScores.length?clamp(baselineScores[i],0,100):0;int after=afterScores!=null&&i<afterScores.length?clamp(afterScores[i],0,100):before;e.putInt(weekSkillKey("week_exam_skill_",keys[i]),sc).putInt(weekSkillKey("week_exam_before_",keys[i]),before).putInt(weekSkillKey("week_exam_after_",keys[i]),after).putInt(weekSkillKey("week_baseline_",keys[i]),after);if(sc<firstScore){second=first;secondScore=firstScore;first=i;firstScore=sc;}else if(sc<secondScore){second=i;secondScore=sc;}}
        if(first<0)first=0;if(second<0||first==second)second=(first+1)%keys.length;e.putString(WEEK_FOCUS_1,keys[first]).putString(WEEK_FOCUS_2,keys[second]).putLong(WEEK_BASELINE_DAY,today.toEpochDay()).apply();LearningDatabase.log(context,"weekly_exam","six_skill",-1,overall>=70,0,"score="+overall+",focus="+keys[first]+"+"+keys[second]);
    }

    // ---------- Graded reading ----------
    private String readingKey(String id){return "read_"+id+"_best";}
    public int readingBest(String id){return p.getInt(readingKey(id),0);}
    public void saveReadingScore(String id,int score){if(id==null||id.isEmpty())return;int best=Math.max(score,readingBest(id));p.edit().putInt(readingKey(id),best).apply();LearningDatabase.log(context,"reading",id,-1,score>=70,0,"score="+score);}


    // ---------- v2.2 Room mirror + error-cause diagnosis ----------
    public WordProgressEntity snapshotWordState(int id){
        WordProgressEntity x=new WordProgressEntity();x.wordId=id;x.mastery=mastery(id);x.meaning=meaningLevel(id);x.listening=listeningLevel(id);x.spelling=spellingLevel(id);x.speaking=speakingLevel(id);x.wrongCount=wrongCount(id);x.correctStreak=correctStreak(id);x.attempts=attempts(id);x.correctAnswers=correctAnswers(id);x.avgResponseMs=avgResponseMs(id);LocalDate last=lastReviewed(id);x.lastEpochDay=last==null?Long.MIN_VALUE:last.toEpochDay();x.dueEpochDay=dueEpochDay(id);x.intervalDays=intervalDays(id);x.stability=memoryStability(id);x.difficulty=memoryDifficulty(id);x.favorite=favorite(id);x.updatedAt=System.currentTimeMillis();return x;
    }
    public DailyStatEntity snapshotDailyState(LocalDate d){DailyStatEntity x=new DailyStatEntity();x.date=d.toString();x.cards=dailyCards(d);x.attempts=dailyAttempts(d);x.correct=dailyCorrect(d);x.responseMs=dailyResponseMs(d);x.activeSeconds=dailyActiveSeconds(d);x.updatedAt=System.currentTimeMillis();return x;}
    private void mirrorWordState(int id){LearningDatabase.mirrorWord(context,snapshotWordState(id));}
    private void mirrorDailyState(LocalDate d){LearningDatabase.mirrorDaily(context,snapshotDailyState(d));}
    private String errorKey(String cause){return "errcause_"+(cause==null?ErrorCause.RECALL:cause);}
    public void recordErrorCause(String cause,int wordId,String mode,String expected,String actual,String detail){String c=cause==null?ErrorCause.RECALL:cause;SharedPreferences.Editor e=p.edit().putInt(errorKey(c),errorCauseCount(c)+1);if(expected!=null&&!expected.trim().isEmpty()&&!"grammar".equals(mode)){String fp=(mode==null?"":mode)+"\u001f"+expected.trim()+"\u001f"+(actual==null?"":actual.trim());long now=System.currentTimeMillis(),last=p.getLong("pending_error_last_ms",0L);String prev=p.getString("pending_error_last_fp","");if(!fp.equals(prev)||now-last>5000L)e.putInt("pending_error_repairs",pendingErrorRepairs()+1);e.putString("pending_error_last_fp",fp).putLong("pending_error_last_ms",now);}e.apply();LearningDatabase.logError(context,wordId,mode,c,expected,actual,detail);}
    public int errorCauseCount(String cause){return p.getInt(errorKey(cause),0);}
    public String topErrorCause(){String[] keys={ErrorCause.MEANING_CONFUSION,ErrorCause.LISTENING_CONFUSION,ErrorCause.SPELLING,ErrorCause.ACCENT,ErrorCause.WORD_FORM,ErrorCause.ARTICLE_GENDER,ErrorCause.WORD_ORDER,ErrorCause.OMISSION,ErrorCause.PRONUNCIATION,ErrorCause.GRAMMAR,ErrorCause.RECALL};String best=ErrorCause.RECALL;int n=-1;for(String k:keys){int v=errorCauseCount(k);if(v>n){n=v;best=k;}}return best;}
    public int errorCauseTotal(){int n=0;for(String k:new String[]{ErrorCause.MEANING_CONFUSION,ErrorCause.LISTENING_CONFUSION,ErrorCause.SPELLING,ErrorCause.ACCENT,ErrorCause.WORD_FORM,ErrorCause.ARTICLE_GENDER,ErrorCause.WORD_ORDER,ErrorCause.OMISSION,ErrorCause.PRONUNCIATION,ErrorCause.GRAMMAR,ErrorCause.RECALL})n+=errorCauseCount(k);return n;}
    public int pendingErrorRepairs(){return Math.max(0,p.getInt("pending_error_repairs",0));}
    public void setPendingErrorRepairs(int count){p.edit().putInt("pending_error_repairs",Math.max(0,count)).apply();}
    public void markEvidenceRepairComplete(){setPendingErrorRepairs(Math.max(0,pendingErrorRepairs()-1));}

    // ---------- General settings ----------
    public LocalDate startDate(){try{return LocalDate.parse(p.getString("start",DEFAULT_START));}catch(Exception e){return LocalDate.parse(DEFAULT_START);}}
    public void setStartDate(LocalDate d){p.edit().putString("start",d.toString()).apply();}
    public int perDay(){return p.getInt("perDay",20);}
    public void setPerDay(int n){p.edit().putInt("perDay",n).apply();}
    public boolean preferOriginalAudio(){return p.getBoolean("originalAudio",true);}
    public void setPreferOriginalAudio(boolean b){p.edit().putBoolean("originalAudio",b).apply();}
    public boolean reminderEnabled(){return p.getBoolean("reminderEnabled",false);}
    public void setReminderEnabled(boolean b){p.edit().putBoolean("reminderEnabled",b).apply();}
    public int reminderHour(){return p.getInt("reminderHour",19);}
    public int reminderMinute(){return p.getInt("reminderMinute",0);}
    public void setReminderTime(int h,int m){p.edit().putInt("reminderHour",h).putInt("reminderMinute",m).apply();}


    // ---------- v2.5 learning route, goal and pressure protection ----------
    public int vocabularyRouteLimit(){int v=p.getInt("vocab_route",ROUTE_2774);return v==ROUTE_1000||v==ROUTE_1600?v:ROUTE_2774;}
    public void setVocabularyRouteLimit(int limit){if(limit!=ROUTE_1000&&limit!=ROUTE_1600&&limit!=ROUTE_2774)limit=ROUTE_2774;p.edit().putInt("vocab_route",limit).apply();}
    public String vocabularyRouteLabel(){int v=vocabularyRouteLimit();return v==ROUTE_1000?"核心1000":(v==ROUTE_1600?"B1核心1600":"完整2774");}
    public int learningGoal(){return clamp(p.getInt("learning_goal",GOAL_BALANCED),GOAL_LIFE,GOAL_BALANCED);}
    public void setLearningGoal(int goal){p.edit().putInt("learning_goal",clamp(goal,GOAL_LIFE,GOAL_BALANCED)).apply();}
    public String learningGoalLabel(){switch(learningGoal()){case GOAL_LIFE:return "意大利生活";case GOAL_WORK:return "工作交流";case GOAL_A2:return "A2能力";case GOAL_B1:return "B1能力";default:return "综合学习";}}
    public int reviewPressureLevel(List<Word> words){int due=dueCount(words,LocalDate.now()),wrong=wrongTotal(words),stub=stubbornCount(words);if(due>=90||wrong>=45||stub>=30)return 3;if(due>=55||wrong>=28||stub>=18)return 2;if(due>=28||wrong>=15||stub>=8)return 1;return 0;}
    public int protectedReviewCap(List<Word> words){int m=sessionMinutes(),pressure=reviewPressureLevel(words);int base=m<=10?18:(m<=20?30:(m<=30?48:(m<=45?60:72)));if(pressure>=3)return Math.min(base,42);if(pressure==2)return Math.min(base,48);return base;}
    public String reviewPressureAdvice(List<Word> words){int due=dueCount(words,LocalDate.now()),level=reviewPressureLevel(words),cap=protectedReviewCap(words);if(level>=3)return "复习压力很高 · 今天只取最高风险约"+cap+"项，其余自动顺延";if(level==2)return "复习偏多 · 已启用压力保护";if(level==1)return "有少量复习积压 · 优先清理高风险项";return "复习压力正常";}
    public int routeIntroducedCount(List<Word> words){int limit=Math.min(vocabularyRouteLimit(),words.size()),n=0;for(int i=0;i<limit;i++)if(mastery(words.get(i).id)>0)n++;return n;}
    public int weekEstimatedMinutes(LocalDate anyDay){LocalDate monday=anyDay.minusDays(anyDay.getDayOfWeek().getValue()-1L);double minutes=0;for(int i=0;i<7;i++){LocalDate d=monday.plusDays(i);minutes+=dailyCards(d)*0.35+dailyAttempts(d)*0.45;}return (int)Math.round(minutes);}
    public int previousWeekAttempts(){LocalDate d=LocalDate.now().minusWeeks(1),m=d.minusDays(d.getDayOfWeek().getValue()-1L);int n=0;for(int i=0;i<7;i++)n+=dailyAttempts(m.plusDays(i));return n;}
    public int previousWeekCorrect(){LocalDate d=LocalDate.now().minusWeeks(1),m=d.minusDays(d.getDayOfWeek().getValue()-1L);int n=0;for(int i=0;i<7;i++)n+=dailyCorrect(m.plusDays(i));return n;}
    public int previousWeekCards(){LocalDate d=LocalDate.now().minusWeeks(1),m=d.minusDays(d.getDayOfWeek().getValue()-1L);int n=0;for(int i=0;i<7;i++)n+=dailyCards(m.plusDays(i));return n;}

    public int countAtLeast(List<Word> words,int level){int n=0;for(Word w:words)if(mastery(w.id)>=level)n++;return n;}
    public int b1Count(List<Word> words){int n=0;for(Word w:words)if(b1Ready(w.id))n++;return n;}
    public int strongCount(List<Word> words){return countAtLeast(words,5);}
    public int introducedCount(List<Word> words){return countAtLeast(words,1);}

    // ---------- v3.3.5 ten-article / 2000-word route ----------
    private String memoryArticleKey(String sectionId){return "memory_article_done_"+(sectionId==null?"":sectionId);}
    public boolean memoryArticleSectionDone(String sectionId){return sectionId!=null&&!sectionId.isEmpty()&&p.getBoolean(memoryArticleKey(sectionId),false);}
    public void markMemoryArticleSectionDone(String sectionId){
        if(sectionId==null||sectionId.isEmpty()||memoryArticleSectionDone(sectionId))return;
        p.edit().putBoolean(memoryArticleKey(sectionId),true).apply();
        recordAuxiliaryResult("memory_article",true,0L);
    }
    public int memoryArticleCompletedSections(MemoryArticle article){int n=0;if(article!=null)for(MemoryArticleSection s:article.sections)if(memoryArticleSectionDone(s.id))n++;return n;}
    public int memoryArticleCompletedTotal(MemoryArticleRepository repo){int n=0;if(repo!=null)for(MemoryArticle a:repo.all())n+=memoryArticleCompletedSections(a);return n;}
    private String memoryArticleExposureKey(int wordId){return "memory_article_exposure_"+wordId;}
    public int memoryArticleExposureCount(int wordId){return Math.max(0,p.getInt(memoryArticleExposureKey(wordId),0));}
    public boolean memoryArticleEncountered(int wordId){return memoryArticleExposureCount(wordId)>0;}
    public void recordMemoryArticleExposure(int wordId){
        if(wordId<=0)return;String key=memoryArticleExposureKey(wordId);int count=Math.min(999,p.getInt(key,0)+1);
        p.edit().putInt(key,count).apply();markWordExposure(wordId,"memory_article");
    }
    public int memoryArticleEncounteredCount(java.util.List<Integer> ids){int n=0;if(ids!=null)for(Integer id:ids)if(id!=null&&memoryArticleEncountered(id))n++;return n;}
    public int memoryArticleRecognizedCount(java.util.List<Integer> ids){int n=0;if(ids!=null)for(Integer id:ids)if(id!=null&&meaningLevel(id)>=2)n++;return n;}
    public int memoryArticleMasteredCount(java.util.List<Integer> ids){int n=0;if(ids!=null)for(Integer id:ids)if(id!=null&&smartMastered(id))n++;return n;}
    /** Backfills one encounter for sections completed before v3.3.7 introduced exposure counters. */
    public void migrateMemoryArticleExposureIfNeeded(MemoryArticleRepository repo){
        if(repo==null||p.getBoolean("memory_article_exposure_migrated_v337",false))return;SharedPreferences.Editor e=p.edit();
        for(MemoryArticle a:repo.all())for(MemoryArticleSection s:a.sections)if(memoryArticleSectionDone(s.id))for(Integer id:s.targetWordIds)if(id!=null&&memoryArticleExposureCount(id)==0)e.putInt(memoryArticleExposureKey(id),1);
        e.putBoolean("memory_article_exposure_migrated_v337",true).apply();
    }

    private String memoryArticleSentenceKey(String sectionId){return "memory_article_sentence_done_"+(sectionId==null?"":sectionId);}
    public boolean memoryArticleSentenceStudyDone(String sectionId){return sectionId!=null&&!sectionId.isEmpty()&&p.getBoolean(memoryArticleSentenceKey(sectionId),false);}
    public void markMemoryArticleSentenceStudyDone(String sectionId){
        if(sectionId==null||sectionId.isEmpty()||memoryArticleSentenceStudyDone(sectionId))return;
        p.edit().putBoolean(memoryArticleSentenceKey(sectionId),true).apply();recordAuxiliaryResult("memory_article_sentence",true,0L);
    }

    // ---------- Backup ----------
    public JSONObject exportJson(List<Word> words) throws Exception {
        JSONObject o=new JSONObject();o.put("version",31);o.put("pendingErrorRepairs",pendingErrorRepairs());o.put("startDate",startDate().toString());o.put("perDay",perDay());o.put("preferOriginalAudio",preferOriginalAudio());
        o.put("homeSimpleMode",homeSimpleMode());o.put("vocabularyRoute",vocabularyRouteLimit());o.put("learningGoal",learningGoal());o.put("reminderEnabled",reminderEnabled());o.put("reminderHour",reminderHour());o.put("reminderMinute",reminderMinute());o.put("sessionMinutes",sessionMinutes());o.put("pronMode",pronunciationMode());o.put("audioSpeedMode",audioSpeedMode());o.put("fontScaleMode",fontScaleMode());o.put("onboardingDone",onboardingCompleted());o.put("placementDone",placementCompleted());o.put("placementKnown",placementKnownEstimate());o.put("desiredRetention",desiredRetention());o.put("courseInitialized",courseInitialized());o.put("courseUnlockedUnit",courseUnlockedUnitIndex());o.put("courseXp",courseXp());
        JSONArray m=new JSONArray(),f=new JSONArray(),wj=new JSONArray(),due=new JSONArray(),metrics=new JSONArray(),dims=new JSONArray(),conf=new JSONArray(),fsrs=new JSONArray(),graduated=new JSONArray(),dimensionSchedules=new JSONArray();
        for(Word word:words){int v=mastery(word.id);if(v>0){JSONObject x=new JSONObject();x.put("id",word.id);x.put("level",v);m.put(x);}if(favorite(word.id))f.put(word.id);if(p.getBoolean("grad_"+word.id,false))graduated.put(word.id);int wc=wrongCount(word.id);if(wc>0){JSONObject x=new JSONObject();x.put("id",word.id);x.put("count",wc);wj.put(x);}long d=dueEpochDay(word.id);if(d!=Long.MIN_VALUE){JSONObject x=new JSONObject();x.put("id",word.id);x.put("day",d);due.put(x);}if(v>0||p.contains(dimKey(word.id,DIM_MEANING))){JSONObject x=new JSONObject();x.put("id",word.id);x.put("meaning",meaningLevel(word.id));x.put("listening",listeningLevel(word.id));x.put("spelling",spellingLevel(word.id));x.put("speaking",speakingLevel(word.id));dims.put(x);}if(attempts(word.id)>0||p.contains("iv_"+word.id)){JSONObject x=new JSONObject();x.put("id",word.id);x.put("interval",intervalDays(word.id));x.put("attempts",attempts(word.id));x.put("correct",correctAnswers(word.id));x.put("avgMs",avgResponseMs(word.id));x.put("last",p.getLong("last_"+word.id,Long.MIN_VALUE));metrics.put(x);}if(memoryStability(word.id)>0){JSONObject x=new JSONObject();x.put("id",word.id);x.put("stability",memoryStability(word.id));x.put("difficulty",memoryDifficulty(word.id));fsrs.put(x);}for(int dim=0;dim<4;dim++)if(dimensionLevel(word.id,dim)>0&&hasDimensionSchedule(word.id,dim)){JSONObject x=new JSONObject();x.put("id",word.id);x.put("dim",dim);x.put("interval",dimensionIntervalDays(word.id,dim));x.put("due",dimensionDueEpochDay(word.id,dim));LocalDate last=dimensionLastReviewed(word.id,dim);x.put("last",last==null?Long.MIN_VALUE:last.toEpochDay());x.put("stability",dimensionMemoryStability(word.id,dim));x.put("difficulty",dimensionMemoryDifficulty(word.id,dim));dimensionSchedules.put(x);}}
        JSONObject forgettingProfile=new JSONObject();for(int dim=0;dim<4;dim++){JSONObject x=new JSONObject();x.put("factor",forgettingFactor(dim));x.put("observations",forgettingObservationCount(dim));forgettingProfile.put(String.valueOf(dim),x);}o.put("dimensionSchedules",dimensionSchedules);o.put("forgettingProfile",forgettingProfile);
        for(int[] c:learnedConfusions()){JSONObject x=new JSONObject();x.put("a",c[0]);x.put("b",c[1]);x.put("score",c[2]);conf.put(x);}
        JSONArray daily=new JSONArray();LocalDate today=LocalDate.now();for(int i=0;i<180;i++){LocalDate d=today.minusDays(i);if(dailyActivity(d)>0){JSONObject x=new JSONObject();x.put("date",d.toString());x.put("cards",dailyCards(d));x.put("attempts",dailyAttempts(d));x.put("correct",dailyCorrect(d));x.put("ms",dailyResponseMs(d));x.put("activeSec",dailyActiveSeconds(d));daily.put(x);}}
        JSONArray dailySpeaking=new JSONArray();for(int i=0;i<365;i++){LocalDate d=today.minusDays(i);int sa=dailyAuxiliaryAttempts("daily_speaking",d);if(sa>0){JSONObject x=new JSONObject();x.put("date",d.toString());x.put("attempts",sa);x.put("correct",dailyAuxiliaryCorrect("daily_speaking",d));dailySpeaking.put(x);}}o.put("dailySpeaking",dailySpeaking);
        JSONObject aux=new JSONObject();for(String type:new String[]{"pattern","dialogue","dialogue_speaking","dialogue_scenario","life_task","freechat","shadowing","pronunciation","mission","level_exam","reading","sentence_dictation","error_repair","verb_center","cloze","word_family","intensive_listening","stubborn","phrases","preposition","past_tense","pronouns","family_train","rescue","writing","sentence_fsrs","error_evidence_repair","weekly_exam","active_recall","listening_course","listen_speak","daily_speaking","focus_mode","core_sentences","memory_article","memory_article_sentence","weak_story","pron_double","pron_r","pron_gli","pron_gn","pron_hard_soft"}){JSONObject x=new JSONObject();x.put("attempts",auxiliaryAttempts(type));x.put("correct",auxiliaryCorrect(type));aux.put(type,x);}
        JSONArray grammar=new JSONArray();for(GrammarPoint gp:GrammarDiagnostics.all()){if(grammarAttempts(gp.id)>0){JSONObject x=new JSONObject();x.put("id",gp.id);x.put("attempts",grammarAttempts(gp.id));x.put("correct",grammarCorrect(gp.id));x.put("interval",grammarIntervalDays(gp.id));x.put("due",grammarDueEpochDay(gp.id));x.put("stability",grammarStability(gp.id));x.put("difficulty",grammarDifficulty(gp.id));grammar.put(x);}}
        JSONObject exams=new JSONObject();for(String level:new String[]{"A1","A2","B1"}){JSONObject x=new JSONObject();x.put("last",lastExamScore(level));x.put("best",bestExamScore(level));exams.put(level,x);}JSONObject readingScores=new JSONObject();for(Map.Entry<String,?> en:p.getAll().entrySet()){String k=en.getKey();if(k.startsWith("read_")&&k.endsWith("_best")){String id=k.substring(5,k.length()-5);Object val=en.getValue();if(val instanceof Integer)readingScores.put(id,(Integer)val);}}JSONObject errorCauses=new JSONObject();for(String k:new String[]{ErrorCause.MEANING_CONFUSION,ErrorCause.LISTENING_CONFUSION,ErrorCause.SPELLING,ErrorCause.ACCENT,ErrorCause.WORD_FORM,ErrorCause.ARTICLE_GENDER,ErrorCause.WORD_ORDER,ErrorCause.OMISSION,ErrorCause.PRONUNCIATION,ErrorCause.GRAMMAR,ErrorCause.RECALL})errorCauses.put(k,errorCauseCount(k));o.put("errorCauseCounts",errorCauses);o.put("graduated",graduated);o.put("readingScores",readingScores);o.put("examStats",exams);o.put("auxStats",aux);o.put("grammarStats",grammar);o.put("fsrs",fsrs);o.put("mastery",m);o.put("dimensions",dims);o.put("favorites",f);o.put("wrong",wj);o.put("due",due);o.put("metrics",metrics);o.put("confusions",conf);o.put("daily",daily);JSONObject bt=new JSONObject();BreakthroughState bs=breakthroughState(LocalDate.now());bt.put("skill",bs.skillKey);bt.put("level",bs.targetLevel);bt.put("phase",bs.phase);bt.put("baseline",bs.baselineScore);bt.put("startDay",bs.startDay);bt.put("lastCompletedDay",bs.lastCompletedDay);bt.put("cycles",bs.completedCycles);o.put("breakthroughCycle",bt);JSONObject course=new JSONObject();for(Map.Entry<String,?> en:p.getAll().entrySet()){if(en.getKey().startsWith("course_done_")&&en.getValue() instanceof Boolean&&((Boolean)en.getValue()))course.put(en.getKey(),true);}o.put("courseDone",course);JSONObject memoryDone=new JSONObject();for(Map.Entry<String,?> en:p.getAll().entrySet()){String k=en.getKey();if(k.startsWith("memory_article_done_")&&en.getValue() instanceof Boolean&&((Boolean)en.getValue()))memoryDone.put(k,true);}o.put("memoryArticleDone",memoryDone);JSONObject memoryExposure=new JSONObject();for(Map.Entry<String,?> en:p.getAll().entrySet()){String k=en.getKey();if(k.startsWith("memory_article_exposure_")&&en.getValue() instanceof Integer&&((Integer)en.getValue())>0)memoryExposure.put(k,(Integer)en.getValue());}o.put("memoryArticleExposure",memoryExposure);JSONObject memorySentenceDone=new JSONObject();for(Map.Entry<String,?> en:p.getAll().entrySet()){String k=en.getKey();if(k.startsWith("memory_article_sentence_done_")&&en.getValue() instanceof Boolean&&((Boolean)en.getValue()))memorySentenceDone.put(k,true);}o.put("memoryArticleSentenceDone",memorySentenceDone);JSONObject dialogueScenarioProgress=new JSONObject();for(Map.Entry<String,?> en:p.getAll().entrySet()){String k=en.getKey();if(k.startsWith("dialogue_scenario_")&&(k.endsWith("_done")||k.endsWith("_best"))&&(en.getValue() instanceof Integer))dialogueScenarioProgress.put(k,(Integer)en.getValue());}o.put("dialogueScenarioProgress",dialogueScenarioProgress);JSONObject weekly=new JSONObject();weekly.put("lastDay",lastWeeklyExamEpochDay());weekly.put("score",lastWeeklyExamScore());weekly.put("count",weeklyExamCount());weekly.put("focus1",weeklyFocusPrimary());weekly.put("focus2",weeklyFocusSecondary());weekly.put("focusUntil",p.getLong(WEEK_FOCUS_UNTIL,Long.MIN_VALUE));weekly.put("baselineDay",p.getLong(WEEK_BASELINE_DAY,Long.MIN_VALUE));for(String key:WeeklyExamEngine.SKILL_KEYS){weekly.put("skill_"+key,weeklyExamSkillScore(key));weekly.put("before_"+key,weeklyExamBeforeScore(key));weekly.put("after_"+key,weeklyExamAfterScore(key));weekly.put("baseline_"+key,weeklyCycleBaselineScore(key,0));}o.put("weeklyExam",weekly);return o;
    }

    public void importJson(JSONObject o) throws Exception {
        SharedPreferences.Editor e=p.edit().clear();e.putString("start",o.optString("startDate",DEFAULT_START)).putInt("perDay",o.optInt("perDay",20)).putBoolean("originalAudio",o.optBoolean("preferOriginalAudio",true));
        e.putBoolean("home_simple_mode",o.optBoolean("homeSimpleMode",true)).putInt("vocab_route",o.optInt("vocabularyRoute",ROUTE_2774)).putInt("learning_goal",o.optInt("learningGoal",GOAL_BALANCED)).putBoolean("course_v3_initialized",o.optBoolean("courseInitialized",false)).putInt("course_unlocked_unit",o.optInt("courseUnlockedUnit",0)).putInt("course_xp",o.optInt("courseXp",0)).putBoolean("reminderEnabled",o.optBoolean("reminderEnabled",false)).putInt("reminderHour",o.optInt("reminderHour",19)).putInt("reminderMinute",o.optInt("reminderMinute",0)).putInt("session_minutes",o.optInt("sessionMinutes",30)).putInt("pron_mode",o.optInt("pronMode",PRON_AUTO)).putInt("audio_speed_mode",o.optInt("audioSpeedMode",1)).putInt("font_scale_mode",o.optInt("fontScaleMode",1)).putBoolean("onboarding_done",o.optBoolean("onboardingDone",true)).putBoolean("placement_done",o.optBoolean("placementDone",false)).putInt("placement_known",o.optInt("placementKnown",0)).putFloat("fs_retention",(float)o.optDouble("desiredRetention",0.90)).putInt("pending_error_repairs",Math.max(0,o.optInt("pendingErrorRepairs",0)));
        JSONArray m=o.optJSONArray("mastery");if(m!=null)for(int i=0;i<m.length();i++){JSONObject x=m.getJSONObject(i);e.putInt("m_"+x.getInt("id"),x.getInt("level"));}
        JSONArray dims=o.optJSONArray("dimensions");if(dims!=null)for(int i=0;i<dims.length();i++){JSONObject x=dims.getJSONObject(i);int id=x.getInt("id");e.putInt(dimKey(id,DIM_MEANING),x.optInt("meaning",0));e.putInt(dimKey(id,DIM_LISTENING),x.optInt("listening",0));e.putInt(dimKey(id,DIM_SPELLING),x.optInt("spelling",0));e.putInt(dimKey(id,DIM_SPEAKING),x.optInt("speaking",0));}
        JSONArray f=o.optJSONArray("favorites");if(f!=null)for(int i=0;i<f.length();i++)e.putBoolean("f_"+f.getInt(i),true);JSONArray graduated=o.optJSONArray("graduated");if(graduated!=null)for(int i=0;i<graduated.length();i++)e.putBoolean("grad_"+graduated.getInt(i),true);
        JSONArray wj=o.optJSONArray("wrong");if(wj!=null)for(int i=0;i<wj.length();i++){JSONObject x=wj.getJSONObject(i);e.putInt("w_"+x.getInt("id"),x.getInt("count"));}
        JSONArray due=o.optJSONArray("due");if(due!=null)for(int i=0;i<due.length();i++){JSONObject x=due.getJSONObject(i);e.putLong("due_"+x.getInt("id"),x.getLong("day"));}
        JSONArray metrics=o.optJSONArray("metrics");if(metrics!=null)for(int i=0;i<metrics.length();i++){JSONObject x=metrics.getJSONObject(i);int id=x.getInt("id");e.putInt("iv_"+id,x.optInt("interval",1));e.putInt("att_"+id,x.optInt("attempts",0));e.putInt("cor_"+id,x.optInt("correct",0));e.putLong("avgms_"+id,x.optLong("avgMs",0));long last=x.optLong("last",Long.MIN_VALUE);if(last!=Long.MIN_VALUE)e.putLong("last_"+id,last);}
        JSONArray fsrs=o.optJSONArray("fsrs");if(fsrs!=null)for(int i=0;i<fsrs.length();i++){JSONObject x=fsrs.getJSONObject(i);int id=x.getInt("id");e.putFloat("fs_s_"+id,(float)x.optDouble("stability",0));e.putFloat("fs_d_"+id,(float)x.optDouble("difficulty",5.5));}
        JSONArray dimensionSchedules=o.optJSONArray("dimensionSchedules");if(dimensionSchedules!=null)for(int i=0;i<dimensionSchedules.length();i++){JSONObject x=dimensionSchedules.getJSONObject(i);int id=x.getInt("id"),dim=clamp(x.optInt("dim",0),0,3);e.putInt(dimScheduleKey("iv",id,dim),clamp(x.optInt("interval",1),1,3650));long dd=x.optLong("due",Long.MIN_VALUE);if(dd!=Long.MIN_VALUE)e.putLong(dimScheduleKey("due",id,dim),dd);long dl=x.optLong("last",Long.MIN_VALUE);if(dl!=Long.MIN_VALUE)e.putLong(dimScheduleKey("last",id,dim),dl);e.putFloat(dimScheduleKey("s",id,dim),(float)x.optDouble("stability",0));e.putFloat(dimScheduleKey("d",id,dim),(float)x.optDouble("difficulty",5.5));}
        JSONObject forgettingProfile=o.optJSONObject("forgettingProfile");if(forgettingProfile!=null)for(int dim=0;dim<4;dim++){JSONObject x=forgettingProfile.optJSONObject(String.valueOf(dim));if(x!=null){e.putFloat(forgettingFactorKey(dim),(float)PersonalForgettingModel.clampFactor(x.optDouble("factor",1.0)));e.putInt(forgettingObservationKey(dim),Math.max(0,x.optInt("observations",0)));}}
        JSONArray conf=o.optJSONArray("confusions");if(conf!=null)for(int i=0;i<conf.length();i++){JSONObject x=conf.getJSONObject(i);e.putInt(confusionKey(x.getInt("a"),x.getInt("b")),x.optInt("score",1));}
        JSONArray daily=o.optJSONArray("daily");if(daily!=null)for(int i=0;i<daily.length();i++){JSONObject x=daily.getJSONObject(i);LocalDate d=LocalDate.parse(x.getString("date"));e.putInt(dayKey("day_cards_",d),x.optInt("cards",0));e.putInt(dayKey("day_att_",d),x.optInt("attempts",0));e.putInt(dayKey("day_cor_",d),x.optInt("correct",0));e.putLong(dayKey("day_ms_",d),x.optLong("ms",0));e.putLong(dayKey("day_active_sec_",d),x.optLong("activeSec",0));}
        JSONArray dailySpeaking=o.optJSONArray("dailySpeaking");if(dailySpeaking!=null)for(int i=0;i<dailySpeaking.length();i++){JSONObject x=dailySpeaking.getJSONObject(i);LocalDate d=LocalDate.parse(x.getString("date"));e.putInt(dayKey("auxday_daily_speaking_att_",d),Math.max(0,x.optInt("attempts",0)));e.putInt(dayKey("auxday_daily_speaking_cor_",d),Math.max(0,x.optInt("correct",0)));}
        JSONObject bt=o.optJSONObject("breakthroughCycle");if(bt!=null){e.putString(BT_SKILL,bt.optString("skill","")).putString(BT_LEVEL,bt.optString("level","A1")).putInt(BT_PHASE,Math.max(1,bt.optInt("phase",1))).putInt(BT_BASE,clamp(bt.optInt("baseline",0),0,100)).putInt(BT_CYCLES,Math.max(0,bt.optInt("cycles",0)));long startDay=bt.optLong("startDay",Long.MIN_VALUE);if(startDay!=Long.MIN_VALUE)e.putLong(BT_START,startDay);long lastDay=bt.optLong("lastCompletedDay",Long.MIN_VALUE);if(lastDay!=Long.MIN_VALUE)e.putLong(BT_LAST,lastDay);}
        JSONObject aux=o.optJSONObject("auxStats");if(aux!=null)for(String type:new String[]{"pattern","dialogue","dialogue_speaking","dialogue_scenario","life_task","freechat","shadowing","pronunciation","mission","level_exam","reading","sentence_dictation","error_repair","verb_center","cloze","word_family","intensive_listening","stubborn","phrases","preposition","past_tense","pronouns","family_train","rescue","writing","sentence_fsrs","error_evidence_repair","weekly_exam","active_recall","listening_course","listen_speak","daily_speaking","focus_mode","core_sentences","memory_article","memory_article_sentence","weak_story","pron_double","pron_r","pron_gli","pron_gn","pron_hard_soft"}){JSONObject x=aux.optJSONObject(type);if(x!=null){e.putInt(auxKey(type,"att"),x.optInt("attempts",0));e.putInt(auxKey(type,"cor"),x.optInt("correct",0));}}
        JSONArray grammar=o.optJSONArray("grammarStats");if(grammar!=null)for(int i=0;i<grammar.length();i++){JSONObject x=grammar.getJSONObject(i);String id=x.optString("id");if(!id.isEmpty()){e.putInt(grammarKey(id,"att"),x.optInt("attempts",0));e.putInt(grammarKey(id,"cor"),x.optInt("correct",0));e.putInt(grammarKey(id,"iv"),x.optInt("interval",0));long gd=x.optLong("due",Long.MIN_VALUE);if(gd!=Long.MIN_VALUE)e.putLong(grammarKey(id,"due"),gd);e.putLong(grammarKey(id,"st"),Double.doubleToRawLongBits(x.optDouble("stability",1.0)));e.putLong(grammarKey(id,"diff"),Double.doubleToRawLongBits(x.optDouble("difficulty",5.0)));}}JSONObject exams=o.optJSONObject("examStats");if(exams!=null)for(String level:new String[]{"A1","A2","B1"}){JSONObject x=exams.optJSONObject(level);if(x!=null){String k="exam_"+level;e.putInt(k+"_last",x.optInt("last",0));e.putInt(k+"_best",x.optInt("best",0));}}JSONObject readingScores=o.optJSONObject("readingScores");if(readingScores!=null){java.util.Iterator<String> keys=readingScores.keys();while(keys.hasNext()){String id=keys.next();e.putInt(readingKey(id),readingScores.optInt(id,0));}}JSONObject errorCauses=o.optJSONObject("errorCauseCounts");if(errorCauses!=null){java.util.Iterator<String> keys=errorCauses.keys();while(keys.hasNext()){String k=keys.next();e.putInt(errorKey(k),errorCauses.optInt(k,0));}}JSONObject courseDone=o.optJSONObject("courseDone");if(courseDone!=null){java.util.Iterator<String> keys=courseDone.keys();while(keys.hasNext()){String k=keys.next();if(k.startsWith("course_done_"))e.putBoolean(k,courseDone.optBoolean(k,false));}}JSONObject memoryDone=o.optJSONObject("memoryArticleDone");if(memoryDone!=null){java.util.Iterator<String> keys=memoryDone.keys();while(keys.hasNext()){String k=keys.next();if(k.startsWith("memory_article_done_"))e.putBoolean(k,memoryDone.optBoolean(k,false));}}JSONObject memoryExposure=o.optJSONObject("memoryArticleExposure");if(memoryExposure!=null){java.util.Iterator<String> keys=memoryExposure.keys();while(keys.hasNext()){String k=keys.next();if(k.startsWith("memory_article_exposure_"))e.putInt(k,Math.max(0,memoryExposure.optInt(k,0)));}}JSONObject memorySentenceDone=o.optJSONObject("memoryArticleSentenceDone");if(memorySentenceDone!=null){java.util.Iterator<String> keys=memorySentenceDone.keys();while(keys.hasNext()){String k=keys.next();if(k.startsWith("memory_article_sentence_done_"))e.putBoolean(k,memorySentenceDone.optBoolean(k,false));}}JSONObject dialogueScenarioProgress=o.optJSONObject("dialogueScenarioProgress");if(dialogueScenarioProgress!=null){java.util.Iterator<String> keys=dialogueScenarioProgress.keys();while(keys.hasNext()){String k=keys.next();if(k.startsWith("dialogue_scenario_")&&(k.endsWith("_done")||k.endsWith("_best")))e.putInt(k,Math.max(0,dialogueScenarioProgress.optInt(k,0)));}}JSONObject weekly=o.optJSONObject("weeklyExam");if(weekly!=null){long wd=weekly.optLong("lastDay",Long.MIN_VALUE);if(wd!=Long.MIN_VALUE)e.putLong(WEEK_EXAM_DAY,wd);e.putInt(WEEK_EXAM_SCORE,clamp(weekly.optInt("score",0),0,100)).putInt(WEEK_EXAM_COUNT,Math.max(0,weekly.optInt("count",0))).putString(WEEK_FOCUS_1,weekly.optString("focus1",MasteryPassportEngine.ACTION_MEANING)).putString(WEEK_FOCUS_2,weekly.optString("focus2",MasteryPassportEngine.ACTION_LISTENING));long wu=weekly.optLong("focusUntil",Long.MIN_VALUE);if(wu!=Long.MIN_VALUE)e.putLong(WEEK_FOCUS_UNTIL,wu);long bd=weekly.optLong("baselineDay",Long.MIN_VALUE);if(bd!=Long.MIN_VALUE)e.putLong(WEEK_BASELINE_DAY,bd);for(String key:WeeklyExamEngine.SKILL_KEYS){e.putInt(weekSkillKey("week_exam_skill_",key),clamp(weekly.optInt("skill_"+key,0),0,100)).putInt(weekSkillKey("week_exam_before_",key),clamp(weekly.optInt("before_"+key,0),0,100)).putInt(weekSkillKey("week_exam_after_",key),clamp(weekly.optInt("after_"+key,0),0,100)).putInt(weekSkillKey("week_baseline_",key),clamp(weekly.optInt("baseline_"+key,0),0,100));}}e.apply();
    }

    public void reset(){String start=startDate().toString();int per=perDay(),minutes=sessionMinutes(),pron=pronunciationMode(),route=vocabularyRouteLimit(),goal=learningGoal(),font=fontScaleMode(),speed=audioSpeedMode();double retention=desiredRetention();boolean audio=preferOriginalAudio(),reminder=reminderEnabled(),onboard=onboardingCompleted(),simpleHome=homeSimpleMode();int rh=reminderHour(),rm=reminderMinute();p.edit().clear().putString("start",start).putInt("perDay",per).putBoolean("originalAudio",audio).putBoolean("reminderEnabled",reminder).putInt("reminderHour",rh).putInt("reminderMinute",rm).putInt("session_minutes",minutes).putInt("pron_mode",pron).putInt("vocab_route",route).putInt("learning_goal",goal).putInt("font_scale_mode",font).putInt("audio_speed_mode",speed).putBoolean("onboarding_done",onboard).putBoolean("home_simple_mode",simpleHome).putFloat("fs_retention",(float)retention).apply();LearningDatabase.clear(context);}
}
