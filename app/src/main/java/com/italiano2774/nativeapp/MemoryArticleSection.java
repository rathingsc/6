package com.italiano2774.nativeapp;

import java.util.ArrayList;
import java.util.List;

/** A short memorisable section inside a 200-word memory article. */
public class MemoryArticleSection {
    public String id="",title="",titleZh="",text="",translation="";
    public final List<Integer> targetWordIds=new ArrayList<>();
    /** Older words deliberately brought back after they were introduced earlier in the route. */
    public final List<Integer> reviewWordIds=new ArrayList<>();
    public final List<String> clozeWords=new ArrayList<>();
    public final List<MemoryArticleReinforcement> reinforcementItems=new ArrayList<>();
    /** Sentence-aligned text for the v3.3.8 sentence-by-sentence memorisation mode. */
    public final List<MemoryArticleSentence> sentences=new ArrayList<>();
}
