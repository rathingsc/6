package com.italiano2774.nativeapp;

import java.util.ArrayList;
import java.util.List;

/** One of the ten long-form vocabulary memory articles. */
public class MemoryArticle {
    public String id="",title="",titleZh="",subtitle="",emoji="📖";
    public final List<Integer> targetWordIds=new ArrayList<>();
    public final List<MemoryArticleSection> sections=new ArrayList<>();
}
