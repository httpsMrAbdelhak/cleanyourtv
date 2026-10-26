package com.cleanyourtv.launcher;

/** The two effect levels. QUALITY: frosted glass and optional animated wallpapers. PERFORMANCE: flat and still. */
enum Effects {
    QUALITY("quality"),
    PERFORMANCE("performance");

    final String id;

    Effects(String id) {
        this.id = id;
    }

    boolean frostedGlass() {
        return this == QUALITY;
    }

    boolean allowsAnimatedWallpaper() {
        return this == QUALITY;
    }

    /** Smaller posters, 16-bit bitmaps: half the memory. */
    boolean lightImages() {
        return this == PERFORMANCE;
    }

    static Effects fromId(String id) {
        for (Effects effects : values()) {
            if (effects.id.equals(id)) return effects;
        }
        return QUALITY;
    }
}
