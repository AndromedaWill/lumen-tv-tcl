package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Offline background plus Lumen-style lower veil. Decode, crop, tint and blur once off UI. */
final class ScenicBackdropDrawable extends Drawable {
    private final Bitmap photo, frost, neutralFrost;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF();
    private boolean lower;
    private float blend;
    private ValueAnimator transition;
    private Drawable previous;
    private float appear=1;
    private ValueAnimator crossfade;
    interface Source { java.io.InputStream open() throws java.io.IOException; }
    private ScenicBackdropDrawable(Bitmap photo, Bitmap frost,Bitmap neutralFrost) { this.photo=photo; this.frost=frost;this.neutralFrost=neutralFrost; }

    private static final class LoadTicket {
        volatile boolean cancelled;
        final java.lang.ref.WeakReference<Activity> owner;
        final java.lang.ref.WeakReference<View> view;
        LoadTicket(Activity owner, View view) {
            this.owner=new java.lang.ref.WeakReference<>(owner);
            this.view=new java.lang.ref.WeakReference<>(view);
        }
    }
    static void load(Activity activity, View target) {
        android.content.res.Resources resources=activity.getResources();
        load(activity,target,()->resources.openRawResource(R.raw.hercules),true,false);
    }
    static void load(Activity activity, View target, Source input, boolean legacyTint, boolean animate) {
        load(activity,target,input,legacyTint,animate,null);
    }
    static void load(Activity activity, View target, Source input, boolean legacyTint, boolean animate,Runnable applied) {
        Object previous=target.getTag(R.id.backdrop_load_job);
        if(previous instanceof LoadTicket) ((LoadTicket)previous).cancelled=true;
        LoadTicket ticket=new LoadTicket(activity,target);
        target.setTag(R.id.backdrop_load_job,ticket);
        android.content.res.Resources resources=activity.getResources();
        int width=target.getWidth()>0?target.getWidth():resources.getDisplayMetrics().widthPixels;
        int height=target.getHeight()>0?target.getHeight():resources.getDisplayMetrics().heightPixels;
        new Thread(() -> prepare(resources,width,height,ticket,input,legacyTint,animate,applied),"home-photo-prepare").start();
    }
    private static void prepare(android.content.res.Resources resources, int width, int height, LoadTicket ticket, Source input, boolean legacyTint, boolean animate,Runnable applied) {
        Bitmap source=null,photo=null,frost=null,neutral=null;
        try {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            long started=android.os.SystemClock.elapsedRealtimeNanos();
            long cpuStarted=android.os.Debug.threadCpuTimeNanos();
            BitmapFactory.Options bounds=new BitmapFactory.Options();
            bounds.inJustDecodeBounds=true;
            bounds.inScaled=false;
            try(java.io.InputStream stream=input.open()){BitmapFactory.decodeStream(stream,null,bounds);}
            if(ticket.cancelled)return;
            BackdropSize size=BackdropSize.forViewport(width,height,bounds.outWidth,bounds.outHeight);
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inSampleSize=size.sample;
            options.inScaled=false;
            options.inPreferredConfig=Bitmap.Config.ARGB_8888;
            long decodeStarted=android.os.SystemClock.elapsedRealtimeNanos();
            try(java.io.InputStream stream=input.open()){source=BitmapFactory.decodeStream(stream,null,options);}
            long decoded=android.os.SystemClock.elapsedRealtimeNanos();
            if(source==null) throw new IllegalStateException("Background decode returned null");
            if(ticket.cancelled)return;
            long sourceBytes=source.getAllocationByteCount();
            photo=Bitmap.createBitmap(size.width,size.height,Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(photo);
            Paint brush=new Paint(Paint.FILTER_BITMAP_FLAG);
            if(legacyTint)brush.setColorFilter(new PorterDuffColorFilter(0x73000000,PorterDuff.Mode.SRC_ATOP));
            float scale=Math.max((float)size.width/source.getWidth(),(float)size.height/source.getHeight());
            float w=source.getWidth()*scale,h=source.getHeight()*scale;
            canvas.drawBitmap(source,null,new RectF((size.width-w)/2,(size.height-h)/2,
                    (size.width+w)/2,(size.height+h)/2),brush);
            source.recycle(); source=null;
            brush.setColorFilter(null);
            brush.setShader(new LinearGradient(0,0,0,size.height,new int[]{0x59000000,0,0,0x99000000},
                    new float[]{0,.25f,.55f,1},Shader.TileMode.CLAMP));
            canvas.drawRect(0,0,size.width,size.height,brush);
            if(ticket.cancelled)return;
            frost=Bitmap.createBitmap(160,90,Bitmap.Config.ARGB_8888);
            Canvas blurred=new Canvas(frost);
            blurred.drawBitmap(photo,null,new RectF(0,0,160,90),new Paint(Paint.FILTER_BITMAP_FLAG));
            long blurStarted=android.os.SystemClock.elapsedRealtimeNanos();
            blur(frost);
            blurred.drawColor(0x42000000);
            long blurredAt=android.os.SystemClock.elapsedRealtimeNanos();
            // Neutral glass samples only the tiny frost, preserving the original photograph's colour.
            neutral=Bitmap.createBitmap(160,90,Bitmap.Config.ARGB_8888);
            ColorMatrix grey=new ColorMatrix();grey.setSaturation(0);ColorMatrix shade=new ColorMatrix();shade.setScale(.72f,.72f,.72f,1);grey.postConcat(shade);
            Paint neutralBrush=new Paint(Paint.FILTER_BITMAP_FLAG);neutralBrush.setColorFilter(new ColorMatrixColorFilter(grey));
            new Canvas(neutral).drawBitmap(frost,0,0,neutralBrush);
            photo.prepareToDraw();frost.prepareToDraw();neutral.prepareToDraw();
            long prepared=android.os.SystemClock.elapsedRealtimeNanos();
            long workerCpu=android.os.Debug.threadCpuTimeNanos()-cpuStarted;
            if(ticket.cancelled)return;
            // Exactly one record per completed load, on the worker; never in draw/key/frame callbacks.
            android.util.Log.i("TCLHomePerf", "background source="+bounds.outWidth+"x"+bounds.outHeight
                    +" photo="+size.width+"x"+size.height+" sample="+size.sample
                    +" decodeMs="+((decoded-decodeStarted)/1e6)+" blurMs="+((blurredAt-blurStarted)/1e6)+" bitmapPrepMs="+((prepared-blurredAt)/1e6)
                    +" prepareMs="+((prepared-started)/1e6)+" workerCpuMs="+(workerCpu/1e6)
                    +" sourceBitmapBytes="+sourceBytes
                    +" backgroundCpuBitmapBytes="+(photo.getAllocationByteCount()+frost.getAllocationByteCount()+neutral.getAllocationByteCount())
                    +" blurScratchArrayBytes="+(160*90*4*2));
            Bitmap readyPhoto=photo,readyFrost=frost,readyNeutral=neutral;
            boolean queued=new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                Activity owner=ticket.owner.get(); View view=ticket.view.get();
                if(ticket.cancelled || owner==null || owner.isFinishing() || owner.isDestroyed()
                        || view==null || view.getTag(R.id.backdrop_load_job)!=ticket) {
                    readyPhoto.recycle();readyFrost.recycle();readyNeutral.recycle();return;
                }
                view.setTag(R.id.backdrop_load_job,null);
                ScenicBackdropDrawable drawable=new ScenicBackdropDrawable(readyPhoto,readyFrost,readyNeutral);
                Drawable old=view.getBackground();
                view.setBackground(drawable);
                drawable.setLower(Boolean.TRUE.equals(view.getTag(R.id.grid)),false);
                if(animate && owner.hasWindowFocus())drawable.appearFrom(old);
                if(applied!=null)applied.run();
                if(!Boolean.TRUE.equals(view.getTag(R.id.background_video_visible)))BackdropStartupGate.ready(view);
            });
            if(queued){photo=null;frost=null;neutral=null;} // Ownership moves to UI; never recycle published bitmaps.
        } catch(java.io.IOException | RuntimeException | OutOfMemoryError failure) {
            android.util.Log.w("TCLHomePerf","background prepareFailed="+failure.getClass().getSimpleName());
            new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{
                View view=ticket.view.get();
                if(view!=null&&view.getTag(R.id.backdrop_load_job)==ticket){
                    view.setTag(R.id.backdrop_load_job,null);
                    if(!Boolean.TRUE.equals(view.getTag(R.id.background_video_visible)))BackdropStartupGate.ready(view);
                }
            });
            // Retain an existing selected image; a cold failure uses the neutral screen, never another scene.
        } finally {
            if(source!=null)source.recycle();
            if(photo!=null)photo.recycle();
            if(frost!=null)frost.recycle();
            if(neutral!=null)neutral.recycle();
        }
    }
    static void dispose(View root) {
        if(root==null)return;
        Object ticket=root.getTag(R.id.backdrop_load_job);
        if(ticket instanceof LoadTicket)((LoadTicket)ticket).cancelled=true;
        root.setTag(R.id.backdrop_load_job,null);
        stop(root);
        if(root.getBackground() instanceof ScenicBackdropDrawable)root.setBackground(null);
        // Displayed bitmaps are released by GC/render-thread ownership, not recycled while in use.
    }
    static boolean cancelPending(View root){
        Object value=root.getTag(R.id.backdrop_load_job);
        if(!(value instanceof LoadTicket))return false;
        ((LoadTicket)value).cancelled=true;root.setTag(R.id.backdrop_load_job,null);return true;
    }
    /** Separable box blur, two passes over only 160x90 pixels. Never runs during drawing. */
    static void blur(Bitmap image) {
        int w=image.getWidth(),h=image.getHeight();
        int[] a=new int[w*h],b=new int[w*h];
        image.getPixels(a,0,w,0,0,w,h);
        int radius=2, count=radius*2+1;
        for (int pass=0;pass<2;pass++) {
            for (int y=0;y<h;y++) {
                int r=0,g=0,blue=0;
                for(int k=-radius;k<=radius;k++){int v=a[y*w+Math.max(0,Math.min(w-1,k))];r+=(v>>16)&255;g+=(v>>8)&255;blue+=v&255;}
                for(int x=0;x<w;x++) {
                    b[y*w+x]=0xFF000000|((r/count)<<16)|((g/count)<<8)|(blue/count);
                    int old=a[y*w+Math.max(0,x-radius)],next=a[y*w+Math.min(w-1,x+radius+1)];
                    r+=((next>>16)&255)-((old>>16)&255);g+=((next>>8)&255)-((old>>8)&255);blue+=(next&255)-(old&255);
                }
            }
            for (int x=0;x<w;x++) {
                int r=0,g=0,blue=0;
                for(int k=-radius;k<=radius;k++){int v=b[Math.max(0,Math.min(h-1,k))*w+x];r+=(v>>16)&255;g+=(v>>8)&255;blue+=v&255;}
                for(int y=0;y<h;y++) {
                    a[y*w+x]=0xFF000000|((r/count)<<16)|((g/count)<<8)|(blue/count);
                    int old=b[Math.max(0,y-radius)*w+x],next=b[Math.min(h-1,y+radius+1)*w+x];
                    r+=((next>>16)&255)-((old>>16)&255);g+=((next>>8)&255)-((old>>8)&255);blue+=(next&255)-(old&255);
                }
            }
        }
        image.setPixels(a,0,w,0,0,w,h);
    }
    static void setLower(View root,boolean lower,boolean animate) {
        if(root==null)return;
        root.setTag(R.id.grid,lower);
        if(root.getBackground() instanceof ScenicBackdropDrawable)
            ((ScenicBackdropDrawable)root.getBackground()).setLower(lower,animate&&!Boolean.TRUE.equals(root.getTag(R.id.background_video_visible)));
    }
    private void setLower(boolean value,boolean animate) {
        if(lower==value && (transition!=null || blend==(value?1:0)))return;
        lower=value;
        if(transition!=null){transition.cancel();transition=null;}
        float end=value?1:0;
        if(!animate){blend=end;invalidateSelf();return;}
        transition=ValueAnimator.ofFloat(blend,end);
        // Let initial RecyclerView row attachment/layout finish before starting the veil.
        // Total motion remains 280 ms; fewer invalidations compete with the first Down frame.
        transition.setStartDelay(value?100:0);
        transition.setDuration(value?180:280);
        transition.addUpdateListener(a->{blend=(float)a.getAnimatedValue();invalidateSelf();});
        transition.start();
    }
    static void stop(View root) {
        if(root!=null && root.getBackground() instanceof ScenicBackdropDrawable) {
            ScenicBackdropDrawable d=(ScenicBackdropDrawable)root.getBackground();
            if(d.transition!=null){d.transition.cancel();d.transition=null;}
            d.blend=d.lower?1:0;
            d.finishCrossfade();
        }
    }
    /** Freeze the visible exit composition; settle/release crossfade resources once hidden. */
    static void freeze(View root) {
        if(root!=null && root.getBackground() instanceof ScenicBackdropDrawable){
            ScenicBackdropDrawable d=(ScenicBackdropDrawable)root.getBackground();
            if(d.transition!=null){d.transition.cancel();d.transition=null;}
            if(d.crossfade!=null){d.crossfade.cancel();d.crossfade=null;}
        }
    }
    private void appearFrom(Drawable old) {
        if(!(old instanceof ScenicBackdropDrawable))return;
        ScenicBackdropDrawable prior=(ScenicBackdropDrawable)old;
        prior.finishCrossfade();
        if(prior.transition!=null){prior.transition.cancel();prior.transition=null;}
        previous=old;appear=0;
        crossfade=ValueAnimator.ofFloat(0,1);crossfade.setDuration(450);
        crossfade.addUpdateListener(a->{appear=(float)a.getAnimatedValue();if(appear>=1)previous=null;invalidateSelf();});
        crossfade.start();
    }
    private void finishCrossfade(){if(crossfade!=null){crossfade.cancel();crossfade=null;}previous=null;appear=1;}
    void drawFrost(Canvas canvas,int opacity) { paint.setAlpha(opacity);canvas.drawBitmap(neutralFrost,null,destination,paint);paint.setAlpha(255); }
    @Override public void draw(Canvas canvas) {
        destination.set(getBounds());
        if(previous!=null && appear<1){
            previous.setBounds(getBounds());previous.draw(canvas);
            int layer=canvas.saveLayerAlpha(destination,Math.round(255*appear));
            drawCurrent(canvas);canvas.restoreToCount(layer);return;
        }
        drawCurrent(canvas);
    }
    private void drawCurrent(Canvas canvas){
        paint.setAlpha(255);
        if(blend>=1){canvas.drawBitmap(frost,null,destination,paint);return;}
        canvas.drawBitmap(photo,null,destination,paint);
        if(blend>0){paint.setAlpha(Math.round(255*blend));canvas.drawBitmap(frost,null,destination,paint);paint.setAlpha(255);}
    }
    @Override public void setAlpha(int a) { }
    @Override public void setColorFilter(ColorFilter f) { paint.setColorFilter(f);invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
