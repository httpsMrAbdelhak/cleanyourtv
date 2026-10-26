package com.cleanyourtv.launcher;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Weather icons drawn in code (no image files): a sun, a moon, clouds, rain, snow, fog and lightning,
 * in soft light colours that read on glass. The drawing scales to any size.
 */
final class WeatherGlyph extends Drawable {
    private static final int SUN = 0xFFFFC857;
    private static final int MOON = 0xFFDCE3F5;
    private static final int CLOUD = 0xFFEAF0FA;
    private static final int CLOUD_DARK = 0xFFA9B6CC;
    private static final int RAIN = 0xFF6FB6FF;
    private static final int SNOW = 0xFFFFFFFF;
    private static final int BOLT = 0xFFFFD84D;

    private final WeatherCode code;
    private final boolean day;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    WeatherGlyph(WeatherCode code, boolean day) {
        this.code = code;
        this.day = day;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        float s = Math.min(b.width(), b.height());
        if (s <= 0) return;
        canvas.save();
        canvas.translate(b.left + (b.width() - s) / 2f, b.top + (b.height() - s) / 2f);
        switch (code) {
            case CLEAR:
                sunOrMoon(canvas, s * 0.5f, s * 0.5f, s * 0.26f, s);
                break;
            case PARTLY:
                sunOrMoon(canvas, s * 0.38f, s * 0.36f, s * 0.2f, s);
                cloud(canvas, s * 0.1f, s * 0.38f, s * 0.8f, CLOUD);
                break;
            case CLOUDY:
                cloud(canvas, s * 0.03f, s * 0.22f, s * 0.7f, CLOUD_DARK);
                cloud(canvas, s * 0.2f, s * 0.36f, s * 0.78f, CLOUD);
                break;
            case FOG:
                cloud(canvas, s * 0.1f, s * 0.12f, s * 0.8f, CLOUD_DARK);
                lines(canvas, s, 0.62f, 0.78f, 0.92f);
                break;
            case DRIZZLE:
                cloud(canvas, s * 0.1f, s * 0.12f, s * 0.8f, CLOUD);
                drops(canvas, s, 2, RAIN, 0.7f);
                break;
            case RAIN:
            case SHOWERS:
                cloud(canvas, s * 0.1f, s * 0.12f, s * 0.8f, CLOUD_DARK);
                drops(canvas, s, 3, RAIN, 0.78f);
                break;
            case SNOW:
                cloud(canvas, s * 0.1f, s * 0.12f, s * 0.8f, CLOUD);
                flakes(canvas, s);
                break;
            case STORM:
                cloud(canvas, s * 0.1f, s * 0.1f, s * 0.8f, CLOUD_DARK);
                bolt(canvas, s);
                break;
            default:
                break;
        }
        canvas.restore();
    }

    private void sunOrMoon(Canvas canvas, float cx, float cy, float r, float s) {
        if (day) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(SUN);
            canvas.drawCircle(cx, cy, r, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(s * 0.045f);
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI / 4 * i;
                float inner = r * 1.35f;
                float outer = r * 1.75f;
                canvas.drawLine(cx + (float) Math.cos(angle) * inner, cy + (float) Math.sin(angle) * inner,
                        cx + (float) Math.cos(angle) * outer, cy + (float) Math.sin(angle) * outer, paint);
            }
        } else {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(MOON);
            // A bite out of the disc makes the crescent. Path.op avoids a CLEAR blend, which a
            // hardware canvas without its own layer would apply to everything behind the icon.
            Path disc = new Path();
            disc.addCircle(cx, cy, r, Path.Direction.CW);
            Path bite = new Path();
            bite.addCircle(cx + r * 0.55f, cy - r * 0.35f, r * 0.85f, Path.Direction.CW);
            disc.op(bite, Path.Op.DIFFERENCE);
            canvas.drawPath(disc, paint);
        }
    }

    private void cloud(Canvas canvas, float x, float y, float width, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        float h = width * 0.5f;
        path.reset();
        path.addCircle(x + width * 0.28f, y + h * 0.62f, h * 0.38f, Path.Direction.CW);
        path.addCircle(x + width * 0.52f, y + h * 0.42f, h * 0.5f, Path.Direction.CW);
        path.addCircle(x + width * 0.76f, y + h * 0.66f, h * 0.34f, Path.Direction.CW);
        path.addRoundRect(x + width * 0.14f, y + h * 0.62f, x + width * 0.88f, y + h * 1.0f, h * 0.19f, h * 0.19f, Path.Direction.CW);
        canvas.drawPath(path, paint);
    }

    private void drops(Canvas canvas, float s, int count, int color, float yTop) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(s * 0.045f);
        paint.setColor(color);
        for (int i = 0; i < count; i++) {
            float x = s * (count == 2 ? 0.38f + 0.24f * i : 0.30f + 0.2f * i);
            canvas.drawLine(x, s * yTop, x - s * 0.05f, s * (yTop + 0.14f), paint);
        }
    }

    private void flakes(Canvas canvas, float s) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(SNOW);
        for (int i = 0; i < 3; i++) {
            canvas.drawCircle(s * (0.3f + 0.2f * i), s * (i == 1 ? 0.86f : 0.76f), s * 0.035f, paint);
        }
    }

    private void lines(Canvas canvas, float s, float... ys) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(s * 0.04f);
        paint.setColor(CLOUD_DARK);
        for (int i = 0; i < ys.length; i++) {
            canvas.drawLine(s * (0.22f + 0.04f * i), s * ys[i], s * (0.78f - 0.04f * i), s * ys[i], paint);
        }
    }

    private void bolt(Canvas canvas, float s) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(BOLT);
        path.reset();
        path.moveTo(s * 0.54f, s * 0.58f);
        path.lineTo(s * 0.40f, s * 0.80f);
        path.lineTo(s * 0.51f, s * 0.80f);
        path.lineTo(s * 0.44f, s * 0.97f);
        path.lineTo(s * 0.64f, s * 0.72f);
        path.lineTo(s * 0.52f, s * 0.72f);
        path.close();
        canvas.drawPath(path, paint);
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }

    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
