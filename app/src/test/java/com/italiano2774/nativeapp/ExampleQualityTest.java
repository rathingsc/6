package com.italiano2774.nativeapp;
import org.junit.Test;
import static org.junit.Assert.*;

public class ExampleQualityTest {
    private Word w(String word,String it,String zh){Word w=new Word();w.word=word;w.example=it;w.exampleZh=zh;return w;}
    @Test public void rejectsSelfReferentialPlaceholder(){assertFalse(ExampleQuality.isUsable(w("sto","Oggi ripasso la parola «sto».","我今天复习单词「sto」。")));}
    @Test public void acceptsRealContext(){assertTrue(ExampleQuality.isUsable(w("sto","Sto cercando lavoro.","我正在找工作。")));}
    @Test public void rejectsPlaceholderTranslation(){assertFalse(ExampleQuality.isUsable(w("rosso","Questo oggetto è rosso.","这个东西具有这种特征。")));}
}
