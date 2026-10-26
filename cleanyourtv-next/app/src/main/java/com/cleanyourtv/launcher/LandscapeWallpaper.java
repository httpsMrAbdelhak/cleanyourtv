package com.cleanyourtv.launcher;

import android.content.Context;
import android.graphics.drawable.Drawable;

/** A painted landscape wallpaper. Still: its beauty is in the layers, not in motion. */
final class LandscapeWallpaper implements WallpaperSource {
    private final String id;
    private final int label;
    private final LandscapeSpec spec;

    LandscapeWallpaper(String id, int label, LandscapeSpec spec) {
        this.id = id;
        this.label = label;
        this.spec = spec;
    }

    @Override public String id() { return id; }

    @Override public int label() { return label; }

    @Override public boolean light() { return spec.light; }

    @Override public boolean animatable() { return false; }

    LandscapeSpec spec() { return spec; }

    @Override public Drawable load(Context context) {
        return new LandscapeDrawable(spec);
    }
}
