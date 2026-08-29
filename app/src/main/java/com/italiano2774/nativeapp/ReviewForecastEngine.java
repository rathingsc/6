package com.italiano2774.nativeapp;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * v3.9.0 seven-day consolidation forecast.
 *
 * It treats everything already due as today's backlog, then adds words on their exact
 * scheduled due date. Each day consumes only a realistic amount for the learner's selected
 * time budget; unfinished reviews are carried forward instead of disappearing from the chart.
 */
public class ReviewForecastEngine {
    private final List<Word> words;
    private final ProgressStore progress;

    public ReviewForecastEngine(List<Word> words,ProgressStore progress){this.words=words;this.progress=progress;}

    public ReviewForecast build(){return build(7);}

    public ReviewForecast build(int requestedDays){
        int count=Math.max(2,Math.min(14,requestedDays));LocalDate today=LocalDate.now();int[] arrivals=new int[count];int backlog=0;
        for(Word w:words){
            if(progress.mastery(w.id)<=0)continue;
            LocalDate due=progress.nextDueDate(w.id);
            if(due==null||!due.isAfter(today)){backlog++;continue;}
            long offset=ChronoUnit.DAYS.between(today,due);
            if(offset>=1&&offset<count)arrivals[(int)offset]++;
        }
        ReviewForecast out=new ReviewForecast();out.startingBacklog=backlog;int baseCapacity=baseCapacity(progress.sessionMinutes());int carry=backlog;
        for(int i=0;i<count;i++){
            ReviewForecastDay d=new ReviewForecastDay();d.date=today.plusDays(i);d.scheduledArrivals=i==0?0:arrivals[i];if(i>0)out.futureArrivals+=d.scheduledArrivals;
            int pending=carry+d.scheduledArrivals;d.pendingBeforeReview=pending;d.capacity=i==0?Math.min(baseCapacity,progress.protectedReviewCap(words)):baseCapacity;d.recommendedReviews=Math.min(pending,d.capacity);d.carryAfter=Math.max(0,pending-d.recommendedReviews);d.estimatedMinutes=estimateMinutes(d.recommendedReviews);out.peakPending=Math.max(out.peakPending,pending);out.days.add(d);carry=d.carryAfter;
        }
        out.remainingAfterWindow=carry;ReviewForecastDay first=out.today(),second=out.tomorrow();
        if(out.remainingAfterWindow>0||(first!=null&&first.carryAfter>0)||(second!=null&&second.pendingBeforeReview>second.capacity))out.riskLevel=2;
        else if(out.peakPending>Math.round(baseCapacity*0.78)||out.futureArrivals>baseCapacity*2)out.riskLevel=1;else out.riskLevel=0;
        if(out.riskLevel>=2)out.advice="未来复习负载偏高：先清积压，系统会自动压低新词，不要求补双倍。";
        else if(out.riskLevel==1)out.advice="未来几天有一波复习到期：保持当前时长即可，今天少量控制新词。";
        else out.advice="未来7天复习量平稳，可以按当前节奏继续推进新内容。";
        return out;
    }

    public static int baseCapacity(int minutes){return minutes<=10?18:(minutes<=20?30:(minutes<=30?48:(minutes<=45?60:72)));}
    private int estimateMinutes(int reviews){return reviews<=0?0:(int)Math.ceil(reviews*0.40);}
}
