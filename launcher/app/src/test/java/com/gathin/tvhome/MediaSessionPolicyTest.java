package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class MediaSessionPolicyTest {
    @Test public void playingOutranksBufferedAndPaused(){assertTrue(MediaSessionPolicy.rank(3)>MediaSessionPolicy.rank(6));assertTrue(MediaSessionPolicy.rank(6)>MediaSessionPolicy.rank(2));}
    @Test public void staleOrErrorSessionsAreHidden(){for(int state:new int[]{0,1,7,-1,100})assertEquals(0,MediaSessionPolicy.rank(state));}
    @Test public void pauseRequiresPauseOrToggle(){assertFalse(MediaSessionPolicy.canToggle(3,4));assertTrue(MediaSessionPolicy.canToggle(3,2));assertTrue(MediaSessionPolicy.canToggle(3,512));}
    @Test public void resumeRequiresPlayOrToggle(){assertFalse(MediaSessionPolicy.canToggle(2,2));assertTrue(MediaSessionPolicy.canToggle(2,4));assertTrue(MediaSessionPolicy.canToggle(2,512));}
    @Test public void noCommandsForInvalidState(){assertFalse(MediaSessionPolicy.canToggle(0,0xffff));assertFalse(MediaSessionPolicy.canToggle(7,0xffff));}
    @Test public void independentSkipCapabilities(){assertTrue(MediaSessionPolicy.supports(32,MediaSessionPolicy.NEXT));assertFalse(MediaSessionPolicy.supports(32,MediaSessionPolicy.PREVIOUS));}
    @Test public void bufferingCanBePaused(){assertTrue(MediaSessionPolicy.canToggle(6,2));assertTrue(MediaSessionPolicy.pausable(8));}
}
