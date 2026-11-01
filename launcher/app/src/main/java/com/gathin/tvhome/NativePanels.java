package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.Toast;

/** Actions observed in the user's TCL package manifests; no key injection or privileged grants. */
final class NativePanels {
    static final String HOME="com.google.android.libraries.tv.smarthome.intent.action.OPEN_SMART_HOME";
    static final String NOTIFICATIONS="android.app.action.OPEN_NOTIFICATION_HANDLER_PANEL";
    static Intent resolve(Activity activity,String action){
        String pkg=HOME.equals(action)?"com.google.android.apps.tv.dreamx":"com.google.android.apps.tv.launcherx";
        Intent intent=new Intent(action).setPackage(pkg);
        ResolveInfo resolved;
        try{resolved=activity.getPackageManager().resolveActivity(intent,PackageManager.MATCH_DEFAULT_ONLY);}
        catch(RuntimeException unavailable){return null;}
        ActivityInfo info=resolved==null?null:resolved.activityInfo;
        if(info==null||!info.exported||!info.enabled||!info.applicationInfo.enabled
                ||info.permission!=null&&activity.checkSelfPermission(info.permission)!=PackageManager.PERMISSION_GRANTED)return null;
        return intent.setComponent(new ComponentName(info.packageName,info.name));
    }
    static void open(Activity activity,String action){
        Intent intent=resolve(activity,action);
        if(intent==null){Toast.makeText(activity,R.string.native_panel_unavailable,Toast.LENGTH_LONG).show();return;}
        try{activity.startActivity(intent);}
        catch(ActivityNotFoundException|SecurityException denied){Toast.makeText(activity,R.string.native_panel_unavailable,Toast.LENGTH_LONG).show();}
    }
}
