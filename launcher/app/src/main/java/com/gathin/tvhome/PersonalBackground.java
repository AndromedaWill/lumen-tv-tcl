package com.gathin.tvhome;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.view.View;
import java.io.File;
import java.time.LocalTime;

/** Chooses a still source; decoding/blur remain owned by ScenicBackdropDrawable. */
final class PersonalBackground {
    static final String PREFS="personal_background", SCHEDULE="schedule", FIXED="fixed", LEGACY="legacy";
    static final String[] PHASES={"Alba","Amanecer","Día","Atardecer","Anochecer","Noche"};
    private final Activity activity;
    private final View root;
    private String displayed="";
    private boolean active;
    private final boolean motion;
    private final BroadcastReceiver clock=new BroadcastReceiver(){
        @Override public void onReceive(Context context,Intent intent){update();}
    };
    PersonalBackground(Activity activity,View root,boolean motion){this.activity=activity;this.root=root;this.motion=motion;}
    static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,0);}
    static boolean legacy(Context c){return LEGACY.equals(prefs(c).getString("mode",SCHEDULE));}
    static File imported(Context c,int phase){return new File(c.getFilesDir(),"personal-"+phase+".image");}
    void start(){
        if(active)return;active=true;
        IntentFilter f=new IntentFilter();f.addAction(Intent.ACTION_TIME_CHANGED);f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        if(android.os.Build.VERSION.SDK_INT>=33)activity.registerReceiver(clock,f,Context.RECEIVER_NOT_EXPORTED);
        else activity.registerReceiver(clock,f);
        update();
    }
    void stop(){if(active){active=false;activity.unregisterReceiver(clock);}ScenicBackdropDrawable.stop(root);}
    void update(){
        if(!active)return;
        SharedPreferences p=prefs(activity);String mode=p.getString("mode",SCHEDULE);
        if(LEGACY.equals(mode))return;
        LocalTime now=LocalTime.now();int phase=FIXED.equals(mode)?p.getInt("phase",2):DayPhase.atMinute(now.getHour()*60+now.getMinute());
        phase=Math.max(0,Math.min(5,phase));File file=imported(activity,phase);
        String key=phase+":"+(file.isFile()?file.lastModified():"bundled");
        if(key.equals(displayed))return;
        displayed=key;
        ScenicBackdropDrawable.Source source;
        if(file.isFile())source=()->new java.io.FileInputStream(file);
        else {String asset="personal/"+(phase+1)+".jpg";android.content.res.Resources resources=activity.getResources();android.content.res.AssetManager assets=resources.getAssets();source=()->{
            try{return assets.open(asset);}catch(java.io.IOException missing){return resources.openRawResource(R.raw.hercules);}
        };}
        ScenicBackdropDrawable.load(activity,root,source,false,motion&&!p.getBoolean("no_transition",false));
    }
}
