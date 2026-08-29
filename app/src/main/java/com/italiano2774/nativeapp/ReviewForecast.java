package com.italiano2774.nativeapp;

import java.util.ArrayList;
import java.util.List;

/** Seven-day review workload projection used by the consolidation calendar and planner. */
public class ReviewForecast {
    public final List<ReviewForecastDay> days=new ArrayList<>();
    public int startingBacklog;
    public int futureArrivals;
    public int peakPending;
    public int remainingAfterWindow;
    public int riskLevel;
    public String advice="";

    public ReviewForecastDay today(){return days.isEmpty()?null:days.get(0);}
    public ReviewForecastDay tomorrow(){return days.size()<2?null:days.get(1);}
}
