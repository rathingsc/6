package com.italiano2774.nativeapp;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Local comprehensible-input scorer. No network or AI is used. */
public class ReadingComprehensibilityEngine {
    public static class Score {
        public int coverage;
        public int totalTokens;
        public int knownTokens;
        public List<String> unknown = new ArrayList<>();
        public String label(){
            if(coverage>=90 && coverage<=98)return "非常适合";
            if(coverage>=80)return "有一点挑战";
            if(coverage>98)return "偏容易";
            return "暂缓阅读";
        }
    }
    private final WordRepository repo;private final ProgressStore progress;
    public ReadingComprehensibilityEngine(WordRepository repo,ProgressStore progress){this.repo=repo;this.progress=progress;}

    public Score score(ReadingPassage passage){
        Score s=new Score();if(passage==null||passage.text==null)return s;
        Set<String> unknownUnique=new LinkedHashSet<>();
        for(String raw:passage.text.split("\\s+")){
            String token=normalize(raw);if(token.length()<2)continue;s.totalTokens++;
            Word w=repo.lookupSurface(token);
            if(w!=null&&progress.meaningLevel(w.id)>=2)s.knownTokens++;
            else if(isTransparent(token))s.knownTokens++;
            else if(unknownUnique.size()<12)unknownUnique.add(token);
        }
        s.coverage=s.totalTokens==0?100:(int)Math.round(s.knownTokens*100.0/s.totalTokens);s.unknown.addAll(unknownUnique);return s;
    }
    public List<ReadingPassage> recommended(List<ReadingPassage> input){
        // v2.7.8: calculate each passage only once. The previous comparator called
        // score() repeatedly while sorting, multiplying token lookup work.
        List<ReadingPassage> out=new ArrayList<>(input);
        final java.util.IdentityHashMap<ReadingPassage,Integer> distance=new java.util.IdentityHashMap<>();
        for(ReadingPassage p:out)distance.put(p,Math.abs(93-score(p).coverage));
        Collections.sort(out, Comparator.comparingInt(p->distance.get(p)));
        return out;
    }
    private boolean isTransparent(String t){
        return t.matches("[0-9]+")||t.equals("a")||t.equals("e")||t.equals("o")||t.equals("ma")||t.equals("non")||t.equals("si")||t.equals("no");
    }
    private String normalize(String s){String n=Normalizer.normalize(s==null?"":s.toLowerCase(Locale.ITALIAN).replace('’','\''),Normalizer.Form.NFC);return n.replaceAll("^[^\\p{L}]+|[^\\p{L}']+$","");}
}
