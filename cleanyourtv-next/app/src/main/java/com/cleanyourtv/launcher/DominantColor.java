package com.cleanyourtv.launcher;

/**
 * Picks the colour that best stands for an icon: the most vivid hue, not the average (an icon on a white
 * or black square would otherwise average to grey). Pure Java, so it is tested without Android.
 */
final class DominantColor {
    private DominantColor() { }

    private static final int BINS = 12;
    private static final float MIN_SATURATION = 0.25f;
    private static final float MIN_VALUE = 0.25f;
    /** Share of pixels that must be vivid before the icon counts as coloured at all. */
    private static final float MIN_COVERAGE = 0.03f;

    /** argb: pixels of a small copy of the icon. fallback: returned when no hue stands out. */
    static int pick(int[] argb, int fallback) {
        if (argb == null || argb.length == 0) return fallback;
        double[] weight = new double[BINS];
        double[] sumR = new double[BINS];
        double[] sumG = new double[BINS];
        double[] sumB = new double[BINS];
        float[] hsv = new float[3];
        for (int pixel : argb) {
            if ((pixel >>> 24) < 128) continue;
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;
            toHsv(r, g, b, hsv);
            if (hsv[1] < MIN_SATURATION || hsv[2] < MIN_VALUE) continue;
            int bin = Math.min(BINS - 1, (int) (hsv[0] / (360f / BINS)));
            double w = hsv[1] * hsv[2];
            weight[bin] += w;
            sumR[bin] += r * w;
            sumG[bin] += g * w;
            sumB[bin] += b * w;
        }
        int best = 0;
        for (int i = 1; i < BINS; i++) {
            if (weight[i] > weight[best]) best = i;
        }
        if (weight[best] < MIN_COVERAGE * argb.length) return fallback;
        int r = (int) Math.round(sumR[best] / weight[best]);
        int g = (int) Math.round(sumG[best] / weight[best]);
        int b = (int) Math.round(sumB[best] / weight[best]);
        toHsv(r, g, b, hsv);
        // A glow must read as light: lift dull colours, keep the hue.
        hsv[1] = Math.max(hsv[1], 0.55f);
        hsv[2] = Math.max(hsv[2], 0.80f);
        return 0xFF000000 | fromHsv(hsv[0], hsv[1], hsv[2]);
    }

    static void toHsv(int r, int g, int b, float[] out) {
        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;
        float hue;
        if (delta == 0f) hue = 0f;
        else if (max == rf) hue = 60f * (((gf - bf) / delta) % 6f);
        else if (max == gf) hue = 60f * (((bf - rf) / delta) + 2f);
        else hue = 60f * (((rf - gf) / delta) + 4f);
        if (hue < 0f) hue += 360f;
        out[0] = hue;
        out[1] = max == 0f ? 0f : delta / max;
        out[2] = max;
    }

    static int fromHsv(float hue, float sat, float val) {
        float c = val * sat;
        float x = c * (1f - Math.abs((hue / 60f) % 2f - 1f));
        float m = val - c;
        float r;
        float g;
        float b;
        int sector = (int) (hue / 60f) % 6;
        switch (sector) {
            case 0: r = c; g = x; b = 0; break;
            case 1: r = x; g = c; b = 0; break;
            case 2: r = 0; g = c; b = x; break;
            case 3: r = 0; g = x; b = c; break;
            case 4: r = x; g = 0; b = c; break;
            default: r = c; g = 0; b = x; break;
        }
        int ri = Math.round((r + m) * 255f);
        int gi = Math.round((g + m) * 255f);
        int bi = Math.round((b + m) * 255f);
        return (ri << 16) | (gi << 8) | bi;
    }
}
