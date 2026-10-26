package com.cleanyourtv.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Posters and backdrops from the network, shown progressively. Three layers keep the 1 GB TV calm: a
 * byte-bounded memory cache, a disk cache with a size cap, and a small thread pool. A view that was reused
 * for another picture while a download ran simply ignores the late answer (its tag no longer matches).
 */
final class ImageLoader {
    private static final int MEMORY_BYTES = 12 * 1024 * 1024;
    private static final long DISK_BYTES = 60L * 1024 * 1024;
    private static final int TRIM_EVERY = 20;

    private static ImageLoader instance;

    static synchronized ImageLoader get(Context context) {
        if (instance == null) instance = new ImageLoader(context.getApplicationContext());
        return instance;
    }

    private final Context context;
    private final File dir;
    private final LruCache<String, Bitmap> memory = new LruCache<String, Bitmap>(MEMORY_BYTES) {
        @Override protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };
    private final ExecutorService pool = Executors.newFixedThreadPool(3, runnable -> {
        Thread thread = new Thread(runnable, "images");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicInteger writes = new AtomicInteger();

    private ImageLoader(Context context) {
        this.context = context;
        dir = new File(context.getCacheDir(), "img");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
    }

    /**
     * Shows url in target when ready. targetWidth bounds the decoded size. light decodes to 16 bits,
     * half the memory. fade: fade the picture in rather than popping it. The view keeps what it shows
     * until the new picture arrives, so there is never a blank flash between two backdrops.
     */
    void load(final String url, final ImageView target, final int targetWidth, final boolean light, final boolean fade) {
        final String key = url + "#" + targetWidth + (light ? "l" : "h");
        target.setTag(key);
        Bitmap hit = memory.get(key);
        if (hit != null) {
            target.setImageBitmap(hit);
            return;
        }
        pool.execute(() -> {
            Bitmap bitmap = decodeCached(url, targetWidth, light);
            if (bitmap == null) return;
            memory.put(key, bitmap);
            main.post(() -> {
                if (!key.equals(target.getTag())) return;
                target.setImageBitmap(bitmap);
                if (fade) {
                    target.setAlpha(0f);
                    target.animate().alpha(1f).setDuration(220).start();
                }
            });
        });
    }

    /** Frees the view's picture. Its bitmap stays in the memory cache until evicted, then is collected. */
    void release(ImageView target) {
        target.setTag(null);
        target.setImageDrawable(null);
    }

    void trimMemory() {
        memory.evictAll();
    }

    private Bitmap decodeCached(String url, int targetWidth, boolean light) {
        File file = new File(dir, hash(url));
        if (!file.isFile()) {
            try {
                byte[] data = fetch(url);
                File tmp = new File(dir, file.getName() + ".tmp");
                try (FileOutputStream out = new FileOutputStream(tmp)) {
                    out.write(data);
                }
                if (!tmp.renameTo(file)) return null;
                if (writes.incrementAndGet() % TRIM_EVERY == 0) trimDisk();
            } catch (IOException | RuntimeException e) {
                return null;
            }
        } else {
            //noinspection ResultOfMethodCallIgnored
            file.setLastModified(System.currentTimeMillis());
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = WallpaperStore.sampleSize(bounds.outWidth, bounds.outHeight, targetWidth, 1);
        if (light) options.inPreferredConfig = Bitmap.Config.RGB_565;
        try {
            return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        } catch (OutOfMemoryError e) {
            memory.evictAll();
            return null;
        }
    }

    /**
     * The picture's bytes: an https address is downloaded; a content:// address (what an app publishes to the
     * TV provider) is read from its own provider. Anything else is refused: no file:// and no plain http.
     */
    private byte[] fetch(String uri) throws IOException {
        if (uri.startsWith("https://")) return Http.image(uri);
        if (uri.startsWith("content://") || uri.startsWith("android.resource://")) {
            try (InputStream in = context.getContentResolver().openInputStream(android.net.Uri.parse(uri))) {
                if (in == null) throw new IOException("cannot open");
                ByteArrayOutputStream out = new ByteArrayOutputStream(32 * 1024);
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    if (out.size() > 4 * 1024 * 1024) throw new IOException("picture too large");
                }
                return out.toByteArray();
            } catch (SecurityException e) {
                throw new IOException("not allowed");
            }
        }
        throw new IOException("unsupported address");
    }

    /** Deletes the least recently used files until the folder is under its cap. */
    private void trimDisk() {
        File[] files = dir.listFiles();
        if (files == null) return;
        long total = 0;
        for (File file : files) total += file.length();
        if (total <= DISK_BYTES) return;
        java.util.Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        for (File file : files) {
            if (total <= DISK_BYTES * 3 / 4) break;
            total -= file.length();
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    private static String hash(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(text.hashCode());
        }
    }
}
