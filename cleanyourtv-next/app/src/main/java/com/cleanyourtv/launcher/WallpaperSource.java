package com.cleanyourtv.launcher;

import android.content.Context;
import android.graphics.drawable.Drawable;

/**
 * A wallpaper the user can pick. This is the extension point: implement it and add one line to
 * {@link Wallpapers#ALL}. The gallery lists whatever is registered there; nothing else needs to change.
 */
interface WallpaperSource {
    /** Stable name saved in the preferences. Never rename one that shipped. */
    String id();

    /** String resource shown in the gallery. */
    int label();

    /** True when the wallpaper is pale: the interface then uses dark text on light glass. */
    boolean light();

    /** True when the drawable is a SceneDrawable that can drift (the animated wallpaper option). */
    boolean animatable();

    /** Builds a fresh drawable. May be called off the main thread: it must not touch views. */
    Drawable load(Context context);
}
