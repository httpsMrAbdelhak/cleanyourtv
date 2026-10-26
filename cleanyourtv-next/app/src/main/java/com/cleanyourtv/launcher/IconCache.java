package com.cleanyourtv.launcher;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * App icons, drawn once at the size they are shown and kept in a small byte-bounded cache. The drawing
 * happens on one background thread, so scrolling never waits for a PackageManager call. It also finds the
 * colour that stands for an icon (see DominantColor), once per app, for the dock's ambient light.
 */
final class IconCache {
    private static final int MAX_BYTES = 6 * 1024 * 1024;
    private static final int SAMPLE = 24;
    private static IconCache instance;

    static synchronized IconCache get() {
        if (instance == null) instance = new IconCache();
        return instance;
    }

    private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(MAX_BYTES) {
        @Override protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };
    private final Map<String, Integer> colors = new HashMap<>();
    private final ExecutorService pool = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "icons");
        thread.setDaemon(true);
        return thread;
    });
    private final Handler main = new Handler(Looper.getMainLooper());

    private IconCache() { }

    /** Shows the icon in target: now if cached, otherwise as soon as it is drawn. */
    void load(final PackageManager pm, final String packageName, final int sizePx, final ImageView target) {
        final String key = packageName + "@" + sizePx;
        target.setTag(key);
        Bitmap hit = cache.get(key);
        if (hit != null) {
            target.setImageBitmap(hit);
            return;
        }
        target.setImageDrawable(null);
        pool.execute(() -> {
            final Bitmap bitmap = render(pm, packageName, sizePx);
            if (bitmap == null) return;
            final int color = dominant(bitmap);
            cache.put(key, bitmap);
            main.post(() -> {
                colors.put(packageName, color);
                if (key.equals(target.getTag())) target.setImageBitmap(bitmap);
            });
        });
    }

    /**
     * Shows a wide tile for the app: its TV banner when it ships one (Netflix, YouTube and most streaming
     * apps do), else its icon centred on a gradient of its own colour with its name underneath.
     */
    void loadBanner(final PackageManager pm, final String packageName, final String label,
                    final int width, final int height, final ImageView target) {
        final String key = packageName + "@b" + width + "x" + height;
        target.setTag(key);
        Bitmap hit = cache.get(key);
        if (hit != null) {
            target.setImageBitmap(hit);
            return;
        }
        target.setImageDrawable(null);
        pool.execute(() -> {
            final Bitmap bitmap = renderBanner(pm, packageName, label, width, height);
            if (bitmap == null) return;
            final int color = dominant(bitmap);
            cache.put(key, bitmap);
            main.post(() -> {
                colors.put(packageName, color);
                if (key.equals(target.getTag())) target.setImageBitmap(bitmap);
            });
        });
    }

    private static Bitmap renderBanner(PackageManager pm, String packageName, String label, int w, int h) {
        try {
            Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(out);
            Drawable banner = null;
            try {
                Intent launch = pm.getLeanbackLaunchIntentForPackage(packageName);
                if (launch != null) banner = pm.getActivityBanner(launch);
                if (banner == null) banner = pm.getApplicationBanner(packageName);
            } catch (PackageManager.NameNotFoundException | RuntimeException e) {
                banner = null;
            }
            if (banner != null) {
                drawCover(canvas, banner, w, h);
                return out;
            }
            Drawable icon = pm.getApplicationIcon(packageName);
            int base = dominant(drawSquare(icon, 48));
            int top = base == 0 ? 0xFF5A6070 : lift(base, 0.95f);
            int bottom = base == 0 ? 0xFF2F333D : lift(base, 0.55f);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(new LinearGradient(0, 0, w, h, top, bottom, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);
            int iconSize = Math.round(h * 0.5f);
            icon.setBounds((w - iconSize) / 2, Math.round(h * 0.10f), (w + iconSize) / 2, Math.round(h * 0.10f) + iconSize);
            icon.draw(canvas);
            paint.setColor(0xFFFFFFFF);
            paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(h * 0.15f);
            String text = label;
            while (text.length() > 1 && paint.measureText(text) > w * 0.9f) text = text.substring(0, text.length() - 1);
            if (!text.equals(label)) text = text.trim() + "\u2026";
            canvas.drawText(text, w / 2f, h * 0.90f, paint);
            return out;
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            return null;
        }
    }

    private static void drawCover(Canvas canvas, Drawable drawable, int w, int h) {
        int iw = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : w;
        int ih = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : h;
        float scale = Math.max(w / (float) iw, h / (float) ih);
        canvas.save();
        canvas.translate((w - iw * scale) / 2f, (h - ih * scale) / 2f);
        canvas.scale(scale, scale);
        drawable.setBounds(0, 0, iw, ih);
        drawable.draw(canvas);
        canvas.restore();
    }

    private static Bitmap drawSquare(Drawable icon, int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        icon.setBounds(0, 0, size, size);
        icon.draw(new Canvas(bitmap));
        return bitmap;
    }

    /** The colour with its brightness scaled: 0.95 keeps it vivid, 0.55 darkens it for the far corner. */
    private static int lift(int color, float value) {
        float[] hsv = new float[3];
        DominantColor.toHsv((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, hsv);
        return 0xFF000000 | DominantColor.fromHsv(hsv[0], Math.min(1f, hsv[1] * 0.95f), Math.min(1f, hsv[2] * value));
    }

    /** The colour of the app's icon, or fallback while the icon is not drawn yet. Main thread only. */
    int colorOf(String packageName, int fallback) {
        Integer color = colors.get(packageName);
        return color == null ? fallback : color;
    }

    /** Samples a 24x24 copy: 576 pixels, whatever the icon size. A 0 means "no vivid colour". */
    private static int dominant(Bitmap bitmap) {
        Bitmap small = Bitmap.createScaledBitmap(bitmap, SAMPLE, SAMPLE, true);
        int[] pixels = new int[SAMPLE * SAMPLE];
        small.getPixels(pixels, 0, SAMPLE, 0, 0, SAMPLE, SAMPLE);
        if (small != bitmap) small.recycle();
        return DominantColor.pick(pixels, 0);
    }

    private static Bitmap render(PackageManager pm, String packageName, int sizePx) {
        try {
            Drawable icon = pm.getApplicationIcon(packageName);
            Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
            icon.setBounds(0, 0, sizePx, sizePx);
            icon.draw(new Canvas(bitmap));
            return bitmap;
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            return null;
        }
    }

    void shrink() {
        cache.trimToSize(MAX_BYTES / 2);
    }

    void clear() {
        cache.evictAll();
    }
}
