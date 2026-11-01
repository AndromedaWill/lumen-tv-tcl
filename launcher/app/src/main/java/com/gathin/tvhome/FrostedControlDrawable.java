package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.View;

/** Reuses the Home photo's tiny frost image; no screenshot or extra decoded wallpaper per control. */
final class FrostedControlDrawable extends LiquidGlassDrawable {
    private final View tile;
    private final Activity activity;
    private final Path clip = new Path();
    private final int[] position = new int[2];
    private final float radius;
    FrostedControlDrawable(Activity activity, View tile, boolean focused) {
        this(activity,tile,focused,27);
    }
    FrostedControlDrawable(Activity activity, View tile, boolean focused, float radiusDp) {
        super(radiusDp, focused ? 0xE0DDE6F4 : 0x68141A25);
        this.activity = activity;
        this.tile = tile;
        radius = radiusDp * activity.getResources().getDisplayMetrics().density;
    }
    @Override protected void onBoundsChange(Rect b) {
        super.onBoundsChange(b);
        clip.reset();
        clip.addRoundRect(b.left, b.top, b.right, b.bottom, radius, radius, Path.Direction.CW);
    }
    @Override public void draw(Canvas canvas) {
        View root = activity.findViewById(R.id.root);
        int save=canvas.save();canvas.clipPath(clip);
        if(root!=null && root.getBackground() instanceof ScenicBackdropDrawable){
            tile.getLocationOnScreen(position);canvas.translate(-position[0],-position[1]);
            ((ScenicBackdropDrawable)root.getBackground()).drawFrost(canvas);
        } else canvas.drawColor(0xFF141C27);
        canvas.restoreToCount(save);
        super.draw(canvas);
    }
}
