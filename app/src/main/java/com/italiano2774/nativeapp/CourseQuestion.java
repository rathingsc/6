package com.italiano2774.nativeapp;

import java.util.ArrayList;
import java.util.List;

/** A deliberately small set of question types so beginners face one clear action at a time. */
public class CourseQuestion {
    public static final int INTRO=0,MEANING=1,LISTEN=2,SPELL_HINT=3,CLOZE=4,ACTIVE=5,EXAMPLE_MEANING=6;
    public int type,dimension=ProgressStore.DIM_MEANING;
    public Word word;
    public String prompt="",display="",support="",answer="",hint="";
    public final List<String> options=new ArrayList<>();
    public boolean autoPlayAudio;
}
