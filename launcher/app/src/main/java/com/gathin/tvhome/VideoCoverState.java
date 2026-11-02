package com.gathin.tvhome;

/** Invalidates asynchronous reveal work when playback is paused, restarted or closed. */
final class VideoCoverState {
    private int generation;
    private boolean waiting, closed;
    int begin() { waiting = !closed; return ++generation; }
    void hold() { waiting = false; generation++; }
    boolean canReveal(int ticket) { return !closed && waiting && ticket == generation; }
    void revealed(int ticket) { if (canReveal(ticket)) waiting = false; }
    boolean waiting() { return waiting && !closed; }
    int ticket() { return generation; }
    void close() { hold(); closed = true; }
}
