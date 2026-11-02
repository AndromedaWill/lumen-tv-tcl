package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class VideoCoverStateTest {
    @Test public void lateCopyCannotReplaceValidStillAfterPause() {
        VideoCoverState state=new VideoCoverState();assertFalse(state.acceptsCopy(state.ticket()));
        int old=state.begin();state.hold();
        assertFalse(state.acceptsCopy(old));
        int current=state.begin();assertFalse(state.acceptsCopy(old));assertTrue(state.acceptsCopy(current));
        state.revealed(current);assertTrue(state.acceptsCopy(current));
        state.close();assertFalse(state.acceptsCopy(current));
    }
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
