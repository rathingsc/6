package com.italiano2774.nativeapp;

import java.util.ArrayList;
import java.util.List;

/** One ordered unit in the v3.0 beginner-first course path. */
public class CourseUnit {
    public String id="",stage="",titleZh="",titleIt="",subtitle="";
    public int index,stageUnit,lessonCount;
    public final List<Integer> wordIds=new ArrayList<>();
    public final List<String> sourceSkills=new ArrayList<>();
}
