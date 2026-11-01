package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Same native controls for the optional settings_key_panel entry; no accessibility is enabled here. */
public final class QuickSettingsActivity extends Activity {
    private ControlCenterDialog panel;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        panel = new ControlCenterDialog(this,
                () -> openHome(MainActivity.ACTION_INPUTS),
                () -> openHome(MainActivity.ACTION_LAUNCHER_SETTINGS),
                () -> openHome(MainActivity.ACTION_NOTIFICATIONS), false);
        panel.setOnDismissListener(d -> finish());
        panel.show();
    }
    private void openHome(String action) {
        startActivity(new Intent(this, MainActivity.class).setAction(action));
    }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); finish(); }
    @Override protected void onPause() {
        super.onPause();
        if (panel != null) panel.dismiss();
        finish();
    }
}
