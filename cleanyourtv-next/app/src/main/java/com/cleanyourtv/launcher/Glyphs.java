package com.cleanyourtv.launcher;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Small icons drawn in code, so they need no image file: apps, settings, search, moon, bolt, chevron. */
final class Glyphs {
    private Glyphs() { }

    private static final int APPS = 0;
    private static final int SETTINGS = 1;
    private static final int SEARCH = 2;
    private static final int MOON = 3;
    private static final int BOLT = 4;
    private static final int CHEVRON = 5;

    static Drawable apps(int color) { return new Glyph(color, APPS); }

    static Drawable settings(int color) { return new Glyph(color, SETTINGS); }

    static Drawable search(int color) { return new Glyph(color, SEARCH); }

    static Drawable moon(int color) { return new Glyph(color, MOON); }

    static Drawable bolt(int color) { return new Glyph(color, BOLT); }

    static Drawable chevronDown(int color) { return new Glyph(color, CHEVRON); }

    private static final class Glyph extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final int kind;

        Glyph(int color, int kind) {
            this.kind = kind;
            paint.setColor(color);
        }

        @Override public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            float size = Math.min(bounds.width(), bounds.height());
            float l = bounds.left + (bounds.width() - size) / 2f;
            float t = bounds.top + (bounds.height() - size) / 2f;
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            switch (kind) {
                case APPS: {
                    paint.setStyle(Paint.Style.FILL);
                    float cell = size * 0.4f;
                    float gap = size * 0.1f;
                    float radius = size * 0.08f;
                    for (int row = 0; row < 2; row++) {
                        for (int col = 0; col < 2; col++) {
                            float x = l + gap + col * (cell + gap);
                            float y = t + gap + row * (cell + gap);
                            canvas.drawRoundRect(new RectF(x, y, x + cell, y + cell), radius, radius, paint);
                        }
                    }
                    break;
                }
                case SETTINGS: {
                    // Three sliders: a line with a knob on it.
                    paint.setStyle(Paint.Style.FILL_AND_STROKE);
                    float[] knob = {0.3f, 0.7f, 0.45f};
                    paint.setStrokeWidth(size * 0.07f);
                    for (int i = 0; i < 3; i++) {
                        float y = t + size * (0.25f + 0.25f * i);
                        canvas.drawLine(l + size * 0.12f, y, l + size * 0.88f, y, paint);
                        canvas.drawCircle(l + size * knob[i], y, size * 0.1f, paint);
                    }
                    break;
                }
                case SEARCH: {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(size * 0.1f);
                    canvas.drawCircle(l + size * 0.43f, t + size * 0.43f, size * 0.26f, paint);
                    canvas.drawLine(l + size * 0.62f, t + size * 0.62f, l + size * 0.84f, t + size * 0.84f, paint);
                    break;
                }
                case MOON: {
                    paint.setStyle(Paint.Style.FILL);
                    Path disc = new Path();
                    disc.addCircle(l + size * 0.5f, t + size * 0.5f, size * 0.34f, Path.Direction.CW);
                    Path bite = new Path();
                    bite.addCircle(l + size * 0.68f, t + size * 0.36f, size * 0.30f, Path.Direction.CW);
                    disc.op(bite, Path.Op.DIFFERENCE);
                    canvas.drawPath(disc, paint);
                    break;
                }
                case BOLT: {
                    paint.setStyle(Paint.Style.FILL);
                    path.reset();
                    path.moveTo(l + size * 0.58f, t + size * 0.10f);
                    path.lineTo(l + size * 0.26f, t + size * 0.56f);
                    path.lineTo(l + size * 0.48f, t + size * 0.56f);
                    path.lineTo(l + size * 0.40f, t + size * 0.90f);
                    path.lineTo(l + size * 0.74f, t + size * 0.42f);
                    path.lineTo(l + size * 0.52f, t + size * 0.42f);
                    path.close();
                    canvas.drawPath(path, paint);
                    break;
                }
                default: {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(size * 0.1f);
                    path.reset();
                    path.moveTo(l + size * 0.25f, t + size * 0.38f);
                    path.lineTo(l + size * 0.5f, t + size * 0.62f);
                    path.lineTo(l + size * 0.75f, t + size * 0.38f);
                    canvas.drawPath(path, paint);
                    break;
                }
            }
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }

        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }

        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
