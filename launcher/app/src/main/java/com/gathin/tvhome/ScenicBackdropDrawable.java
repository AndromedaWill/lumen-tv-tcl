package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Offline background plus Lumen-style lower veil. Decode, crop, tint and blur once off UI. */
final class ScenicBackdropDrawable extends Drawable {
    private final Bitmap photo, frost;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF();
    private boolean lower;
    private float blend;
    private ValueAnimator transition;
    private ScenicBackdropDrawable(Bitmap photo, Bitmap frost) { this.photo=photo; this.frost=frost; }

    static void load(Activity activity, View target) {
        new Thread(() -> {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize=2;
            Bitmap source = BitmapFactory.decodeResource(activity.getResources(), R.drawable.hercules, options);
            if (source == null) return;
            Bitmap photo = Bitmap.createBitmap(960,540,Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(photo);
            Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);
            p.setColorFilter(new PorterDuffColorFilter(0x73000000,PorterDuff.Mode.SRC_ATOP));
            float scale=Math.max(960f/source.getWidth(),540f/source.getHeight());
            float w=source.getWidth()*scale,h=source.getHeight()*scale;
            c.drawBitmap(source,null,new RectF((960-w)/2,(540-h)/2,(960+w)/2,(540+h)/2),p);
            source.recycle();
            p.setColorFilter(null);
            p.setShader(new LinearGradient(0,0,0,540,new int[]{0x59000000,0,0,0x99000000},
                    new float[]{0,.25f,.55f,1},Shader.TileMode.CLAMP));
            c.drawRect(0,0,960,540,p);
            Bitmap frost=Bitmap.createBitmap(160,90,Bitmap.Config.ARGB_8888);
            Canvas f=new Canvas(frost);
            f.drawBitmap(photo,null,new RectF(0,0,160,90),new Paint(Paint.FILTER_BITMAP_FLAG));
            blur(frost);
            f.drawColor(0x42000000); // Lumen lower-veil tint .26
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) { photo.recycle(); frost.recycle(); return; }
                ScenicBackdropDrawable d=new ScenicBackdropDrawable(photo,frost);
                target.setBackground(d);
                d.setLower(Boolean.TRUE.equals(target.getTag(R.id.grid)),false);
            });
        },"home-photo-prepare").start();
    }
    /** Separable box blur, two passes over only 160x90 pixels. Never runs during drawing. */
    private static void blur(Bitmap image) {
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
            ((ScenicBackdropDrawable)root.getBackground()).setLower(lower,animate);
    }
    private void setLower(boolean value,boolean animate) {
        if(lower==value && (transition!=null || blend==(value?1:0)))return;
        lower=value;
        if(transition!=null){transition.cancel();transition=null;}
        float end=value?1:0;
        if(!animate){blend=end;invalidateSelf();return;}
        transition=ValueAnimator.ofFloat(blend,end);
        transition.setDuration(280);
        transition.addUpdateListener(a->{blend=(float)a.getAnimatedValue();invalidateSelf();});
        transition.start();
    }
    static void stop(View root) {
        if(root!=null && root.getBackground() instanceof ScenicBackdropDrawable) {
            ScenicBackdropDrawable d=(ScenicBackdropDrawable)root.getBackground();
            if(d.transition!=null){d.transition.cancel();d.transition=null;}
            d.blend=d.lower?1:0;
        }
    }
    void drawFrost(Canvas canvas) { paint.setAlpha(255);canvas.drawBitmap(frost,null,destination,paint); }
    @Override public void draw(Canvas canvas) {
        destination.set(getBounds());
        paint.setAlpha(255);
        if(blend>=1){canvas.drawBitmap(frost,null,destination,paint);return;}
        canvas.drawBitmap(photo,null,destination,paint);
        if(blend>0){paint.setAlpha(Math.round(255*blend));canvas.drawBitmap(frost,null,destination,paint);paint.setAlpha(255);}
    }
    @Override public void setAlpha(int a) { }
    @Override public void setColorFilter(ColorFilter f) { paint.setColorFilter(f);invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
