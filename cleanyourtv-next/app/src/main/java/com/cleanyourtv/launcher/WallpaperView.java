package com.cleanyourtv.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;

/**
 * The full-screen wallpaper, with a soft shade at the top and a deeper one at the bottom that keeps the dock
 * readable on any picture. An animated scene redraws at about 15 frames a second, only while the screen is
 * shown, and gives up for good (back to the still picture) if the TV cannot keep that pace.
 */
final class WallpaperView extends View {
    private static final long TICK_MS = 66;
    private static final long SLOW_GAP_MS = 300;
    private static final int SLOW_LIMIT = 8;

    interface SlowListener {
        void onTooSlow();
    }

    private final GradientDrawable top = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[] {Palette.SHADE_TOP, Palette.SHADE_TOP & 0x00FFFFFF});
    private final GradientDrawable bottom = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[] {Palette.SHADE_BOTTOM & 0x00FFFFFF, Palette.SHADE_BOTTOM});
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Drawable wallpaper;
    private SceneDrawable scene;
    private boolean wantAnimation;
    private boolean running;
    private long loopStart;
    private long lastTick;
    private int slowTicks;
    private SlowListener slowListener;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running || scene == null) return;
            long now = SystemClock.uptimeMillis();
            slowTicks = (now - lastTick > SLOW_GAP_MS && lastTick != 0) ? slowTicks + 1 : 0;
            lastTick = now;
            if (slowTicks >= SLOW_LIMIT) {
                stopAnimation();
                scene.setPhase(0f);
                if (slowListener != null) slowListener.onTooSlow();
                return;
            }
            scene.setPhase(SceneMath.phaseAt(now - loopStart));
            invalidate();
            handler.postDelayed(this, TICK_MS);
        }
    };

    WallpaperView(Context context) {
        super(context);
        setFocusable(false);
    }

    void setSlowListener(SlowListener listener) {
        slowListener = listener;
    }

    void setWallpaper(Drawable drawable) {
        stopAnimation();
        wallpaper = drawable;
        scene = drawable instanceof SceneDrawable ? (SceneDrawable) drawable : null;
        if (getWidth() > 0) wallpaper.setBounds(0, 0, getWidth(), getHeight());
        invalidate();
        if (wantAnimation) startAnimation();
    }

    /** Whether the wallpaper should move. Takes effect now if the view is shown, else when it is. */
    void setAnimated(boolean animated) {
        wantAnimation = animated;
        if (animated) startAnimation();
        else stopAnimation();
    }

    void startAnimation() {
        if (!wantAnimation || scene == null || running || !isShown()) return;
        running = true;
        slowTicks = 0;
        lastTick = 0;
        loopStart = SystemClock.uptimeMillis();
        handler.post(tick);
    }

    void stopAnimation() {
        running = false;
        handler.removeCallbacks(tick);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (wallpaper != null) wallpaper.setBounds(0, 0, w, h);
        top.setBounds(0, 0, w, h * 2 / 5);
        bottom.setBounds(0, h * 55 / 100, w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (wallpaper != null) wallpaper.draw(canvas);
        top.draw(canvas);
        bottom.draw(canvas);
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnimation();
        super.onDetachedFromWindow();
    }
}
