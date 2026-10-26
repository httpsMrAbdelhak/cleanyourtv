package com.cleanyourtv.launcher;

/** The drift of the animated wallpaper's light blobs: slow, and looping without a seam. */
final class SceneMath {
    private SceneMath() { }

    /** Length of one loop of the animation. */
    static final long LOOP_MS = 90_000L;

    /** phase in [0, 1). speed must be a whole number so that phase 1 lands where phase 0 started. */
    static float wave(float phase, int speed, float offset) {
        return (float) Math.sin(2 * Math.PI * (phase * speed + offset));
    }

    static float phaseAt(long elapsedMs) {
        return (float) (elapsedMs % LOOP_MS) / LOOP_MS;
    }
}
