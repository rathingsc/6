package com.italiano2774.nativeapp;
import org.junit.Test;
import static org.junit.Assert.*;
public class SmartMemorySchedulerTest{
 @Test public void rememberedUsesExpectedLadder(){assertEquals(1,SmartMemoryScheduler.nextInterval(2,0,1,1));assertEquals(3,SmartMemoryScheduler.nextInterval(2,1,2,1));assertEquals(7,SmartMemoryScheduler.nextInterval(2,2,3,3));assertEquals(15,SmartMemoryScheduler.nextInterval(2,3,4,7));assertEquals(30,SmartMemoryScheduler.nextInterval(2,4,4,15));}
 @Test public void forgotReturnsTomorrow(){assertEquals(1,SmartMemoryScheduler.nextInterval(0,8,5,240));}
 @Test public void fuzzyReturnsSoon(){assertEquals(1,SmartMemoryScheduler.nextInterval(1,3,2,30));assertEquals(2,SmartMemoryScheduler.nextInterval(1,3,4,30));}
 @Test public void rememberedRaisesMeaning(){assertEquals(2,SmartMemoryScheduler.nextMeaningLevel(2,0));assertEquals(5,SmartMemoryScheduler.nextMeaningLevel(2,5));}
}
