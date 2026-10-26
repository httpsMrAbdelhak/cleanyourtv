package com.cleanyourtv.launcher;

import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * The Liquid Glass surface: the blurred wallpaper showing through, a pale tint that is brighter at the top,
 * and a thin lit edge that is strongest at the top-left corner like light catching glass.
 *
 * Two modes. Sampling panels paint the blurred backdrop at the place they sit on screen; use them only for
 * panels that stay put (a scrolled panel would show the wrong part). Flat panels, and every panel in the
 * Performance mode, use a plain translucent fill instead.
 */
final class GlassDrawable extends Drawable {
    private final float radius;
    private final float strokeWidth;
    private final boolean sample;
    private final boolean bright;
    private final boolean frosted;
    private final boolean clear;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hairline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Matrix matrix = new Matrix();
    private final int[] where = new int[2];
    private BitmapShader shader;
    private int shaderVersion = -1;

    /**
     * @param sampleBackdrop paint the blurred wallpaper behind the panel (static panels only)
     * @param bright a livelier panel, for the one that has focus
     */
    GlassDrawable(float radiusPx, float strokePx, boolean sampleBackdrop, boolean bright, Effects effects) {
        this(radiusPx, strokePx, sampleBackdrop, bright, false, effects);
    }

    /** clear: the smoked, see-through glass of the dock instead of the milky one. */
    GlassDrawable(float radiusPx, float strokePx, boolean sampleBackdrop, boolean bright, boolean clear, Effects effects) {
        this.clear = clear;
        this.radius = radiusPx;
        this.strokeWidth = strokePx;
        this.sample = sampleBackdrop;
        this.bright = bright;
        this.frosted = effects.frostedGlass();
        shade.setColor(clear ? Palette.CLEAR_SHADE : Palette.GLASS_SHADE);
        fill.setColor(clear ? Palette.CLEAR_FLAT : (bright ? Palette.GLASS_FILL_BRIGHT : Palette.GLASS_FILL));
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(strokePx);
        hairline.setStyle(Paint.Style.STROKE);
        hairline.setStrokeWidth(strokePx + Math.max(1f, strokePx * 0.6f));
        hairline.setColor(Palette.HAIRLINE);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        int top = clear ? Palette.CLEAR_TINT_TOP : (bright ? Palette.TINT_TOP_BRIGHT : Palette.TINT_TOP);
        int bottom = clear ? Palette.CLEAR_TINT_BOTTOM : (bright ? Palette.TINT_BOTTOM_BRIGHT : Palette.TINT_BOTTOM);
        tint.setShader(new LinearGradient(0, bounds.top, 0, bounds.bottom, top, bottom, Shader.TileMode.CLAMP));
        int lit = bright ? Palette.EDGE_LIT_BRIGHT : Palette.EDGE_LIT;
        edge.setShader(new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                new int[] {lit, Palette.EDGE_MID, Palette.EDGE_END}, new float[] {0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        float inset = strokeWidth / 2f;
        rect.set(b.left + inset, b.top + inset, b.right - inset, b.bottom - inset);

        boolean drawn = false;
        if (frosted && sample && getCallback() instanceof View && Backdrop.bitmap() != null) {
            View view = (View) getCallback();
            if (shader == null || shaderVersion != Backdrop.version()) {
                shader = new BitmapShader(Backdrop.bitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                shaderVersion = Backdrop.version();
            }
            view.getLocationInWindow(where);
            matrix.setScale(Backdrop.scaleX(), Backdrop.scaleY());
            matrix.postTranslate(-where[0], -where[1]);
            shader.setLocalMatrix(matrix);
            fill.setShader(shader);
            canvas.drawRoundRect(rect, radius, radius, fill);
            fill.setShader(null);
            canvas.drawRoundRect(rect, radius, radius, shade);
            drawn = true;
        }
        if (!drawn) canvas.drawRoundRect(rect, radius, radius, fill);
        canvas.drawRoundRect(rect, radius, radius, tint);
        if (Palette.HAIRLINE != 0) canvas.drawRoundRect(rect, radius, radius, hairline);
        canvas.drawRoundRect(rect, radius, radius, edge);
    }

    @Override public void setAlpha(int alpha) { }

    @Override public void setColorFilter(ColorFilter filter) { }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
