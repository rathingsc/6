package com.italiano2774.nativeapp;
import org.junit.Test;import static org.junit.Assert.*;
public class GrammarFsrsTest{
 @Test public void correctAnswerExtendsGrammarInterval(){GrammarProgressEntity x=new GrammarProgressEntity();x.grammarId="test";GrammarProgressEntity a=GrammarFsrs.next(x,"test",true);int first=a.intervalDays;GrammarProgressEntity b=GrammarFsrs.next(a,"test",true);assertTrue(b.intervalDays>=first);}
 @Test public void mistakeReturnsSoon(){GrammarProgressEntity x=new GrammarProgressEntity();x.grammarId="test";x.intervalDays=20;x.stability=20;GrammarProgressEntity r=GrammarFsrs.next(x,"test",false);assertEquals(1,r.intervalDays);}
}
