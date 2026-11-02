package com.gathin.tvhome;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import org.xmlpull.v1.XmlPullParser;

/** Quiet translucent surfaces shared by dock, controls and menus.
 * Thin directional rim and cached sheen, without bloom, thick bevel or live blur.
 * The historical name remains because XML resources and R8 refer to this class. */
public class LiquidGlassDrawable extends Drawable {
    private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float density = Resources.getSystem().getDisplayMetrics().density;
    private final float radiusDp;
    private final int tint;
    private int alpha = 255;
    private float[] cornersDp;
    private android.graphics.Path bodyPath, rimPath;

    public LiquidGlassDrawable() { this(-1f, 0x30101620); }

    LiquidGlassDrawable(float radiusDp, int tint) {
        this.radiusDp = radiusDp;
        this.tint = tint;
        updateColor();
    }

    LiquidGlassDrawable(float[] cornersDp, int tint) {
        this(0f,tint);
        if(cornersDp.length!=8)throw new IllegalArgumentException("Eight corner radii required");
        this.cornersDp=cornersDp.clone();
        bodyPath=new android.graphics.Path();rimPath=new android.graphics.Path();
    }

    public static class Panel extends LiquidGlassDrawable {
        public Panel() { super(22f, 0xA01C1C1C); }
    }

    @Override public void inflate(@NonNull Resources r, @NonNull XmlPullParser parser,
                                  @NonNull AttributeSet attrs, Resources.Theme theme) {
        density = r.getDisplayMetrics().density;
    }

    private void updateColor() {
        body.setColor((tint & 0x00FFFFFF) | (((tint >>> 24) * alpha / 255) << 24));
    }

    @Override protected void onBoundsChange(Rect b) {
        sheen.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom,
                new int[]{0x18FFFFFF, 0x04FFFFFF, 0x00FFFFFF, 0x18000000},
                new float[]{0, .24f, .52f, 1}, Shader.TileMode.CLAMP));
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(Math.max(1f, .65f * density));
        rim.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom,
                new int[]{0xB8FFFFFF, 0x30FFFFFF, 0x0CFFFFFF, 0x487B7B7B},
                new float[]{0, .34f, .65f, 1}, Shader.TileMode.CLAMP));
        if(cornersDp!=null){
            float[] radii=new float[8];
            for(int i=0;i<8;i++)radii[i]=cornersDp[i]*density;
            bodyPath.reset();bodyPath.addRoundRect(new RectF(b),radii,android.graphics.Path.Direction.CW);
            float inset=rim.getStrokeWidth()/2;RectF stroke=new RectF(b);stroke.inset(inset,inset);
            for(int i=0;i<8;i++)radii[i]=Math.max(0,radii[i]-inset);
            rimPath.reset();rimPath.addRoundRect(stroke,radii,android.graphics.Path.Direction.CW);
        }
    }

    @Override public void draw(@NonNull Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        if(bodyPath!=null){
            if(body.getAlpha()>0)canvas.drawPath(bodyPath,body);
            canvas.drawPath(bodyPath,sheen);canvas.drawPath(rimPath,rim);return;
        }
        rect.set(b);
        float r = radiusDp < 0 ? rect.height() / 2f : Math.min(radiusDp * density, rect.height() / 2f);
        if (body.getAlpha() > 0) canvas.drawRoundRect(rect, r, r, body);
        canvas.drawRoundRect(rect, r, r, sheen);
        float inset = rim.getStrokeWidth() / 2;
        rect.inset(inset, inset);
        canvas.drawRoundRect(rect, Math.max(0, r - inset), Math.max(0, r - inset), rim);
    }

    @Override public void getOutline(@NonNull android.graphics.Outline outline) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        if(bodyPath!=null){
            if(android.os.Build.VERSION.SDK_INT>=30)outline.setPath(bodyPath);else outline.setConvexPath(bodyPath);
            outline.setAlpha(.3f);return;
        }
        float r = radiusDp < 0 ? b.height() / 2f : Math.min(radiusDp * density, b.height() / 2f);
        outline.setRoundRect(b, r);
        outline.setAlpha(.3f);
    }

    @Override public void setAlpha(int a) { alpha = Math.max(0, Math.min(255, a)); updateColor(); sheen.setAlpha(alpha); rim.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { body.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
