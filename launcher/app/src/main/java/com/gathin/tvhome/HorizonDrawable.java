package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Original twilight landscape. Static display-list drawing; no bitmap, blur, clock or worker. */
final class HorizonDrawable extends Drawable {
    private final Paint sky = gradient(0, 540, 0xFF111A38, 0xFFB47783);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint distant = gradient(210, 470, 0xFF58608B, 0xFF333D68);
    private final Paint middle = gradient(260, 540, 0xFF394B78, 0xFF192847);
    private final Paint near = gradient(330, 540, 0xFF243550, 0xFF0C1528);
    private final Paint shade = gradient(270, 540, 0x00101A30, 0xD9081020);
    private final Paint sun = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path farRidge = new Path();
    private final Path midRidge = new Path();
    private final Path nearRidge = new Path();

    HorizonDrawable() {
        glow.setShader(new RadialGradient(640, 230, 330,
                new int[]{0x88EAA698, 0x20C597AA, 0x00C597AA},
                new float[]{0, .55f, 1}, Shader.TileMode.CLAMP));
        sun.setColor(0xB8F6D5C2);
        farRidge.moveTo(0, 338);
        farRidge.cubicTo(130, 315, 195, 200, 310, 228);
        farRidge.cubicTo(377, 245, 403, 290, 470, 268);
        farRidge.cubicTo(560, 223, 612, 180, 691, 208);
        farRidge.cubicTo(760, 224, 828, 290, 960, 255);
        closeLandscape(farRidge);
        midRidge.moveTo(0, 290);
        midRidge.cubicTo(94, 260, 185, 319, 280, 338);
        midRidge.cubicTo(388, 362, 420, 291, 516, 304);
        midRidge.cubicTo(650, 324, 725, 397, 960, 316);
        closeLandscape(midRidge);
        nearRidge.moveTo(0, 406);
        nearRidge.cubicTo(152, 353, 248, 389, 405, 401);
        nearRidge.cubicTo(620, 428, 720, 309, 960, 346);
        closeLandscape(nearRidge);
    }

    private static Paint gradient(float top, float bottom, int a, int b) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new LinearGradient(0, top, 0, bottom, a, b, Shader.TileMode.CLAMP));
        return paint;
    }

    private static void closeLandscape(Path path) {
        path.lineTo(960, 540); path.lineTo(0, 540); path.close();
    }

    @Override public void draw(Canvas canvas) {
        int save = canvas.save();
        canvas.translate(getBounds().left, getBounds().top);
        canvas.scale(getBounds().width() / 960f, getBounds().height() / 540f);
        canvas.drawRect(0, 0, 960, 540, sky);
        canvas.drawRect(0, 0, 960, 540, glow);
        canvas.drawCircle(640, 183, 17, sun);
        canvas.drawPath(farRidge, distant);
        canvas.drawPath(midRidge, middle);
        canvas.drawPath(nearRidge, near);
        canvas.drawRect(0, 270, 960, 540, shade);
        canvas.restoreToCount(save);
    }

    @Override public void setAlpha(int alpha) { /* Root remains opaque. */ }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
