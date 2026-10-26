package com.cleanyourtv.launcher;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** The two icons of the dock's fixed tiles, drawn in code so they need no image file. */
final class Glyphs {
    private Glyphs() { }

    static Drawable apps(int color) {
        return new Glyph(color, true);
    }

    static Drawable settings(int color) {
        return new Glyph(color, false);
    }

    private static final class Glyph extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean grid;

        Glyph(int color, boolean grid) {
            this.grid = grid;
            paint.setColor(color);
        }

        @Override public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            float size = Math.min(bounds.width(), bounds.height());
            float left = bounds.left + (bounds.width() - size) / 2f;
            float top = bounds.top + (bounds.height() - size) / 2f;
            if (grid) {
                float cell = size * 0.4f;
                float gap = size * 0.1f;
                float radius = size * 0.08f;
                for (int row = 0; row < 2; row++) {
                    for (int col = 0; col < 2; col++) {
                        float x = left + gap + col * (cell + gap);
                        float y = top + gap + row * (cell + gap);
                        canvas.drawRoundRect(new RectF(x, y, x + cell, y + cell), radius, radius, paint);
                    }
                }
            } else {
                // Three sliders: a line with a knob on it.
                float[] knob = {0.3f, 0.7f, 0.45f};
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeWidth(size * 0.07f);
                for (int i = 0; i < 3; i++) {
                    float y = top + size * (0.25f + 0.25f * i);
                    canvas.drawLine(left + size * 0.12f, y, left + size * 0.88f, y, paint);
                    canvas.drawCircle(left + size * knob[i], y, size * 0.1f, paint);
                }
            }
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }

        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }

        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
