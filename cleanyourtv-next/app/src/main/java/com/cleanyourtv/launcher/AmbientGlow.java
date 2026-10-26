package com.cleanyourtv.launcher;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * The soft light that spreads along the bottom of the screen under the selected dock icon. One white radial
 * gradient, tinted with a colour filter and stretched into a wide ellipse by the canvas: moving, recolouring
 * and fading it costs a few state changes per frame, never a bitmap or a blur. When the focus leaves the
 * dock the light fades out. It has no input of its own and never takes the focus.
 */
final class AmbientGlow extends View {
    private static final int MOVE_MS = 420;

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

    /** Lights the glow under centerX (in this view's coordinates) with colour, gliding from where it was. */
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
        canvas.save();
        canvas.translate(curX, height);
        canvas.scale(width * 0.36f, height * 0.95f);
        canvas.drawCircle(0f, 0f, 1f, paint);
        canvas.restore();
    }

    @Override
    protected void onDetachedFromWindow() {
        animator.cancel();
        super.onDetachedFromWindow();
    }
}
