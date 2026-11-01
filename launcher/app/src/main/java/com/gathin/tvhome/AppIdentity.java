package com.gathin.tvhome;

import java.util.Locale;

/** Explicit brand selection. A renamed Xuper always wins over an inherited Magis package name. */
final class AppIdentity {
    static String brand(String pkg,String label){
        String name=(label==null?"":label).toLowerCase(Locale.ROOT);
        String id=(pkg==null?"":pkg).toLowerCase(Locale.ROOT);
        if(name.contains("xuper")||id.contains("xuper"))return "xuper";
        if(name.contains("magis")||id.contains("magis"))return "installed";
        if(id.equals("com.google.android.youtube.tv")||id.equals("com.google.android.youtube"))return "youtube";
        if(id.equals("com.netflix.ninja")||id.equals("com.netflix.mediaclient"))return "netflix";
        if(id.startsWith("com.spotify."))return "spotify";
        if(id.startsWith("com.plexapp."))return "plex";
        if(id.startsWith("tv.twitch."))return "twitch";
        if(id.equals("org.xbmc.kodi"))return "kodi";
        if(id.startsWith("org.jellyfin."))return "jellyfin";
        if(id.equals("com.apple.atve.androidtv.appletv")||id.equals("com.apple.atve.sony.appletv"))return "apple-installed";
        return "installed";
    }
}
