package com.cleanyourtv.launcher;

import android.content.Context;
import android.graphics.drawable.Drawable;

/** Resolves the saved wallpaper choice into a drawable, with the built-in scene as the fallback. */
final class Wallpaper {
    private Wallpaper() { }

    /** The drawable for the saved choice. Never null: a missing or broken picture falls back to the first scene. */
    static Drawable load(Context context, Prefs prefs) {
        if (Wallpapers.CUSTOM.equals(prefs.wallpaperId())) {
            Drawable picture = WallpaperStore.load(context, prefs.effects().lightImages());
            if (picture != null) return picture;
        }
        return Wallpapers.byId(prefs.wallpaperId()).load(context);
    }

    /** Whether the saved wallpaper is pale, so that the interface uses dark text on it. */
    static boolean isLight(Prefs prefs) {
        String id = prefs.wallpaperId();
        if (Wallpapers.CUSTOM.equals(id)) return prefs.customWallpaperLight();
        return Wallpapers.byId(id).light();
    }

    /** Whether the saved wallpaper may drift (a scene, not a picture) and the user allows it. */
    static boolean animated(Prefs prefs) {
        return prefs.animatedWallpaper() && prefs.effects().allowsAnimatedWallpaper()
                && !Wallpapers.CUSTOM.equals(prefs.wallpaperId()) && Wallpapers.byId(prefs.wallpaperId()).animatable();
    }
}
