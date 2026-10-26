package com.cleanyourtv.launcher;

/**
 * The colours of the interface. The theme follows the wallpaper: a light wallpaper gives dark text on pale
 * glass (the clean, white look), a dark wallpaper gives light text on smoked glass. Call apply before
 * building any view; the fields are read when a view is built, so a change of theme rebuilds the screen.
 */
final class Palette {
    private Palette() { }

    static boolean light = true;

    static int BACKGROUND;
    static int SURFACE;
    static int TEXT;
    static int TEXT_DIM;
    static int TEXT_FAINT;
    /** Flat glass for lists that scroll (a sampled backdrop would drift out of place). */
    static int TILE;
    static int TILE_FOCUSED;
    /** The ring around the focused item: the accent colour on a light theme, white on a dark one. */
    static int RING;
    static int PLACEHOLDER;
    static int DIALOG;
    static int SHADE_TOP;
    static int SHADE_BOTTOM;

    // Liquid Glass (see GlassDrawable)
    static int GLASS_FILL;
    static int GLASS_FILL_BRIGHT;
    static int GLASS_SHADE;
    static int TINT_TOP;
    static int TINT_BOTTOM;
    static int TINT_TOP_BRIGHT;
    static int TINT_BOTTOM_BRIGHT;
    static int EDGE_LIT;
    static int EDGE_LIT_BRIGHT;
    static int EDGE_MID;
    static int EDGE_END;
    static int HAIRLINE;

    static {
        apply(true, 0xFF2F6BFF);
    }

    static boolean apply(Prefs prefs) {
        return apply(Wallpaper.isLight(prefs), prefs.accent().color);
    }

    /** Returns true when the light/dark theme changed (the caller must rebuild its views). */
    static boolean apply(boolean isLight, int accent) {
        boolean changed = isLight != light;
        light = isLight;
        if (isLight) {
            BACKGROUND = 0xFFEEF1F6;
            SURFACE = 0xFFFFFFFF;
            TEXT = 0xFF14171F;
            TEXT_DIM = 0xFF4F5767;
            TEXT_FAINT = 0xFF858E9E;
            TILE = 0x70FFFFFF;
            TILE_FOCUSED = 0xD9FFFFFF;
            RING = accent;
            PLACEHOLDER = 0x14000000;
            DIALOG = 0xF7FFFFFF;
            SHADE_TOP = 0x66FFFFFF;
            SHADE_BOTTOM = 0x80FFFFFF;
            GLASS_FILL = 0xA6FFFFFF;
            GLASS_FILL_BRIGHT = 0xD9FFFFFF;
            GLASS_SHADE = 0x40FFFFFF;
            TINT_TOP = 0x66FFFFFF;
            TINT_BOTTOM = 0x1AFFFFFF;
            TINT_TOP_BRIGHT = 0x99FFFFFF;
            TINT_BOTTOM_BRIGHT = 0x40FFFFFF;
            EDGE_LIT = 0xFFFFFFFF;
            EDGE_LIT_BRIGHT = 0xFFFFFFFF;
            EDGE_MID = 0x40FFFFFF;
            EDGE_END = 0xB3FFFFFF;
            HAIRLINE = 0x24202C40;
        } else {
            BACKGROUND = 0xFF05060A;
            SURFACE = 0xFF14161C;
            TEXT = 0xFFF5F6FA;
            TEXT_DIM = 0xB3F5F6FA;
            TEXT_FAINT = 0x80F5F6FA;
            TILE = 0x24FFFFFF;
            TILE_FOCUSED = 0x4DFFFFFF;
            RING = 0xF2FFFFFF;
            PLACEHOLDER = 0x1FFFFFFF;
            DIALOG = 0xF2181B22;
            SHADE_TOP = 0x66000000;
            SHADE_BOTTOM = 0xB0000000;
            GLASS_FILL = 0x4A1E222C;
            GLASS_FILL_BRIGHT = 0x66303644;
            GLASS_SHADE = 0x38000000;
            TINT_TOP = 0x2EFFFFFF;
            TINT_BOTTOM = 0x0DFFFFFF;
            TINT_TOP_BRIGHT = 0x4DFFFFFF;
            TINT_BOTTOM_BRIGHT = 0x1AFFFFFF;
            EDGE_LIT = 0x80FFFFFF;
            EDGE_LIT_BRIGHT = 0xCCFFFFFF;
            EDGE_MID = 0x14FFFFFF;
            EDGE_END = 0x3DFFFFFF;
            HAIRLINE = 0x00000000;
        }
        return changed;
    }
}
