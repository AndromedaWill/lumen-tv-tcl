package com.gathin.tvhome;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.provider.Settings;

import java.io.File;

/** Optional app-open sound. Android handles navigation sounds. */
final class NavSound {
    // Keep accepting legacy theme assets without playing the navigation sample.
    static final String TICK_FILE = "nav_tick.wav";
    static final String POP_FILE = "open_pop.wav";
    private static final float VOLUME = 0.5f;
    private static final String PREFS = "nav_sound", MUTED = "muted", POP_ON = "pop_on";

    private final SoundPool pool;
    private final android.content.ContentResolver resolver;
    private final android.content.SharedPreferences prefs;
    private int pop;
    private boolean popLoaded;
    private boolean popOn;

    NavSound(Context context) {
        resolver = context.getContentResolver();
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        // "muted" is the older all-or-nothing switch: it still sets both until each is chosen on its own.
        boolean wasMuted = prefs.getBoolean(MUTED, false);
        popOn = prefs.getBoolean(POP_ON, !wasMuted);
        pool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();
        pool.setOnLoadCompleteListener((p, id, status) -> {
            if (id == pop) popLoaded = status == 0;
        });
        pop = load(context, POP_FILE, R.raw.open_pop);
    }

    private int load(Context context, String name, int fallback) {
        File dir = Config.dir(context);
        File custom = dir == null ? null : new File(dir, name);
        return custom != null && custom.isFile() && custom.length() > 44
                ? pool.load(custom.getPath(), 1) : pool.load(context, fallback, 1);
    }

    boolean isMuted() {
        return !popOn;
    }

    void setMuted(boolean muted) {
        popOn = !muted;
        prefs.edit().putBoolean(POP_ON, popOn).remove(MUTED).apply();
    }

    void onAppOpened() {
        if (popOn && popLoaded
                && Settings.System.getInt(resolver, Settings.System.SOUND_EFFECTS_ENABLED, 0) != 0) {
            pool.play(pop, VOLUME, VOLUME, 2, 0, 1f);
        }
    }

    void release() {
        pool.release();
    }
}
