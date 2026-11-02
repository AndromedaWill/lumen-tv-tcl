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
    private final int sampleAlpha,fallbackFill;
    private float[] corners;
    FrostedControlDrawable(Activity activity, View tile, boolean focused) {
        this(activity,tile,focused,TvDesign.radius(activity,R.dimen.ds_radius_panel));
    }
    FrostedControlDrawable(Activity activity, View tile, boolean focused, float radiusDp) {
        super(radiusDp,TvDesign.color(activity,focused?R.color.ds_focus_fill:R.color.ds_glass_fill));
        this.activity = activity;
        this.tile = tile;
        radius = radiusDp * activity.getResources().getDisplayMetrics().density;
        sampleAlpha=activity.getResources().getInteger(R.integer.ds_glass_sample_alpha);fallbackFill=TvDesign.color(activity,R.color.ds_glass_fallback);
    }
    FrostedControlDrawable(Activity activity, View tile, boolean focused, float[] radiiDp) {
        super(radiiDp,TvDesign.color(activity,focused?R.color.ds_focus_fill:R.color.ds_glass_fill));
        this.activity=activity;this.tile=tile;radius=0;
        sampleAlpha=activity.getResources().getInteger(R.integer.ds_glass_sample_alpha);fallbackFill=TvDesign.color(activity,R.color.ds_glass_fallback);
        corners=radiiDp.clone();float scale=activity.getResources().getDisplayMetrics().density;
        for(int i=0;i<corners.length;i++)corners[i]*=scale;
    }
    @Override protected void onBoundsChange(Rect b) {
        super.onBoundsChange(b);
        clip.reset();
        if(corners!=null)clip.addRoundRect(new android.graphics.RectF(b),corners,Path.Direction.CW);
        else clip.addRoundRect(b.left, b.top, b.right, b.bottom, radius, radius, Path.Direction.CW);
    }
    @Override public void draw(Canvas canvas) {
        View root = activity.findViewById(R.id.root);
        int save=canvas.save();canvas.clipPath(clip);
        if(root!=null && !Boolean.TRUE.equals(root.getTag(R.id.background_video_visible)) && root.getBackground() instanceof ScenicBackdropDrawable){
            tile.getLocationOnScreen(position);canvas.translate(-position[0],-position[1]);
            ((ScenicBackdropDrawable)root.getBackground()).drawFrost(canvas,sampleAlpha);
        } else canvas.drawColor(fallbackFill);
        canvas.restoreToCount(save);
        super.draw(canvas);
    }
}
