package com.gathin.tvhome;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.widget.FrameLayout;
import android.media.MediaMetadataRetriever;
import java.io.File;

/** One bounded snapshot of our own video surface; never captures another app or polls in the background. */
final class VideoStillCover extends View {
    private final SurfaceView video;
    private final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
    private final android.graphics.RectF dest=new android.graphics.RectF();
    private Bitmap still;
    private boolean copying,closed;
    private int attempts;
    private int posterGeneration;
    private Runnable departing;
    private long departureStarted;
    private final Runnable deadline=this::finishDeparture;
    private final Runnable retry=this::capture;
    VideoStillCover(Context c,FrameLayout root,SurfaceView video){super(c);this.video=video;setWillNotDraw(false);}
    /** A poster from this video only. An unrelated photo must never replace the video on pause. */
    void poster(File file,int position){
        final int generation=++posterGeneration;
        new Thread(()->{
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            Bitmap frame=null;MediaMetadataRetriever retriever=new MediaMetadataRetriever();
            try{retriever.setDataSource(file.getPath());frame=Build.VERSION.SDK_INT>=27
                    ?retriever.getScaledFrameAtTime(Math.max(0,position)*1000L,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,960,540)
                    :retriever.getFrameAtTime(Math.max(0,position)*1000L);}
            catch(Exception|OutOfMemoryError unavailable){ }
            finally{try{retriever.release();}catch(Exception ignored){ }}
            Bitmap ready=frame;post(()->{if(ready==null)return;if(closed||generation!=posterGeneration||still!=null)ready.recycle();else{still=ready;invalidate();}});
        },"home-video-poster").start();
    }
    void begin(){attempts=0;capture();}
    /** Capture while the surface still owns a frame, before Android pauses/releases it. Bounded wait. */
    void beforeAppLaunch(Runnable launch){
        if(closed||departing!=null)return;
        departing=launch;departureStarted=android.os.SystemClock.elapsedRealtime();postDelayed(deadline,96);attempts=0;capture();
    }
    private void finishDeparture(){removeCallbacks(deadline);Runnable launch=departing;departing=null;if(launch!=null&&!closed){android.util.Log.i("TCLHomePerf","videoFreezeMs="+(android.os.SystemClock.elapsedRealtime()-departureStarted)+" sameVideoStill="+(still!=null));launch.run();}}
    private void capture(){
        if(closed||copying||!video.isShown()||!video.getHolder().getSurface().isValid()||attempts>=2)return;
        copying=true;attempts++;
        int w=Math.min(1280,Math.max(1,video.getWidth())),h=Math.max(1,Math.round(w*(float)Math.max(1,video.getHeight())/Math.max(1,video.getWidth())));
        new Thread(()->{
            Bitmap bitmap;
            try{bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);}catch(OutOfMemoryError unavailable){post(()->copying=false);return;}
            post(()->{
                if(closed||!video.isShown()){copying=false;bitmap.recycle();return;}
                try{PixelCopy.request(video,bitmap,result->{
                    copying=false;
                    if(!closed&&result==PixelCopy.SUCCESS){still=bitmap;invalidate();}
                    else{bitmap.recycle();if(!closed&&video.isShown()&&attempts<2)postDelayed(retry,120);}
                    finishDeparture();
                },new Handler(Looper.getMainLooper()));}
                catch(RuntimeException unavailable){copying=false;bitmap.recycle();}
            });
        },"home-video-still").start();
    }
    void showStill(){removeCallbacks(retry);animate().cancel();setAlpha(1);invalidate();}
    void close(){closed=true;departing=null;posterGeneration++;removeCallbacks(retry);removeCallbacks(deadline);still=null;}
    @Override protected void onDraw(Canvas canvas){
        if(still!=null){dest.set(0,0,getWidth(),getHeight());canvas.drawBitmap(still,null,dest,paint);return;}
        // Transparent until the first video/poster frame: retain the already visible composition,
        // never substitute an arbitrary collection image or fade the surface through black.
    }
}
