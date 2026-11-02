package com.gathin.tvhome;

/** Invalidates asynchronous reveal work when playback is paused, restarted or closed. */
final class VideoCoverState {
    private int generation;
    private boolean waiting, closed, live;
    int begin() { live = !closed; waiting = !closed; return ++generation; }
    void hold() { live = false; waiting = false; generation++; }
    boolean acceptsCopy(int ticket) { return !closed && live && ticket == generation; }
    boolean canReveal(int ticket) { return !closed && waiting && ticket == generation; }
    void revealed(int ticket) { if (canReveal(ticket)) waiting = false; }
    boolean waiting() { return waiting && !closed; }
    int ticket() { return generation; }
    void close() { hold(); closed = true; }
}
