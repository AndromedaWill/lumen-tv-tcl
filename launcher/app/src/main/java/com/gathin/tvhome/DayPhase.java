package com.gathin.tvhome;

/** Local wall-clock phases, including the wrap through midnight. No network or location. */
final class DayPhase {
    private static final int[] START = {300, 420, 540, 960, 1110, 1230};
    static int atMinute(int minute) {
        if (minute < 0 || minute >= 1440) throw new IllegalArgumentException("Minute outside day");
        for (int i=START.length-1;i>=0;i--) if(minute>=START[i])return i;
        return 5;
    }
}
