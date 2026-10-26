package com.cleanyourtv.launcher;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
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
