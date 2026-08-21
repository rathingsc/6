package com.italiano2774.nativeapp;
import org.junit.Test;import static org.junit.Assert.*;import java.util.*;
public class SessionQualityEngineTest{
 @Test public void detectsRepeatedStruggle(){assertTrue(SessionQualityEngine.isFatigued(Arrays.asList(0,1,0,1,0,2),Arrays.asList(10000L,11000L,9000L,10000L,12000L,8000L)));}
 @Test public void doesNotFlagHealthyRun(){assertFalse(SessionQualityEngine.isFatigued(Arrays.asList(2,3,2,3,2,2,3),Arrays.asList(2500L,3000L,2800L,3200L,2600L,3100L,2400L)));}
}
