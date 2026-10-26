package com.cleanyourtv.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The wallpaper picture from the device. It is chosen with the system file picker (no storage permission),
 * shrunk to screen size and copied into the app's own folder, so it survives a reboot and the original may
 * move or disappear.
 */
final class WallpaperStore {
    private WallpaperStore() { }

    private static final String FILE = "wallpaper.jpg";
    private static final int MAX_W = 1920;
    private static final int MAX_H = 1080;

    static File file(Context context) {
        return new File(context.getFilesDir(), FILE);
    }

    static boolean exists(Context context) {
        return file(context).isFile();
    }

    /**
     * Copies the picked picture, scaled down to at most 1920x1080. Throws when it cannot be read as an image.
     * Returns whether the picture is pale (the interface then switches to dark text).
     */
    static boolean save(Context context, Uri source) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(source)) {
            if (in == null) throw new IOException("cannot open");
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("not an image");
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_W, MAX_H);
        Bitmap bitmap;
        try (InputStream in = context.getContentResolver().openInputStream(source)) {
            if (in == null) throw new IOException("cannot open");
            bitmap = BitmapFactory.decodeStream(in, null, options);
        }
        if (bitmap == null) throw new IOException("decode failed");
        File tmp = new File(context.getFilesDir(), FILE + ".tmp");
        boolean light;
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            Bitmap small = Bitmap.createScaledBitmap(bitmap, 32, 18, true);
            int[] pixels = new int[32 * 18];
            small.getPixels(pixels, 0, 32, 0, 0, 32, 18);
            light = Luma.isLight(pixels);
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)) throw new IOException("encode failed");
        } finally {
            bitmap.recycle();
        }
        if (!tmp.renameTo(file(context))) throw new IOException("rename failed");
        return light;
    }

    /** The saved picture as a drawable, or null when there is none or it cannot be read. */
    static Drawable load(Context context, boolean lightImages) {
        if (!exists(context)) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        if (lightImages) options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeFile(file(context).getAbsolutePath(), options);
        return bitmap == null ? null : new CoverDrawable(bitmap);
    }

    /** Largest power of two that keeps the result at least as big as the wanted size. */
    static int sampleSize(int width, int height, int wantW, int wantH) {
        int sample = 1;
        while (width / (sample * 2) >= wantW && height / (sample * 2) >= wantH) sample *= 2;
        return sample;
    }

    static void clear(Context context) {
        //noinspection ResultOfMethodCallIgnored
        file(context).delete();
    }
}
