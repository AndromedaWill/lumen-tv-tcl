package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

/** TV firmwares may resolve SAF to a no-op stub. Only offer a real accessible picker. */
final class ImagePickerIntent {
    static Intent resolve(Context context){
        PackageManager pm=context.getPackageManager();
        for(String action:new String[]{Intent.ACTION_OPEN_DOCUMENT,Intent.ACTION_GET_CONTENT}){
            Intent candidate=new Intent(action).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*");
            candidate.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/jpeg","image/png","image/webp"});
            for(ResolveInfo info:pm.queryIntentActivities(candidate,0)){
                if(info.activityInfo==null||!info.activityInfo.exported||info.activityInfo.packageName.equals(context.getPackageName())
                        ||info.activityInfo.packageName.equals("com.android.tv.frameworkpackagestubs"))continue;
                String permission=info.activityInfo.permission;
                if(permission!=null&&context.checkSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED)continue;
                return candidate.setClassName(info.activityInfo.packageName,info.activityInfo.name);
            }
        }
        return null;
    }
}
