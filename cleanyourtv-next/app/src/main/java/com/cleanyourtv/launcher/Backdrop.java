package com.cleanyourtv.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The frosted copy of the wallpaper that glass panels sample from. The wallpaper is drawn once into a small
 * bitmap and blurred once; a panel then paints the part of it that lies behind itself. That gives the look of
 * real background blur at the price of one 320x180 bitmap, with nothing recomputed per frame.
 */
final class Backdrop {
    private Backdrop() { }

    private static final int W = 320;
    private static final int H = 180;
    private static final int RADIUS = 5;
    private static final int PASSES = 3;

    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "backdrop");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static volatile Bitmap blurred;
    private static volatile int version;
    private static volatile int generation;
    private static int screenW = 1920;
    private static int screenH = 1080;

    static Bitmap bitmap() {
        return blurred;
    }

    static int version() {
        return version;
    }

    static float scaleX() {
        return screenW / (float) W;
    }

    static float scaleY() {
        return screenH / (float) H;
    }

    /** The window size the blurred bitmap stands for. Call before rebuild. */
    static void setScreen(int width, int height) {
        screenW = Math.max(1, width);
        screenH = Math.max(1, height);
    }

    /** Drops the bitmap: glass panels fall back to their flat look until the next rebuild. */
    static void release() {
        generation++;
        blurred = null;
        version++;
    }

    /**
     * Builds the frosted copy off the main thread, then repaints the glass panels under root. A newer call
     * cancels the effect of an older one that is still running.
     */
    static void rebuild(final Context context, final Prefs prefs, final View root) {
        final Context app = context.getApplicationContext();
        final int mine = ++generation;
        if (!prefs.effects().frostedGlass()) {
            blurred = null;
            version++;
            repaint(root);
            return;
        }
        POOL.execute(() -> {
            Drawable source;
            try {
                source = Wallpaper.load(app, prefs);
            } catch (RuntimeException | OutOfMemoryError e) {
                return;
            }
            Bitmap small = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
            source.setBounds(0, 0, W, H);
            source.draw(new Canvas(small));
            int[] pixels = new int[W * H];
            small.getPixels(pixels, 0, W, 0, 0, W, H);
            for (int pass = 0; pass < PASSES; pass++) BoxBlur.blur(pixels, W, H, RADIUS);
            small.setPixels(pixels, 0, W, 0, 0, W, H);
            if (mine != generation) {
                small.recycle();
                return;
            }
            MAIN.post(() -> {
                if (mine != generation) return;
                blurred = small;
                version++;
                repaint(root);
            });
        });
    }

    /** Glass panels read the backdrop when they draw: ask every view under root to draw again. */
    static void repaint(View view) {
        view.invalidate();
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) repaint(group.getChildAt(i));
        }
    }
}
