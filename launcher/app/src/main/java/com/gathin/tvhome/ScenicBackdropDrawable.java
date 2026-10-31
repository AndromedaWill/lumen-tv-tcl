package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

/** One offline photo, decoded once off the UI thread. No renderer, timers or network. */
final class ScenicBackdropDrawable extends Drawable {
    private final Bitmap photo;
    private final Bitmap frost;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF();
    private ScenicBackdropDrawable(Bitmap photo, Bitmap frost) {
        this.photo = photo; this.frost = frost;
        paint.setColorFilter(new android.graphics.PorterDuffColorFilter(0x50000000,
                android.graphics.PorterDuff.Mode.SRC_ATOP));
    }
    static void load(Activity activity, View target) {
        new Thread(() -> {
            BitmapFactory.Options options = new BitmapFactory.Options();
            // TV UI is often 960x540 dp; a background needs less detail than app artwork.
            options.inSampleSize = 2;
            Bitmap photo = BitmapFactory.decodeResource(activity.getResources(), R.drawable.hercules, options);
            if (photo == null) return;
            Bitmap frost = Bitmap.createScaledBitmap(photo, 96, 64, true);
            activity.runOnUiThread(() -> {
                if (!activity.isFinishing() && !activity.isDestroyed())
                    target.setBackground(new ScenicBackdropDrawable(photo, frost));
                else { photo.recycle(); frost.recycle(); }
            });
        }, "home-photo-decode").start();
    }
    /** Same photo alignment as the wallpaper, with a tiny cached image for the dock's frost. */
    void drawFrost(Canvas canvas) {
        canvas.drawBitmap(frost, null, destination, paint);
    }
    @Override public void draw(Canvas canvas) {
        float scale = Math.max(getBounds().width() / (float) photo.getWidth(),
                getBounds().height() / (float) photo.getHeight());
        float w = photo.getWidth() * scale, h = photo.getHeight() * scale;
        float x = getBounds().centerX() - w / 2, y = getBounds().centerY() - h / 2;
        destination.set(x, y, x + w, y + h);
        canvas.drawBitmap(photo, null, destination, paint);
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
