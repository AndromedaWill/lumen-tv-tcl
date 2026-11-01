package com.gathin.tvhome;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;

/** Lumen's full-tile colour + centred glyph composition. No generated bitmaps or live filters. */
final class LumenArtwork extends Drawable {
    private final Drawable glyph;
    private Drawable adaptiveBackground;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Paint xPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect iconBounds=new Rect();
    private final int top,bottom;
    private final String brand;
    private final boolean cover;
    private LumenArtwork(Drawable glyph,String brand,int top,int bottom,boolean cover){
        this.glyph=glyph;this.brand=brand;this.top=top;this.bottom=bottom;this.cover=cover;
        xPaint.setColor(Color.WHITE);xPaint.setStrokeCap(Paint.Cap.ROUND);
    }
    static Drawable forApp(Context context,ResolveInfo app,Drawable installed){
        return forApp(context,app.activityInfo.packageName,String.valueOf(app.loadLabel(context.getPackageManager())),installed);
    }
    static boolean builtIn(String pkg,String label){String b=AppIdentity.brand(pkg,label);return !b.equals("installed")&&!b.equals("apple-installed");}
    static Drawable forApp(Context context,String pkg,String label,Drawable installed){
        String brand=AppIdentity.brand(pkg,label);
        int res=switch(brand){
            case "youtube"->R.drawable.app_brand_youtube;case "netflix"->R.drawable.app_brand_netflix;
            case "spotify"->R.drawable.app_brand_spotify;case "plex"->R.drawable.app_brand_plex;
            case "twitch"->R.drawable.app_brand_twitch;case "kodi"->R.drawable.app_brand_kodi;
            case "jellyfin"->R.drawable.app_brand_jellyfin;default->0;
        };
        int top=0xFF191C23,bottom=0xFF101217;
        if(brand.equals("youtube")){top=0xFFFFFFFF;bottom=0xFFE8EAF0;}
        else if(brand.equals("netflix")){top=0xFF26262A;bottom=0xFF000000;}
        else if(brand.equals("xuper")){top=0xFF21618D;bottom=0xFF29234F;}
        else if(res==0){top=bottom=background(installed);}
        Drawable logo=res==0?installed:context.getDrawable(res);
        AdaptiveIconDrawable adaptive=logo instanceof AdaptiveIconDrawable?(AdaptiveIconDrawable)logo:null;
        boolean wide=installed!=null&&installed.getIntrinsicWidth()>installed.getIntrinsicHeight()*1.3f;
        LumenArtwork tile=new LumenArtwork(adaptive==null?logo:adaptive.getForeground(),brand,top,bottom,brand.equals("apple-installed")||res==0&&wide);
        if(adaptive!=null)tile.adaptiveBackground=adaptive.getBackground();return tile;
    }
    private static int background(Drawable icon){
        if(icon instanceof BitmapDrawable){
            try{
                Bitmap b=((BitmapDrawable)icon).getBitmap();int w=b.getWidth(),h=b.getHeight();
                int[] samples={b.getPixel(w/12,h/12),b.getPixel(w-w/12-1,h/12),b.getPixel(w/12,h-h/12-1),b.getPixel(w-w/12-1,h-h/12-1)};
                for(int p:samples)if(Color.alpha(p)<240)return 0xFF191C23;
                int r=0,g=0,blue=0;for(int p:samples){r+=Color.red(p);g+=Color.green(p);blue+=Color.blue(p);}
                return Color.rgb(r/4,g/4,blue/4);
            }catch(RuntimeException unavailable){ }
        }
        return 0xFF191C23;
    }
    @Override protected void onBoundsChange(Rect b){
        paint.setShader(new LinearGradient(0,b.top,0,b.bottom,top,bottom,Shader.TileMode.CLAMP));
        float ratio=glyph==null?1:(float)Math.max(1,glyph.getIntrinsicWidth())/Math.max(1,glyph.getIntrinsicHeight());
        float h=b.height()*(brand.equals("youtube")?.72f:.78f),w=h*ratio;
        if(adaptiveBackground!=null){h=b.height()*1.2f;w=h*ratio;}
        if(w>b.width()*.78f){w=b.width()*.78f;h=w/ratio;}
        if(cover){w=b.width();h=w/ratio;if(h<b.height()){h=b.height();w=h*ratio;}}
        iconBounds.set(Math.round(b.exactCenterX()-w/2),Math.round(b.exactCenterY()-h/2),Math.round(b.exactCenterX()+w/2),Math.round(b.exactCenterY()+h/2));
    }
    @Override public void draw(Canvas canvas){
        Rect b=getBounds();canvas.drawRect(b,paint);
        if(adaptiveBackground!=null){adaptiveBackground.setBounds(b);adaptiveBackground.draw(canvas);}
        if(brand.equals("xuper")){
            xPaint.setStrokeWidth(b.height()*.10f);
            float h=b.height()*.45f,cx=b.exactCenterX(),cy=b.exactCenterY();
            canvas.drawLine(cx-h/2,cy-h/2,cx+h/2,cy+h/2,xPaint);canvas.drawLine(cx+h/2,cy-h/2,cx-h/2,cy+h/2,xPaint);
        }else if(glyph!=null){int saved=canvas.save();canvas.clipRect(b);glyph.setBounds(iconBounds);glyph.draw(canvas);canvas.restoreToCount(saved);}
    }
    @Override public int getIntrinsicWidth(){return 300;}
    @Override public int getIntrinsicHeight(){return 180;}
    @Override public void setAlpha(int alpha){ }
    @Override public void setColorFilter(ColorFilter f){ }
    @Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
