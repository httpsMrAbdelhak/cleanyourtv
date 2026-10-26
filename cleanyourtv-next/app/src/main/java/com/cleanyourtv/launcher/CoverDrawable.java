package com.cleanyourtv.launcher;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** A bitmap scaled to cover the whole area, centred, cropping what overflows (like a poster crop). */
final class CoverDrawable extends Drawable {
    private final Bitmap bitmap;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Matrix matrix = new Matrix();

    CoverDrawable(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        if (bounds.isEmpty() || bitmap.getWidth() == 0 || bitmap.getHeight() == 0) return;
        float scale = Math.max(bounds.width() / (float) bitmap.getWidth(), bounds.height() / (float) bitmap.getHeight());
        matrix.setScale(scale, scale);
        matrix.postTranslate(bounds.left + (bounds.width() - bitmap.getWidth() * scale) / 2f,
                bounds.top + (bounds.height() - bitmap.getHeight() * scale) / 2f);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.save();
        canvas.clipRect(getBounds());
        canvas.drawBitmap(bitmap, matrix, paint);
        canvas.restore();
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }

    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }

    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
