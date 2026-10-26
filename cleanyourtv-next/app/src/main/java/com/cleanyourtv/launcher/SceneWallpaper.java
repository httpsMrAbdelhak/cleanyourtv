package com.cleanyourtv.launcher;

import android.content.Context;
import android.graphics.drawable.Drawable;

/** A wallpaper made of soft lights, drawn live: still by default, drifting when the animated option is on. */
final class SceneWallpaper implements WallpaperSource {
    private final String id;
    private final int label;
    private final SceneSpec spec;

    SceneWallpaper(String id, int label, SceneSpec spec) {
        this.id = id;
        this.label = label;
        this.spec = spec;
    }

    @Override public String id() { return id; }

    @Override public int label() { return label; }

    @Override public boolean animatable() { return true; }

    SceneSpec spec() { return spec; }

    @Override public Drawable load(Context context) {
        return new SceneDrawable(spec);
    }
}
