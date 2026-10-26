package com.cleanyourtv.launcher;

/** Whether a picture is pale, so that the interface can pick dark or light text for it. Pure Java. */
final class Luma {
    private Luma() { }

    /** Mean luminance (0..255) of opaque ARGB pixels; transparent pixels are ignored. */
    static float mean(int[] argb) {
        double sum = 0;
        int counted = 0;
        for (int pixel : argb) {
            if ((pixel >>> 24) < 128) continue;
            sum += 0.299 * ((pixel >> 16) & 0xFF) + 0.587 * ((pixel >> 8) & 0xFF) + 0.114 * (pixel & 0xFF);
            counted++;
        }
        return counted == 0 ? 0f : (float) (sum / counted);
    }

    /** Pale enough that dark text reads on it. The threshold leans towards "dark": white text on a mid grey stays readable. */
    static boolean isLight(int[] argb) {
        return mean(argb) >= 150f;
    }
}
