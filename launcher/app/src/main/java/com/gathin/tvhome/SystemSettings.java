package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

/** Adapted from CleanYourTV SystemSettings (fe9ad320, MIT).
 * Checks the actual exported activity, not just the presence of its package. */
final class SystemSettings {
    private SystemSettings() { }

    static Intent intent(Context context, String action) {
        PackageManager pm = context.getPackageManager();
        Intent standard = new Intent(action);
        ResolveInfo match = pm.resolveActivity(standard, PackageManager.MATCH_DEFAULT_ONLY);
        if (match != null && usable(context, match.activityInfo)
                && !match.activityInfo.name.contains("CTSDummy")) return standard;
        for (String name : context.getResources().getStringArray(R.array.settings_fallback_components)) {
            ComponentName component = ComponentName.unflattenFromString(name);
            if (component == null) continue;
            try {
                if (usable(context, pm.getActivityInfo(component, 0))) {
                    return new Intent().setComponent(component);
                }
            } catch (PackageManager.NameNotFoundException | SecurityException ignored) {
                // This firmware does not expose that screen. Do not guess or grant privileges.
            }
        }
        return standard; // Caller reports unavailability when no safe handler exists.
    }

    private static boolean usable(Context context, ActivityInfo info) {
        return info != null && info.exported && info.enabled && info.applicationInfo.enabled
                && (info.permission == null || context.checkSelfPermission(info.permission)
                    == PackageManager.PERMISSION_GRANTED);
    }
}
