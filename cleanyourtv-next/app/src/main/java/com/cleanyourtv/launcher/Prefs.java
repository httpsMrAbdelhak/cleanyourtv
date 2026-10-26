package com.cleanyourtv.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;

/** Everything the user can change, in one small SharedPreferences file. */
final class Prefs {
    private static final String FILE = "cleanyourtv";
    private static final String DOCK = "dock";
    private static final String ACCENT = "accent";
    private static final String WALLPAPER = "wallpaper";
    private static final String CLOCK = "clock";
    private static final String NAME = "name";
    private static final String NON_TV = "non_tv_apps";

    private final SharedPreferences prefs;

    Prefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** False until the dock was saved once: the first run fills it from DockDefaults. */
    boolean dockConfigured() {
        return prefs.contains(DOCK);
    }

    DockModel dock() {
        return DockModel.parse(prefs.getString(DOCK, ""));
    }

    void saveDock(DockModel dock) {
        prefs.edit().putString(DOCK, dock.serialize()).apply();
    }

    void saveDock(List<String> packages) {
        saveDock(new DockModel(packages));
    }

    Accents.Option accent() {
        return Accents.byId(prefs.getString(ACCENT, ""));
    }

    void setAccent(String id) {
        prefs.edit().putString(ACCENT, id).apply();
    }

    WallpaperSource wallpaper() {
        return Wallpapers.byId(prefs.getString(WALLPAPER, ""));
    }

    void setWallpaper(String id) {
        prefs.edit().putString(WALLPAPER, id).apply();
    }

    ClockMode clockMode() {
        return ClockMode.fromId(prefs.getString(CLOCK, ""));
    }

    void setClockMode(ClockMode mode) {
        prefs.edit().putString(CLOCK, mode.id).apply();
    }

    String name() {
        return prefs.getString(NAME, "");
    }

    void setName(String name) {
        prefs.edit().putString(NAME, name == null ? "" : name.trim()).apply();
    }

    /** Apps with only a phone launcher entry. On by default: a sideloaded app often lacks the TV entry. */
    boolean includeNonTvApps() {
        return prefs.getBoolean(NON_TV, true);
    }

    void setIncludeNonTvApps(boolean include) {
        prefs.edit().putBoolean(NON_TV, include).apply();
    }
}
