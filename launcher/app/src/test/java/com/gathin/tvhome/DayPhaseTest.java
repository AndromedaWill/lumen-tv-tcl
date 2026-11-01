package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class DayPhaseTest {
    @Test public void midnightRemainsNightUntilAlba() {
        assertEquals(5,DayPhase.atMinute(1439)); assertEquals(5,DayPhase.atMinute(0));
        assertEquals(5,DayPhase.atMinute(299)); assertEquals(0,DayPhase.atMinute(300));
    }
    @Test public void everyBoundarySwitchesExactlyOnce() {
        int[] starts={300,420,540,960,1110,1230};
        for(int i=0;i<starts.length;i++) {
            assertEquals((i+5)%6,DayPhase.atMinute(starts[i]-1));
            assertEquals(i,DayPhase.atMinute(starts[i]));
        }
    }
    @Test(expected=IllegalArgumentException.class) public void invalidTimeRejected(){DayPhase.atMinute(1440);}
}
