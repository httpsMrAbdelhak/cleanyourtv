package com.cleanyourtv.launcher;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * The light along the bottom of the screen, from corner to corner, in the colour of the selected app. The band
 * itself never moves; only a brighter spot under the selected app slides sideways, so the selection shows
 * without the whole light travelling. Both are one white gradient each, tinted by a colour filter: changing
 * colour, moving the spot and fading cost a few state changes per frame, never a bitmap or a blur. When the
 * focus leaves the dock the light fades out. It has no input of its own and never takes the focus.
 */
final class AmbientGlow extends View {
    private static final int MOVE_MS = 420;

    private final Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArgbEvaluator argb = new ArgbEvaluator();
    private final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);

    private float fromX;
    private float toX;
    private float curX;
    private int fromColor;
    private int toColor;
    private int curColor;
    private float fromAlpha;
    private float toAlpha;
    private float curAlpha;
    private boolean placed;
    private float strength = 0.85f;
    private int lastHeight = -1;

    AmbientGlow(Context context) {
        super(context);
        setFocusable(false);
        setWillNotDraw(false);
        paint.setShader(new RadialGradient(0f, 0f, 1f,
                new int[] {0xFFFFFFFF, 0x66FFFFFF, 0x00FFFFFF}, new float[] {0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        animator.setDuration(MOVE_MS);
        animator.setInterpolator(new DecelerateInterpolator(1.6f));
        animator.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            curX = fromX + (toX - fromX) * t;
            curColor = (Integer) argb.evaluate(t, fromColor, toColor);
            curAlpha = fromAlpha + (toAlpha - fromAlpha) * t;
            invalidate();
        });
    }

    /** How bright the light gets at most (0..1): a pale wallpaper needs less. */
    void setStrength(float strength) {
        this.strength = strength;
    }

    /** Lights the band, with the brighter spot under centerX (in this view's coordinates) with colour, gliding from where it was. */
    void aim(float centerX, int color) {
        if (!placed) {
            // First light: appear in place, from nothing, instead of sliding in from the corner.
            curX = centerX;
            curColor = color;
            placed = true;
        }
        start(centerX, color, 1f);
    }

    /** Fades the light out where it is. */
    void fadeOut() {
        start(curX, curColor, 0f);
    }

    private void start(float x, int color, float alpha) {
        animator.cancel();
        fromX = curX;
        toX = x;
        fromColor = curColor;
        toColor = color;
        fromAlpha = curAlpha;
        toAlpha = alpha;
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (curAlpha <= 0.01f) return;
        int width = getWidth();
        int height = getHeight();
        paint.setColorFilter(new PorterDuffColorFilter(curColor, PorterDuff.Mode.SRC_IN));
        paint.setAlpha(Math.round(255 * curAlpha * strength));
        // The band: the whole width, strongest at the bottom edge, gone above the lower third.
        if (lastHeight != height) {
            band.setShader(new LinearGradient(0f, height, 0f, height * 0.25f,
                    new int[] {0xFFFFFFFF, 0x55FFFFFF, 0x00FFFFFF}, new float[] {0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
            lastHeight = height;
        }
        band.setColorFilter(new PorterDuffColorFilter(curColor, PorterDuff.Mode.SRC_IN));
        band.setAlpha(Math.round(255 * curAlpha * strength * 0.55f));
        canvas.drawRect(0f, 0f, width, height, band);
        // The spot: brighter under the selected app. Only this part slides.
        canvas.save();
        canvas.translate(curX, height);
        canvas.scale(width * 0.20f, height * 0.85f);
        canvas.drawCircle(0f, 0f, 1f, paint);
        canvas.restore();
    }

    @Override
    protected void onDetachedFromWindow() {
        animator.cancel();
        super.onDetachedFromWindow();
    }
}
