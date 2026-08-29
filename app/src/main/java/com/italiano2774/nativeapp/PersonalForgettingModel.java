package com.italiano2774.nativeapp;

/**
 * v4.9 learner-specific forgetting calibration shared by the four vocabulary dimensions.
 * A factor below 1.0 means this channel tends to decay faster than the base FSRS estimate;
 * above 1.0 means the learner has repeatedly retained it longer than expected.
 */
public final class PersonalForgettingModel {
    private PersonalForgettingModel() {}

    public static double clampFactor(double value){return Math.max(0.70,Math.min(1.30,value));}

    /**
     * Slowly adapts a dimension-wide interval factor from real recall evidence.
     * The update is intentionally conservative: per-card FSRS remains the primary scheduler.
     */
    public static double updateFactor(double current, boolean correct, double retrievability, int quality){
        double cur=clampFactor(current<=0?1.0:current);
        double target;
        if(quality==2){
            // "Hard / fuzzy" is weak evidence, not a full lapse. Shorten cautiously.
            target=retrievability>=0.82?0.92:0.97;
        }else if(!correct){
            target=retrievability>=0.82?0.78:(retrievability>=0.68?0.86:0.94);
        }else if(retrievability<=0.72){
            target=quality>=4?1.24:1.16;
        }else if(retrievability<=0.82){
            target=quality>=4?1.14:1.08;
        }else{
            target=quality>=4?1.06:1.01;
        }
        return clampFactor(cur*0.90+target*0.10);
    }

    public static int scaledInterval(int baseDays,double factor,int quality){
        if(quality<=1)return 0;
        return Math.max(1,Math.min(3650,(int)Math.round(Math.max(1,baseDays)*clampFactor(factor))));
    }

    public static String speedLabel(double factor,int observations){
        if(observations<5)return "校准中";
        double f=clampFactor(factor);
        if(f<0.86)return "较容易忘";
        if(f<0.96)return "略快遗忘";
        if(f<=1.05)return "接近基准";
        if(f<=1.15)return "保持较稳";
        return "保持很稳";
    }

    public static String dimensionLabel(int dim){
        switch(dim){
            case ProgressStore.DIM_LISTENING:return "听力";
            case ProgressStore.DIM_SPELLING:return "拼写";
            case ProgressStore.DIM_SPEAKING:return "口语";
            default:return "识义";
        }
    }
}
