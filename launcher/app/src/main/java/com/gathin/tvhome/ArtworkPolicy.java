package com.gathin.tvhome;

import java.util.Locale;

/** Prefer the installed app's icon when its TV banner is often an older wordmark. */
final class ArtworkPolicy {
    static boolean preferIcon(String pkg, String label) {
        return pkg.startsWith("com.google.")
                || label.toLowerCase(Locale.ROOT).contains("xuper");
    }
}
