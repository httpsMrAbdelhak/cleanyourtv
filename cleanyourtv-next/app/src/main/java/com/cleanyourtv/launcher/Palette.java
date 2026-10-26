package com.cleanyourtv.launcher;

/** The fixed dark palette. The accent colour is the only part the user changes. */
final class Palette {
    private Palette() { }

    static final int BACKGROUND = 0xFF05060A;
    static final int SURFACE = 0xFF14161C;
    static final int TEXT = 0xFFF5F6FA;
    static final int TEXT_DIM = 0xB3F5F6FA;
    static final int TEXT_FAINT = 0x80F5F6FA;
    /** Flat glass, for lists that scroll (a sampled backdrop would drift out of place). */
    static final int TILE = 0x24FFFFFF;
    static final int TILE_FOCUSED = 0x4DFFFFFF;
}
