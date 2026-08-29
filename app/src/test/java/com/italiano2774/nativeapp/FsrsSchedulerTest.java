package com.italiano2774.nativeapp;
import org.junit.Test;import static org.junit.Assert.*;
public class FsrsSchedulerTest{
 @Test public void easySchedulesLongerThanAgain(){FsrsScheduler.Result again=FsrsScheduler.schedule(5,5,1,3,.90);FsrsScheduler.Result easy=FsrsScheduler.schedule(5,5,4,3,.90);assertTrue(easy.intervalDays>again.intervalDays);assertTrue(easy.stability>again.stability);}
 @Test public void retrievabilityFallsWithTime(){assertTrue(FsrsScheduler.retrievability(10,1)>FsrsScheduler.retrievability(10,20));}
}
