package com.cleanyourtv.launcher;

/**
 * A wallpaper painted as a landscape: a sky, a sun, a few mountain ranges that fade into the haze, and
 * optionally a lake and stars. Pure data; LandscapeDrawable draws it.
 */
final class LandscapeSpec {
    static final class Range {
        /** Where the foot of the range sits, as a share of the height, and how tall its peaks may be. */
        final float base;
        final float peaks;
        final int seed;
        final int color;

        Range(float base, float peaks, int seed, int color) {
            this.base = base;
            this.peaks = peaks;
            this.seed = seed;
            this.color = color;
        }
    }

    final boolean light;
    final int[] sky;
    final float[] skyStops;
    final float sunX;
    final float sunY;
    final float sunRadius;
    final int sunColor;
    final int glowColor;
    /** The colour mist takes between ranges. */
    final int mist;
    final Range[] ranges;
    /** The lake starts at this share of the height, or 0 for none. */
    final float lake;
    final boolean stars;
    /** Streaks of cloud lit by the sun: x, y, width as a share of the width, thickness. */
    final float[][] clouds;
    final int cloudColor;

    LandscapeSpec(boolean light, int[] sky, float[] skyStops, float sunX, float sunY, float sunRadius, int sunColor,
                  int glowColor, int mist, Range[] ranges, float lake, boolean stars, float[][] clouds, int cloudColor) {
        this.light = light;
        this.sky = sky;
        this.skyStops = skyStops;
        this.sunX = sunX;
        this.sunY = sunY;
        this.sunRadius = sunRadius;
        this.sunColor = sunColor;
        this.glowColor = glowColor;
        this.mist = mist;
        this.ranges = ranges;
        this.lake = lake;
        this.stars = stars;
        this.clouds = clouds;
        this.cloudColor = cloudColor;
    }

    /**
     * Mean luminance (0..255) of the upper half of the sky, where the title and the tabs are drawn: it decides
     * whether dark or light text suits the picture. The bright horizon at the bottom does not count.
     */
    float skyLuma() {
        int count = 0;
        for (float stop : skyStops) {
            if (stop <= 0.5f) count++;
        }
        int[] pixels = new int[Math.max(1, count)];
        int at = 0;
        for (int i = 0; i < sky.length && at < pixels.length; i++) {
            if (skyStops[i] <= 0.5f) pixels[at++] = sky[i];
        }
        return Luma.mean(pixels);
    }
}
