package com.gathin.tvhome;

/** Pure policy; values follow framework PlaybackState (API 21+). No app/package aliases. */
final class MediaSessionPolicy {
    static final long PLAY = 4, PAUSE = 2, PREVIOUS = 16, NEXT = 32, TOGGLE = 512;
    static boolean playing(int state) { return state == 3 || state == 4 || state == 5; }
    static boolean pausable(int state) { return playing(state)||state==6||state==8||state==9||state==10||state==11; }
    static int rank(int state) {
        if (playing(state)) return 3;
        if (state == 6 || state == 8 || state == 9 || state == 10 || state == 11) return 2;
        return state == 2 ? 1 : 0;
    }
    static boolean canToggle(int state, long actions) {
        return rank(state) > 0 && (actions & ((pausable(state) ? PAUSE : PLAY) | TOGGLE)) != 0;
    }
    static boolean supports(long actions, long action) { return (actions & action) != 0; }
}
