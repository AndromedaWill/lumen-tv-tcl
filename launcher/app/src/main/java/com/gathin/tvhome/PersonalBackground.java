package com.gathin.tvhome;
import android.app.Activity;
import android.content.*;
import android.os.*;
import android.view.View;
import android.widget.FrameLayout;
import java.io.File;
import java.time.*;
import java.util.*;

/** Visible-only scheduling/slideshow. Scenic owns image decode; Aerial owns the single video decoder. */
final class PersonalBackground {
    static final String PREFS="personal_background",SCHEDULE="schedule",FIXED="fixed",LEGACY="legacy";
    static final String[] PHASES={"Alba","Amanecer","Día","Atardecer","Anochecer","Noche"};
    private final Activity activity;
    private final View root;
    private final boolean motion;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable due=this::update;
    private final Random random=new Random();
    private boolean active;
    private String displayed="";
    private AerialBackdrop video;
    private AerialBackdrop retiring;
    private boolean usingLegacy;
    private final BroadcastReceiver clock=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){update();}};
    PersonalBackground(Activity a,View root,boolean motion){activity=a;this.root=root;this.motion=motion;}
    static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,0);}
    static boolean legacy(Context c){return LEGACY.equals(BackgroundStore.load(c).mode);}
    static File imported(Context c,int phase){return new File(c.getFilesDir(),"personal-"+phase+".image");}
    void start(){
        if(active)return;active=true;IntentFilter f=new IntentFilter();f.addAction(Intent.ACTION_TIME_CHANGED);f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        if(Build.VERSION.SDK_INT>=33)activity.registerReceiver(clock,f,Context.RECEIVER_NOT_EXPORTED);else activity.registerReceiver(clock,f);
        update();if(video!=null)video.resume();
    }
    void stop(){handler.removeCallbacks(due);if(active){active=false;activity.unregisterReceiver(clock);}if(video!=null)video.pause();if(!usingLegacy&&ScenicBackdropDrawable.cancelPending(root))displayed="";ScenicBackdropDrawable.freeze(root);}
    void hidden(){if(video!=null)video.hidden();if(retiring!=null)retiring.hidden();}
    void close(){stop();if(video!=null){video.close();video=null;}finishRetiring();}
    private void finishRetiring(){if(retiring!=null){retiring.close();retiring=null;}root.setTag(R.id.background_video_visible,video!=null);}
    void setGridFocused(boolean focused,boolean animate){if(video!=null)video.setGridFocused(focused,animate);}
    void beforeAppLaunch(Runnable launch){if(video!=null)video.beforeAppLaunch(launch);else launch.run();}
    void update(){
        if(!active)return;handler.removeCallbacks(due);BackgroundStore s=BackgroundStore.load(activity);usingLegacy=LEGACY.equals(s.mode);if(usingLegacy)return;
        BackgroundStore.Item selected=s.find(s.fixed);long delay=0;
        if(BackgroundStore.SCHEDULE.equals(s.mode)){
            List<BackgroundStore.Item> images=s.images(true);
            try{
                ZonedDateTime now=ZonedDateTime.now();selected=images.get(BackgroundPolicy.scheduledIndex(BackgroundStore.starts(images),now.getHour()*60+now.getMinute()));
                delay=Long.MAX_VALUE;
                for(BackgroundStore.Item x:images){ZonedDateTime next=now.toLocalDate().atTime(LocalTime.of(x.minute/60,x.minute%60)).atZone(now.getZone());if(!next.isAfter(now))next=next.plusDays(1);delay=Math.min(delay,Duration.between(now,next).toMillis());}
            }catch(IllegalArgumentException invalid){selected=images.isEmpty()?selected:images.get(0);}
        }else if(BackgroundStore.SLIDESHOW.equals(s.mode)){
            List<BackgroundStore.Item> images=s.images(false);android.content.SharedPreferences state=BackgroundStore.prefs(activity);
            String id=state.getString("slide_id","");int index=-1;for(int i=0;i<images.size();i++)if(images.get(i).id.equals(id))index=i;
            long now=System.currentTimeMillis(),at=state.getLong("slide_changed",0),interval=s.intervalSeconds*1000L;
            if(images.size()==1)selected=images.get(0);
            else if(!images.isEmpty()){
                if(index<0||at>now||now-at>=interval){index=BackgroundPolicy.nextIndex(index,images.size(),s.random,random);at=now;state.edit().putString("slide_id",images.get(index).id).putLong("slide_changed",at).apply();}
                selected=images.get(index);delay=Math.max(100,interval-(now-at));
            }
        }
        if(delay>0&&delay!=Long.MAX_VALUE)handler.postDelayed(due,Math.max(100,delay));
        if(selected==null)return;File file=BackgroundStore.file(activity,selected);
        String key=selected.id+":"+selected.source+":"+(file==null?0:file.lastModified());
        if(key.equals(displayed))return;displayed=key;
        if(video!=null){
            video.pause();
            if(video.hasStill()){if(retiring!=null)retiring.close();retiring=video;}
            else video.close();
            video=null;
        }
        if(selected.video){
            ScenicBackdropDrawable.cancelPending(root);
            if(root instanceof FrameLayout){
                video=AerialBackdrop.attachFile(activity,(FrameLayout)root,0,file);
                if(video!=null){
                    AerialBackdrop selectedVideo=video;
                    video.setGridFocused(Boolean.TRUE.equals(root.getTag(R.id.grid)),false);
                    video.whenReady(()->{if(video==selectedVideo)finishRetiring();});video.resume();
                }
            }
        }else{
            Context app=activity.getApplicationContext();BackgroundStore.Item image=selected;
            ScenicBackdropDrawable.load(activity,root,()->BackgroundStore.open(app,image),false,motion&&s.transitions&&retiring==null,this::finishRetiring);
        }
    }
}
