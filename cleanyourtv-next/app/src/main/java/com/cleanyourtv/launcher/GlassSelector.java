package com.cleanyourtv.launcher;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * The selector of the dock: a pane of tinted glass that slides from one banner to the next, takes the colour
 * of the selected app, and sits behind the banners (which lift above it). It replaces a ring around the
 * focused icon. It has no input of its own and never takes the focus. While the dock does not have the
 * focus it fades out.
 */
final class GlassSelector extends View {
    private static final int SLIDE_MS = 360;

    private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF from = new RectF();
    private final RectF to = new RectF();
    private final RectF cur = new RectF();
    private final RectF work = new RectF();
    private final ArgbEvaluator argb = new ArgbEvaluator();
    private final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
    private final float edgeWidth;

    private int fromColor;
    private int toColor;
    private int curColor;
    private float fromAlpha;
    private float toAlpha;
    private float curAlpha;
    private boolean placed;

    GlassSelector(Context context) {
        super(context);
        setFocusable(false);
        setWillNotDraw(false);
        edgeWidth = Ui.dp(context, 1.6f);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(edgeWidth);
        animator.setDuration(SLIDE_MS);
        animator.setInterpolator(new DecelerateInterpolator(1.8f));
        animator.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            cur.left = from.left + (to.left - from.left) * t;
            cur.top = from.top + (to.top - from.top) * t;
            cur.right = from.right + (to.right - from.right) * t;
            cur.bottom = from.bottom + (to.bottom - from.bottom) * t;
            curColor = (Integer) argb.evaluate(t, fromColor, toColor);
            curAlpha = fromAlpha + (toAlpha - fromAlpha) * t;
            invalidate();
        });
    }

    /** Slides the pane to a banner (left, top, right, bottom in this view's coordinates) and recolours it. */
    void moveTo(float left, float top, float right, float bottom, int color) {
        if (!placed) {
            // First time: appear in place instead of sliding in from the corner.
            cur.set(left, top, right, bottom);
            curColor = color;
            placed = true;
        }
        animator.cancel();
        from.set(cur);
        to.set(left, top, right, bottom);
        fromColor = curColor;
        toColor = color;
        fromAlpha = curAlpha;
        toAlpha = 1f;
        animator.start();
    }

    /** Fades the pane out where it is. */
    void hide() {
        if (!placed) return;
        animator.cancel();
        from.set(cur);
        to.set(cur);
        fromColor = curColor;
        toColor = curColor;
        fromAlpha = curAlpha;
        toAlpha = 0f;
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (curAlpha <= 0.01f) return;
        float radius = cur.height() * 0.26f;
        int rgb = curColor & 0x00FFFFFF;
        float a = curAlpha;

        // A soft colour spill around the pane, drawn as two larger, fainter panes.
        for (int i = 2; i >= 1; i--) {
            float grow = Ui.dp(getContext(), 5) * i;
            work.set(cur.left - grow, cur.top - grow, cur.right + grow, cur.bottom + grow);
            halo.setColor(rgb | (Math.round(0x26 * a / i) << 24));
            canvas.drawRoundRect(work, radius + grow, radius + grow, halo);
        }

        // The pane: the app's colour, see-through.
        body.setColor(rgb | (Math.round((Palette.light ? 0x88 : 0x77) * a) << 24));
        canvas.drawRoundRect(cur, radius, radius, body);

        // The sheen: light running across the top half, as on a glass edge.
        sheen.setShader(new LinearGradient(0, cur.top, 0, cur.centerY(),
                Math.round(0x80 * a) << 24 | 0x00FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        work.set(cur.left, cur.top, cur.right, cur.centerY());
        canvas.drawRoundRect(work, radius, radius, sheen);

        // The lit edge: bright at the top-left, fading, catching again at the bottom-right.
        edge.setShader(new LinearGradient(cur.left, cur.top, cur.right, cur.bottom,
                new int[] {Math.round(0xF0 * a) << 24 | 0x00FFFFFF, rgb | (Math.round(0x66 * a) << 24),
                        Math.round(0xA0 * a) << 24 | 0x00FFFFFF},
                new float[] {0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        work.set(cur.left + edgeWidth / 2, cur.top + edgeWidth / 2, cur.right - edgeWidth / 2, cur.bottom - edgeWidth / 2);
        canvas.drawRoundRect(work, radius, radius, edge);
    }

    @Override
    protected void onDetachedFromWindow() {
        animator.cancel();
        super.onDetachedFromWindow();
    }
}
