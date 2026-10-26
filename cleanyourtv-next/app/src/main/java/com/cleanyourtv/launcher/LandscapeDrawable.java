package com.cleanyourtv.launcher;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Paints a LandscapeSpec with gradients and paths only: no bitmap, so it costs no memory and stays sharp on
 * any screen. The outlines are computed once per size; drawing is a handful of fills. It does not move.
 */
final class LandscapeDrawable extends Drawable {
    private static final int STEPS = 160;

    private final LandscapeSpec spec;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path[] ridges;
    private final Shader unitGlow = new RadialGradient(0f, 0f, 1f,
            new int[] {0xFFFFFFFF, 0x66FFFFFF, 0x00FFFFFF}, new float[] {0f, 0.4f, 1f}, Shader.TileMode.CLAMP);

    LandscapeDrawable(LandscapeSpec spec) {
        this.spec = spec;
        ridges = new Path[spec.ranges.length];
        for (int i = 0; i < ridges.length; i++) ridges[i] = new Path();
    }

    @Override
    protected void onBoundsChange(Rect b) {
        for (int r = 0; r < ridges.length; r++) {
            LandscapeSpec.Range range = spec.ranges[r];
            Path path = ridges[r];
            path.reset();
            path.moveTo(b.left, b.bottom);
            for (int i = 0; i <= STEPS; i++) {
                float x = i / (float) STEPS;
                float y = b.top + b.height() * (range.base - range.peaks * Ridge.height(x, range.seed));
                path.lineTo(b.left + b.width() * x, y);
            }
            path.lineTo(b.right, b.bottom);
            path.close();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        float w = b.width();
        float h = b.height();

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, b.top, 0, b.bottom, spec.sky, spec.skyStops, Shader.TileMode.CLAMP));
        canvas.drawRect(b, paint);
        paint.setShader(null);

        if (spec.stars) {
            paint.setColor(0xFFFFFFFF);
            long seed = 17;
            for (int i = 0; i < 140; i++) {
                seed = (seed * 1103515245L + 12345L) & 0x7FFFFFFFL;
                float x = (seed % 10000) / 10000f;
                seed = (seed * 1103515245L + 12345L) & 0x7FFFFFFFL;
                float y = (seed % 10000) / 10000f * 0.5f;
                paint.setAlpha(60 + (int) (seed % 140));
                canvas.drawCircle(b.left + w * x, b.top + h * y, 0.0012f * w * (1 + (seed % 3)), paint);
            }
            paint.setAlpha(255);
        }

        // The sun: a wide glow, then the disc.
        float sx = b.left + w * spec.sunX;
        float sy = b.top + h * spec.sunY;
        float r = w * spec.sunRadius;
        blob(canvas, sx, sy, r * 7f, r * 5f, spec.glowColor, 0.85f);
        blob(canvas, sx, sy, r * 2.4f, r * 2.4f, spec.glowColor, 1f);
        blob(canvas, sx, sy, r, r, spec.sunColor, 1f);

        for (float[] cloud : spec.clouds) {
            blob(canvas, b.left + w * cloud[0], b.top + h * cloud[1], w * cloud[2], w * cloud[2] * cloud[3], spec.cloudColor, 0.8f);
        }

        // Ranges from the far one to the near one, each fading a little into the mist.
        for (int i = 0; i < ridges.length; i++) {
            LandscapeSpec.Range range = spec.ranges[i];
            float top = b.top + h * (range.base - range.peaks);
            int foot = mix(range.color, spec.mist, 0.55f - 0.1f * (i / (float) Math.max(1, ridges.length)));
            paint.setShader(new LinearGradient(0, top, 0, b.top + h * (range.base + 0.04f), range.color, foot, Shader.TileMode.CLAMP));
            canvas.drawPath(ridges[i], paint);
            paint.setShader(null);
        }

        if (spec.lake > 0f) {
            float top = b.top + h * spec.lake;
            int horizon = spec.sky[spec.sky.length - 1];
            paint.setShader(new LinearGradient(0, top, 0, b.bottom, mix(horizon, spec.ranges[spec.ranges.length - 1].color, 0.45f),
                    mix(horizon, 0xFF0A0614, 0.8f), Shader.TileMode.CLAMP));
            canvas.drawRect(b.left, top, b.right, b.bottom, paint);
            paint.setShader(null);
            // The sun's reflection: a tall, narrow glow on the water.
            blob(canvas, sx, top + h * 0.10f, r * 1.6f, h * 0.12f, spec.glowColor, 0.7f);
            paint.setColor(0x14FFFFFF);
            for (int i = 0; i < 7; i++) {
                float y = top + h * (0.03f + 0.045f * i);
                canvas.drawRect(b.left + w * (0.1f + 0.07f * i), y, b.left + w * (0.5f + 0.06f * i), y + h * 0.003f, paint);
            }
        }
    }

    /** A soft light: the unit glow scaled to rx by ry and tinted. */
    private void blob(Canvas canvas, float cx, float cy, float rx, float ry, int color, float strength) {
        paint.setShader(unitGlow);
        paint.setColorFilter(new android.graphics.PorterDuffColorFilter(color | 0xFF000000, android.graphics.PorterDuff.Mode.SRC_IN));
        paint.setAlpha(Math.round(255 * strength));
        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(rx, ry);
        canvas.drawCircle(0f, 0f, 1f, paint);
        canvas.restore();
        paint.setColorFilter(null);
        paint.setShader(null);
        paint.setAlpha(255);
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    @Override public void setAlpha(int alpha) { }

    @Override public void setColorFilter(ColorFilter filter) { }

    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
