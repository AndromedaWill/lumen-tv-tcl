package com.gathin.tvhome;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

/** One source for Home and Library: actual installed activity/application icon at high density. */
final class InstalledArtwork {
    static Drawable icon(Context context,ResolveInfo app){
        PackageManager pm=context.getPackageManager();
        try{
            int id=app.activityInfo.icon!=0?app.activityInfo.icon:app.activityInfo.applicationInfo.icon;
            if(id!=0){
                Resources res=pm.getResourcesForApplication(app.activityInfo.applicationInfo);
                return res.getDrawableForDensity(id,Math.max(DisplayMetrics.DENSITY_XXXHIGH,
                        context.getResources().getDisplayMetrics().densityDpi),null);
            }
        }catch(PackageManager.NameNotFoundException|RuntimeException ignored){ }
        try{return app.loadIcon(pm);}catch(RuntimeException unavailable){return pm.getDefaultActivityIcon();}
    }
}
