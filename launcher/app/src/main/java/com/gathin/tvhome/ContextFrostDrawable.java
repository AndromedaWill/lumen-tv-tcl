package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.*;
import android.os.*;
import android.view.*;
import java.lang.ref.WeakReference;

/** Fixed, opaque frost of our own backdrop/window. Tiny preparation once, never live GPU blur. */
final class ContextFrostDrawable extends LiquidGlassDrawable implements AutoCloseable {
    private final WeakReference<View> host;
    private final Paint brush=new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RectF screen=new RectF();
    private final int[] position=new int[2];
    private final int fallback;
    private ScenicBackdropDrawable photo;
    private final Path clip=new Path();
    private final float radius;
    private Bitmap frost;
    private volatile boolean closed;
    private HandlerThread workerThread;
    private final Handler ui=new Handler(Looper.getMainLooper());
    ContextFrostDrawable(Activity activity,View host){
        super(TvDesign.radius(activity,R.dimen.ds_radius_panel),TvDesign.color(activity,R.color.ds_glass_fill));
        this.host=new WeakReference<>(host);fallback=TvDesign.color(activity,R.color.ds_screen_bottom);radius=TvDesign.dp(activity,22);
        screen.set(0,0,activity.getResources().getDisplayMetrics().widthPixels,activity.getResources().getDisplayMetrics().heightPixels);
        View root=activity.findViewById(R.id.root);
        boolean video=root!=null&&Boolean.TRUE.equals(root.getTag(R.id.background_video_visible));
        photo=!video&&root!=null&&root.getBackground() instanceof ScenicBackdropDrawable?(ScenicBackdropDrawable)root.getBackground():null;
        if(photo!=null)return;
        Bitmap source=null;
        if(video){Object owner=root.getTag(R.id.video_still_owner);if(owner instanceof VideoStillCover)source=((VideoStillCover)owner).snapshot();}
        if(video&&source==null)return; // Never use an unrelated photo or copy a video hole in the window.
        workerThread=new HandlerThread("HomePanelFrost",android.os.Process.THREAD_PRIORITY_BACKGROUND);workerThread.start();Handler worker=new Handler(workerThread.getLooper());
        Bitmap frame=source;
        worker.post(()->{
            Bitmap tiny;
            try{tiny=Bitmap.createBitmap(160,90,Bitmap.Config.ARGB_8888);}catch(OutOfMemoryError unavailable){finishWorker();return;}
            if(closed){tiny.recycle();finishWorker();return;}
            if(frame!=null){
                new Canvas(tiny).drawBitmap(frame,null,new RectF(0,0,160,90),brush);safePrepare(tiny);
            }else{
                try{PixelCopy.request(activity.getWindow(),tiny,result->{
                    if(result==PixelCopy.SUCCESS&&!closed)safePrepare(tiny);else{tiny.recycle();finishWorker();}
                },worker);}catch(RuntimeException unavailable){tiny.recycle();finishWorker();}
            }
        });
    }
    private void safePrepare(Bitmap tiny){try{prepare(tiny);}catch(OutOfMemoryError|RuntimeException unavailable){if(!tiny.isRecycled())tiny.recycle();finishWorker();}}
    private void prepare(Bitmap tiny){
        if(closed){tiny.recycle();finishWorker();return;}
        ScenicBackdropDrawable.blur(tiny);
        ColorMatrix neutral=new ColorMatrix();neutral.setSaturation(0);ColorMatrix shade=new ColorMatrix();shade.setScale(.72f,.72f,.72f,1);neutral.postConcat(shade);
        Paint tint=new Paint();tint.setColorFilter(new ColorMatrixColorFilter(neutral));
        Bitmap result=Bitmap.createBitmap(160,90,Bitmap.Config.ARGB_8888);new Canvas(result).drawBitmap(tiny,0,0,tint);tiny.recycle();
        result.prepareToDraw();ui.post(()->{if(closed)result.recycle();else{frost=result;invalidateSelf();}});finishWorker();
    }
    private void finishWorker(){HandlerThread thread=workerThread;if(thread!=null)thread.quitSafely();}
    @Override protected void onBoundsChange(Rect bounds){super.onBoundsChange(bounds);clip.reset();clip.addRoundRect(new RectF(bounds),radius,radius,Path.Direction.CW);}
    @Override public void draw(Canvas canvas){
        View view=host.get();int save=canvas.save();
        canvas.clipPath(clip);
        if(view!=null&&(photo!=null||frost!=null)){
            view.getLocationOnScreen(position);canvas.translate(-position[0],-position[1]);
            if(photo!=null)photo.drawFrost(canvas,255);else canvas.drawBitmap(frost,null,screen,brush);
        }else canvas.drawColor(fallback);
        canvas.restoreToCount(save);super.draw(canvas);
    }
    @Override public void close(){closed=true;ui.removeCallbacksAndMessages(null);frost=null;photo=null;finishWorker();}
}
