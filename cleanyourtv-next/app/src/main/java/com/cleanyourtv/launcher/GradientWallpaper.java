package com.cleanyourtv.launcher;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

/** A diagonal gradient: no bitmap, no memory to speak of, and sharp on any resolution. */
final class GradientWallpaper implements WallpaperSource {
    private final String id;
    private final int label;
    private final int[] colors;

    GradientWallpaper(String id, int label, int... colors) {
        this.id = id;
        this.label = label;
        this.colors = colors;
    }

    @Override public String id() { return id; }

    @Override public int label() { return label; }

    int[] colors() { return colors.clone(); }

    @Override public Drawable load(Context context) {
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
    }
}
