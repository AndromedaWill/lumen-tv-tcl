package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/** Optional shortcuts for the verified TCL remote while another app is visible. */
public final class TclRemoteShortcutService extends AccessibilityService {
    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (!RemoteKeys.isVerifiedTclRemote(event)) return false;
        boolean settings = event.getKeyCode() == KeyEvent.KEYCODE_NOTIFICATION;
        String shortcut = RemoteKeys.foregroundTclShortcut(event);
        if (!settings && shortcut == null) return false;
        if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
            try {
                if (settings) {
                    startActivity(new Intent(this, QuickSettingsActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } else {
                    RemoteKeys.launch(this, shortcut);
                }
            } catch (ActivityNotFoundException | SecurityException ignored) {
                // A missing target must not stop the service.
            }
        }
        return true;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }
}
