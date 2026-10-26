package com.cleanyourtv.launcher;

/** The outline of a mountain range: a smooth, repeatable height for any position, from a few layered waves. */
final class Ridge {
    private Ridge() { }

    private static final int OCTAVES = 5;

    /** Height in [0, 1] at x in [0, 1]. The same seed always gives the same outline. */
    static float height(float x, int seed) {
        double sum = 0;
        double amplitude = 1;
        double total = 0;
        double frequency = 1.3;
        for (int i = 0; i < OCTAVES; i++) {
            double phase = (seed * 0.618034 * (i + 1) + seed * 1.7 * i) % (2 * Math.PI);
            sum += amplitude * Math.sin(2 * Math.PI * x * frequency + phase);
            total += amplitude;
            amplitude *= 0.5;
            frequency *= 2.1;
        }
        return (float) ((sum / total + 1.0) / 2.0);
    }
}
