package com.italiano2774.nativeapp;

import java.time.LocalDate;

/** One day in the v3.9.0 consolidation forecast. */
public class ReviewForecastDay {
    public LocalDate date;
    public int scheduledArrivals;
    public int pendingBeforeReview;
    public int recommendedReviews;
    public int carryAfter;
    public int capacity;
    public int estimatedMinutes;

    public boolean overloaded(){return carryAfter>0;}
    public int loadPercent(){return capacity<=0?0:(int)Math.round(pendingBeforeReview*100.0/capacity);}
}
