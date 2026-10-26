package com.cleanyourtv.launcher;

/** A wallpaper as data: a dark base gradient and a few big soft lights that may drift. Pure Java. */
final class SceneSpec {
    static final class Blob {
        final float x;
        final float y;
        /** Radius as a share of the width. */
        final float radius;
        /** Height of the light relative to its width: below 1 makes a flattened glow. */
        final float squash;
        final int color;
        /** How far the light drifts, as a share of the width, and how many loops it makes per cycle. */
        final float driftX;
        final float driftY;
        final int speedX;
        final int speedY;
        final float offset;

        Blob(float x, float y, float radius, float squash, int color,
             float driftX, float driftY, int speedX, int speedY, float offset) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.squash = squash;
            this.color = color;
            this.driftX = driftX;
            this.driftY = driftY;
            this.speedX = speedX;
            this.speedY = speedY;
            this.offset = offset;
        }
    }

    final int top;
    final int bottom;
    /** A pale wallpaper: the interface then uses dark text on light glass. */
    final boolean light;
    final Blob[] blobs;

    SceneSpec(boolean light, int top, int bottom, Blob... blobs) {
        this.light = light;
        this.top = top;
        this.bottom = bottom;
        this.blobs = blobs;
    }
}
