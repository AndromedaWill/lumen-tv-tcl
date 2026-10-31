package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

/** Persists identity, not a View or only a row index; survives recreation and process death. */
final class HomeFocusStore {
    private final SharedPreferences prefs;

    HomeFocusStore(Context context) {
        prefs = context.getSharedPreferences("home_focus", Context.MODE_PRIVATE);
    }

    String key() { return prefs.getString("key", null); }
    int position() { return Math.max(0, prefs.getInt("position", 0)); }

    void save(String key, int position) {
        if (key == null || key.isEmpty()) return;
        prefs.edit().putString("key", key).putInt("position", Math.max(0, position)).apply();
    }
}
