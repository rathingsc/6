package com.italiano2774.nativeapp;
import org.junit.Test;import static org.junit.Assert.*;
public class ErrorCauseAnalyzerTest{
 @Test public void detectsOmission(){ErrorCauseAnalyzer.SentenceAnalysis a=ErrorCauseAnalyzer.analyzeSentence("Vorrei un caffè per favore","Vorrei un caffè",null);assertTrue(a.causes.contains(ErrorCause.OMISSION));assertTrue(a.score<100);}
 @Test public void detectsWordOrder(){ErrorCauseAnalyzer.SentenceAnalysis a=ErrorCauseAnalyzer.analyzeSentence("Io studio italiano","Italiano studio io",null);assertTrue(a.causes.contains(ErrorCause.WORD_ORDER));}
 @Test public void detectsAccent(){ErrorCauseAnalyzer.SentenceAnalysis a=ErrorCauseAnalyzer.analyzeSentence("Prendo un caffè","Prendo un caffe",null);assertTrue(a.causes.contains(ErrorCause.ACCENT));}
 @Test public void spellingDistanceWorks(){assertEquals(1,ErrorCauseAnalyzer.levenshtein("appuntamento","appuntameto"));}
}
