package com.gathin.tvhome;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.lang.ref.WeakReference;

/** Optional OS-bound listener. Never started/granted by the launcher, ADB, boot receiver or foreground service. */
public final class HomeNotificationService extends NotificationListenerService {
    static final NotificationBuffer BUFFER=new NotificationBuffer();
    private static WeakReference<HomeNotificationService> current=new WeakReference<>(null);
    private static final Handler UI=new Handler(Looper.getMainLooper());
    private static Runnable observer;
    private static boolean scheduled;
    private static final Runnable DELIVER=()->{scheduled=false;if(observer!=null)observer.run();};
    private long callbacks,cpuNanos;
    static SharedPreferences prefs(Context context){return context.getSharedPreferences("notification_viewer",0);}
    static ComponentName component(Context context){return new ComponentName(context,HomeNotificationService.class);}
    static boolean hasAccess(Context context){
        try{
            if(android.os.Build.VERSION.SDK_INT>=27)return context.getSystemService(NotificationManager.class).isNotificationListenerAccessGranted(component(context));
            String allowed=android.provider.Settings.Secure.getString(context.getContentResolver(),"enabled_notification_listeners");
            return allowed!=null&&java.util.Arrays.asList(allowed.split(":" )).contains(component(context).flattenToString());
        }catch(RuntimeException unavailable){return false;}
    }
    static boolean enabled(Context context){return prefs(context).getBoolean("enabled",false)&&hasAccess(context);}
    static void setEnabled(Context context,boolean value){
        prefs(context).edit().putBoolean("enabled",value).apply();
        if(value&&hasAccess(context)){
            try{requestRebind(component(context));}catch(RuntimeException denied){prefs(context).edit().putBoolean("enabled",false).apply();BUFFER.clear();publish();}
        }else{BUFFER.clear();HomeNotificationService service=current.get();if(service!=null){try{service.requestUnbind();}catch(RuntimeException alreadyDisconnected){ }}publish();}
    }
    static void observe(Runnable value){UI.removeCallbacks(DELIVER);scheduled=false;observer=value;}
    static void publish(){if(observer!=null&&!scheduled){scheduled=true;UI.postDelayed(DELIVER,150);}}
    static void refreshFilters(){HomeNotificationService service=current.get();if(service!=null)service.refresh();}
    @Override public void onListenerConnected(){
        current=new WeakReference<>(this);
        if(!enabled(this)){BUFFER.clear();requestUnbind();return;}
        refresh();
    }
    private void refresh(){
        BUFFER.clear();
        if(!enabled(this))return;
        try{StatusBarNotification[] notices=getActiveNotifications();RankingMap ranking=getCurrentRanking();if(notices!=null)for(StatusBarNotification n:notices)capture(n,ranking);}
        catch(RuntimeException unavailable){BUFFER.clear();}
        publish();
    }
    @Override public void onNotificationPosted(StatusBarNotification notice,RankingMap ranking){
        long started=android.os.Debug.threadCpuTimeNanos();
        if(enabled(this))capture(notice,ranking);
        callbacks++;cpuNanos+=android.os.Debug.threadCpuTimeNanos()-started;publish();
    }
    private void capture(StatusBarNotification sbn,RankingMap map){
        if(sbn==null)return;
        try{
            Notification n=sbn.getNotification();Ranking rank=new Ranking();
            int importance=map!=null&&map.getRanking(sbn.getKey(),rank)?rank.getImportance():-1;
            boolean allowed=NotificationBuffer.accepts(sbn.isOngoing(),(n.flags&Notification.FLAG_GROUP_SUMMARY)!=0,
                    n.visibility==Notification.VISIBILITY_SECRET,importance,prefs(this).getBoolean("show_quiet",false));
            if(!allowed){BUFFER.remove(sbn.getKey());return;}
            CharSequence title=n.extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence text=n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
            if(text==null)text=n.extras.getCharSequence(Notification.EXTRA_TEXT);
            if(title==null&&text==null){BUFFER.remove(sbn.getKey());return;}
            BUFFER.put(new NotificationBuffer.Notice(sbn.getKey(),sbn.getPackageName(),bounded(title,120),bounded(text,320),sbn.getPostTime()));
        }catch(RuntimeException malformed){BUFFER.remove(sbn.getKey()); /* Never log notification content or exception messages. */}
    }
    private static String bounded(CharSequence value,int limit){
        if(value==null)return "";int length=Math.min(value.length(),limit);
        if(length>0&&Character.isHighSurrogate(value.charAt(length-1)))length--;
        return value.subSequence(0,length).toString();
    }
    @Override public void onNotificationRemoved(StatusBarNotification notice){if(notice!=null)BUFFER.remove(notice.getKey());publish();}
    @Override public void onListenerDisconnected(){BUFFER.clear();if(current.get()==this)current.clear();publish();}
    @Override public void onDestroy(){BUFFER.clear();if(current.get()==this)current.clear();publish();super.onDestroy();}
    @Override protected void dump(java.io.FileDescriptor fd,java.io.PrintWriter writer,String[] args){
        writer.println("Optional notification viewer: counts only; no notification content");
        writer.println("callbacks="+callbacks+" callbackCpuNanos="+cpuNanos+" retainedNotices="+BUFFER.snapshot().size());
    }
}
