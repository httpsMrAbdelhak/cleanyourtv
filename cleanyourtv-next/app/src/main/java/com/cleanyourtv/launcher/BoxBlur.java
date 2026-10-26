package com.cleanyourtv.launcher;

/**
 * A box blur on ARGB pixels, run on a small copy of the wallpaper (a few hundred pixels wide) to make the
 * frosted-glass backdrop. Three passes approximate a Gaussian. Never run on a full-size bitmap.
 */
final class BoxBlur {
    private BoxBlur() { }

    /** Blurs in place: a horizontal then a vertical pass, edges clamped. radius 0 changes nothing. */
    static void blur(int[] pixels, int width, int height, int radius) {
        if (radius <= 0 || width <= 0 || height <= 0) return;
        int[] line = new int[Math.max(width, height)];
        for (int y = 0; y < height; y++) pass(pixels, y * width, 1, width, radius, line);
        for (int x = 0; x < width; x++) pass(pixels, x, width, height, radius, line);
    }

    private static void pass(int[] px, int start, int step, int length, int radius, int[] out) {
        int window = 2 * radius + 1;
        long a = 0;
        long r = 0;
        long g = 0;
        long b = 0;
        for (int i = -radius; i <= radius; i++) {
            int c = px[start + clamp(i, length) * step];
            a += c >>> 24;
            r += (c >> 16) & 0xFF;
            g += (c >> 8) & 0xFF;
            b += c & 0xFF;
        }
        for (int i = 0; i < length; i++) {
            out[i] = ((int) (a / window) << 24) | ((int) (r / window) << 16)
                    | ((int) (g / window) << 8) | (int) (b / window);
            int leaving = px[start + clamp(i - radius, length) * step];
            int entering = px[start + clamp(i + radius + 1, length) * step];
            a += (entering >>> 24) - (leaving >>> 24);
            r += ((entering >> 16) & 0xFF) - ((leaving >> 16) & 0xFF);
            g += ((entering >> 8) & 0xFF) - ((leaving >> 8) & 0xFF);
            b += (entering & 0xFF) - (leaving & 0xFF);
        }
        for (int i = 0; i < length; i++) px[start + i * step] = out[i];
    }

    private static int clamp(int i, int length) {
        return i < 0 ? 0 : (i >= length ? length - 1 : i);
    }
}
