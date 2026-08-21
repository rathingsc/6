package com.italiano2774.nativeapp;

import java.util.List;

/** Internal learning score for guidance only; it is not an official CEFR score. */
public final class LearningScoreEngine {
    public static class Summary {
        public int score,meaning,listening,spelling,speaking,coverage;
        public String stage;
    }
    private LearningScoreEngine(){}
    public static Summary calculate(WordRepository repo, ProgressStore progress){
        Summary s=new Summary();List<Word> all=repo.all();if(all.isEmpty()){s.stage="A1 起步";return s;}
        long sm=0,sl=0,ss=0,sp=0;int introduced=0;
        for(Word w:all){sm+=progress.meaningLevel(w.id);sl+=progress.listeningLevel(w.id);ss+=progress.spellingLevel(w.id);sp+=progress.speakingLevel(w.id);if(progress.mastery(w.id)>0)introduced++;}
        double denom=all.size()*5.0;
        s.meaning=(int)Math.round(sm*100.0/denom);s.listening=(int)Math.round(sl*100.0/denom);s.spelling=(int)Math.round(ss*100.0/denom);s.speaking=(int)Math.round(sp*100.0/denom);s.coverage=(int)Math.round(introduced*100.0/all.size());
        int checkpoint=Math.max(progress.bestExamScore("A1"),(int)Math.round(progress.bestExamScore("A2")*0.8));checkpoint=Math.max(checkpoint,(int)Math.round(progress.bestExamScore("B1")*0.65));
        s.score=(int)Math.round(s.meaning*0.30+s.listening*0.23+s.spelling*0.17+s.speaking*0.20+s.coverage*0.07+checkpoint*0.03);s.score=Math.max(0,Math.min(100,s.score));
        if(s.score<18)s.stage="A1 起步";else if(s.score<35)s.stage="A1 巩固";else if(s.score<55)s.stage="A2 进阶";else if(s.score<75)s.stage="A2-B1 过渡";else s.stage="B1 强化";
        return s;
    }
    /** v2.7.8 fast path for the Today dashboard. */
    public static Summary calculate(ProgressStore.DashboardStats d, ProgressStore progress){
        Summary s=new Summary();if(d==null){s.stage="A1 起步";return s;}
        s.meaning=d.allDimensionPct[ProgressStore.DIM_MEANING];s.listening=d.allDimensionPct[ProgressStore.DIM_LISTENING];s.spelling=d.allDimensionPct[ProgressStore.DIM_SPELLING];s.speaking=d.allDimensionPct[ProgressStore.DIM_SPEAKING];
        s.coverage=(int)Math.round(d.totalIntroduced*100.0/Math.max(1,d.totalWords));
        int checkpoint=Math.max(progress.bestExamScore("A1"),(int)Math.round(progress.bestExamScore("A2")*0.8));checkpoint=Math.max(checkpoint,(int)Math.round(progress.bestExamScore("B1")*0.65));
        s.score=(int)Math.round(s.meaning*0.30+s.listening*0.23+s.spelling*0.17+s.speaking*0.20+s.coverage*0.07+checkpoint*0.03);s.score=Math.max(0,Math.min(100,s.score));
        if(s.score<18)s.stage="A1 起步";else if(s.score<35)s.stage="A1 巩固";else if(s.score<55)s.stage="A2 进阶";else if(s.score<75)s.stage="A2-B1 过渡";else s.stage="B1 强化";
        return s;
    }

}
