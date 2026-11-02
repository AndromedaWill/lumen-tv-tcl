package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class VideoCoverStateTest {
    @Test public void pauseBeforeCopyCompletesKeepsStillVisible() {
        VideoCoverState state=new VideoCoverState(); int request=state.begin();
        state.hold(); assertFalse(state.canReveal(request));
    }
    @Test public void oldPlaybackCannotRevealAfterResume() {
        VideoCoverState state=new VideoCoverState(); int old=state.begin(); state.hold(); int current=state.begin();
        assertFalse(state.canReveal(old)); assertTrue(state.canReveal(current));
    }
    @Test public void revealingIsConsumedOnce() {
        VideoCoverState state=new VideoCoverState(); int request=state.begin();
        state.revealed(request); assertFalse(state.canReveal(request)); assertFalse(state.waiting());
    }
    @Test public void staleCallbackCannotConsumeNewPlayback() {
        VideoCoverState state=new VideoCoverState(); int old=state.begin(); int current=state.begin();
        state.revealed(old); assertTrue(state.canReveal(current));
    }
    @Test public void closingInvalidatesAllQueuedWork() {
        VideoCoverState state=new VideoCoverState(); int request=state.begin(); state.close();
        assertFalse(state.canReveal(request)); assertFalse(state.canReveal(state.begin())); assertFalse(state.waiting());
    }
}
