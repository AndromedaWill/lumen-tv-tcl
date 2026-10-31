package com.gathin.tvhome;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Native translation of Lumen cf116b6 tokens.css glass-bar (MIT; assets/licenses/lumen-MIT.txt).
 * Material only: its alpha must preserve the real wallpaper underneath, never an opaque photo copy. */
final class LumenDockDrawable extends Drawable {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF area = new RectF();
    private final float density;
    LumenDockDrawable(float density) {
        this.density = density;
        fill.setColor(0x4D000000); // rgba(0,0,0,.30)
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(.75f * density); // 1.5 CSS px at 1080p
    }
    @Override protected void onBoundsChange(Rect b) {
        area.set(b);
        float edge = Math.min(.49f, 100 * density / b.width()); // 200 CSS px
        sheen.setShader(new LinearGradient(0, 0, b.width(), b.height(),
                new int[]{0x21FFFFFF, 0x08FFFFFF, 0x08FFFFFF, 0x14FFFFFF},
                new float[]{0, edge, 1-edge, 1}, Shader.TileMode.CLAMP));
        rim.setShader(new LinearGradient(0, 0, b.width(), b.height(),
                new int[]{0xB36EC8FF, 0x8CFFFFFF, 0x33FFFFFF, 0x47FF78BE},
                new float[]{0, .18f, .72f, 1}, Shader.TileMode.CLAMP));
    }
    @Override public void draw(Canvas c) {
        float radius = 37 * density; // 34px tile radius + 40px inset
        c.drawRoundRect(area, radius, radius, fill);
        c.drawRoundRect(area, radius, radius, sheen);
        float inset = rim.getStrokeWidth()/2;
        area.inset(inset,inset);
        c.drawRoundRect(area,radius-inset,radius-inset,rim);
        area.inset(-inset,-inset);
    }
    @Override public void setAlpha(int a) { fill.setAlpha(Math.round(a*.3f)); sheen.setAlpha(a); rim.setAlpha(a); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter f) { fill.setColorFilter(f); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
