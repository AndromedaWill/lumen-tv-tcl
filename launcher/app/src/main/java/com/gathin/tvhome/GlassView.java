package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** Single crisp editing marker. No outer glow or duplicate white contours. */
public class GlassView extends View {
    private final Paint marker = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;
    private float radius;

    public GlassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        radius = 20 * density;
        marker.setStyle(Paint.Style.STROKE);
        marker.setStrokeWidth(1.5f * density);
        setAccent(0);
    }

    void setAccent(int argb) { marker.setColor(argb == 0 ? 0xFF9BBFE8 : argb); invalidate(); }
    void setRadius(float px) { radius = px; invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        float inset = 6f * density;
        rect.set(inset, inset, getWidth() - inset, getHeight() - inset);
        canvas.drawRoundRect(rect, radius, radius, marker);
    }
}
