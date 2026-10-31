package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

/** Local diagonal reflection, inspired by Lumen's glass sheen. No outer white rim or live blur. */
final class ReflectiveSurfaceDrawable extends LiquidGlassDrawable {
    private final Paint reflection = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF area = new RectF();
    private final float radius;
    ReflectiveSurfaceDrawable(float radiusDp, int tint) {
        super(radiusDp, tint);
        radius = radiusDp * android.content.res.Resources.getSystem().getDisplayMetrics().density;
    }
    @Override protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        area.set(bounds);
        reflection.setShader(new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                new int[]{0x30E8F1FF, 0x10E8F1FF, 0x00E8F1FF, 0x10000000},
                new float[]{0, .32f, .6f, 1}, Shader.TileMode.CLAMP));
    }
    @Override public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.drawRoundRect(area, radius, radius, reflection);
    }
    @Override public void setAlpha(int alpha) {
        super.setAlpha(alpha);
        reflection.setAlpha(alpha);
    }
}
