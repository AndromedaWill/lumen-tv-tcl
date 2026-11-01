package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class BackdropSizeTest {
    @Test public void tclUiKeepsFull1080Detail() {
        BackdropSize s=BackdropSize.forViewport(1920,1080,1920,1275);
        assertEquals(1920,s.width); assertEquals(1080,s.height); assertEquals(1,s.sample);
    }
    @Test public void hdUiDoesNotDecodeBelowItsCanvasResolution() {
        BackdropSize s=BackdropSize.forViewport(1280,720,1920,1275);
        assertEquals(1280,s.width); assertEquals(720,s.height); assertEquals(1,s.sample);
    }
    @Test public void largerPanelCannotAddDetailMissingFromSource() {
        BackdropSize s=BackdropSize.forViewport(3840,2160,1920,1275);
        assertEquals(1920,s.width); assertEquals(1080,s.height); assertEquals(1,s.sample);
    }
    @Test public void smallerViewportCanSampleOnlyIfStillCovered() {
        BackdropSize s=BackdropSize.forViewport(640,360,1920,1275);
        assertTrue(1920/s.sample>=s.width); assertTrue(1275/s.sample>=s.height);
        assertEquals(2,s.sample);
    }
    @Test(expected=IllegalArgumentException.class) public void rejectMissingSource() {
        BackdropSize.forViewport(1920,1080,0,0);
    }
}
