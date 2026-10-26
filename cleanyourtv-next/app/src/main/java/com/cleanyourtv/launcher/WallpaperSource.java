package com.cleanyourtv.launcher;

import android.content.Context;
import android.graphics.drawable.Drawable;

/**
 * A wallpaper the user can pick. This is the extension point: implement it (a picture from a folder, a
 * slow animation, ...) and add one line to {@link Wallpapers#ALL}. The settings screen lists whatever
 * is registered there; nothing else needs to change.
 */
interface WallpaperSource {
    /** Stable name saved in the preferences. Never rename one that shipped. */
    String id();

    /** String resource shown in the settings. */
    int label();

    /** Builds the drawable. Called on the main thread each time the home screen resumes: keep it cheap. */
    Drawable load(Context context);
}
