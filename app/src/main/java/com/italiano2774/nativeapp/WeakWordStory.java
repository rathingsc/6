package com.italiano2774.nativeapp;

import java.util.ArrayList;
import java.util.List;

/** One locally assembled micro-reading built from audited app content and the learner's weak words. */
public class WeakWordStory {
    public static class ClozeTarget {
        public int wordId;
        public String answer="";
        public String chinese="";
        public ClozeTarget(int wordId,String answer,String chinese){this.wordId=wordId;this.answer=answer==null?"":answer;this.chinese=chinese==null?"":chinese;}
    }
    public String title="今日弱词微短文";
    public String sourceLabel="";
    public String italian="";
    public String chinese="";
    public int weakCandidateCount=0;
    public int wordCount=0;
    public int variant=0;
    public final List<Word> targetWords=new ArrayList<>();
    public final List<MemoryArticleSentence> sentences=new ArrayList<>();
    public final List<ClozeTarget> clozeTargets=new ArrayList<>();

    public String targetSummary(){StringBuilder sb=new StringBuilder();for(Word w:targetWords){if(sb.length()>0)sb.append("  ·  ");sb.append(w.word);if(w.chinese!=null&&!w.chinese.trim().isEmpty())sb.append(" ").append(w.chinese);}return sb.toString();}
}
