package com.gathin.tvhome;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Cached grid veil. Only its composition alpha changes during navigation. */
final class GridBackdropView extends View {
    private Bitmap image;
    private final Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Rect destination=new Rect();
    private int generation;
    GridBackdropView(Context context){
        super(context);setWillNotDraw(false);setAlpha(0);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        forceHasOverlappingRendering(false);
    }
    void image(Bitmap bitmap){generation++;image=bitmap;invalidate();}
    void blur(Bitmap source){
        if(source==null)return;
        int ticket=++generation;
        new Thread(()->{
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            Bitmap result=null;
            try{
                result=Bitmap.createBitmap(160,90,Bitmap.Config.ARGB_8888);
                new Canvas(result).drawBitmap(source,null,new Rect(0,0,160,90),new Paint(Paint.FILTER_BITMAP_FLAG));
                ScenicBackdropDrawable.blur(result);
                new Canvas(result).drawColor(0x42000000);result.prepareToDraw();
            }catch(RuntimeException|OutOfMemoryError failure){if(result!=null)result.recycle();return;}
            Bitmap ready=result;
            post(()->{if(ticket!=generation){ready.recycle();return;}image=ready;invalidate();});
        },"home-video-grid-blur").start();
    }
    void focused(boolean focused,boolean motion){
        animate().cancel();
        if(motion)animate().alpha(focused?1:0).setDuration(280).start();else setAlpha(focused?1:0);
    }
    void freeze(){generation++;animate().cancel();}
    void close(){freeze();image=null;}
    @Override protected void onDraw(Canvas canvas){
        if(image==null)return;
        destination.set(0,0,getWidth(),getHeight());canvas.drawBitmap(image,null,destination,paint);
    }
}
