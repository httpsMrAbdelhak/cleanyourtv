package com.cleanyourtv.launcher;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Draws a SceneSpec: a vertical base gradient and soft radial lights. No bitmap, so it costs no memory and is
 * sharp at any size. Each light is one unit-radius gradient, scaled by the canvas: nothing is allocated per
 * frame, which keeps the animated version cheap. setPhase moves the lights along their slow loops.
 */
final class SceneDrawable extends Drawable {
    private final SceneSpec spec;
    private final Paint base = new Paint();
    private final Paint[] lights;
    private float phase;

    SceneDrawable(SceneSpec spec) {
        this.spec = spec;
        lights = new Paint[spec.blobs.length];
        for (int i = 0; i < lights.length; i++) {
            int rgb = spec.blobs[i].color & 0x00FFFFFF;
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(new RadialGradient(0f, 0f, 1f,
                    new int[] {0xB0000000 | rgb, 0x58000000 | rgb, 0x00000000 | rgb},
                    new float[] {0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
            lights[i] = paint;
        }
    }

    /** phase in [0, 1): where the loop is. 0 is the still picture used by the static wallpaper. */
    void setPhase(float phase) {
        this.phase = phase;
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        base.setShader(new LinearGradient(0, bounds.top, 0, bounds.bottom, spec.top, spec.bottom, Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        canvas.drawRect(b, base);
        float width = b.width();
        for (int i = 0; i < lights.length; i++) {
            SceneSpec.Blob blob = spec.blobs[i];
            float cx = b.left + width * (blob.x + blob.driftX * SceneMath.wave(phase, blob.speedX, blob.offset));
            float cy = b.top + b.height() * (blob.y) + width * blob.driftY
                    * SceneMath.wave(phase, blob.speedY, blob.offset + 0.25f);
            float rx = width * blob.radius;
            canvas.save();
            canvas.translate(cx, cy);
            canvas.scale(rx, rx * blob.squash);
            canvas.drawCircle(0f, 0f, 1f, lights[i]);
            canvas.restore();
        }
    }

    @Override public void setAlpha(int alpha) { }

    @Override public void setColorFilter(ColorFilter filter) { }

    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
