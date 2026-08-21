package com.italiano2774.nativeapp;
import org.junit.Test;import static org.junit.Assert.*;import java.util.*;
public class WritingEvaluatorTest{
 @Test public void completeTaskScoresHigher(){WritingPrompt p=new WritingPrompt();p.minWords=5;p.keywords=Arrays.asList("vorrei","caffè|tè","per favore");WritingEvaluator.Result a=WritingEvaluator.evaluate(p,"Vorrei un caffè, per favore.");WritingEvaluator.Result b=WritingEvaluator.evaluate(p,"Ciao.");assertTrue(a.score>b.score);assertTrue(a.score>=70);}
 @Test public void missingKeywordIsReported(){WritingPrompt p=new WritingPrompt();p.minWords=3;p.keywords=Arrays.asList("appuntamento","domani");WritingEvaluator.Result r=WritingEvaluator.evaluate(p,"Vorrei parlare oggi.");assertFalse(r.missing.isEmpty());}
}
