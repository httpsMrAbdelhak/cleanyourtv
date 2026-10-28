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
    private static final String ANIMATED = "animated_wallpaper";
    private static final String EFFECTS = "effects";
    private static final String CLOCK = "clock";
    private static final String NAME = "name";
    private static final String NON_TV = "non_tv_apps";
    private static final String CITY_NAME = "city_name";
    private static final String CITY_LAT = "city_lat";
    private static final String CITY_LON = "city_lon";
    private static final String SOURCE = "source_package";
    private static final String CUSTOM_LIGHT = "custom_light";
    private static final String TMDB_KEY = "tmdb_key";
    private static final String STATUS_PREFIX = "status_";

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

    /** The saved wallpaper id: a built-in scene, or Wallpapers.CUSTOM for a picture from the device. */
    String wallpaperId() {
        String id = prefs.getString(WALLPAPER, "");
        return Wallpapers.CUSTOM.equals(id) ? id : Wallpapers.byId(id).id();
    }

    void setWallpaperId(String id) {
        prefs.edit().putString(WALLPAPER, id).apply();
    }

    /** Whether the picture from the device is pale (measured when it was saved). */
    boolean customWallpaperLight() {
        return prefs.getBoolean(CUSTOM_LIGHT, false);
    }

    void setCustomWallpaperLight(boolean light) {
        prefs.edit().putBoolean(CUSTOM_LIGHT, light).apply();
    }

    /** Off by default: a still wallpaper is the safe choice on a 1 GB stick. */
    boolean animatedWallpaper() {
        return prefs.getBoolean(ANIMATED, false);
    }

    void setAnimatedWallpaper(boolean on) {
        prefs.edit().putBoolean(ANIMATED, on).apply();
    }

    Effects effects() {
        return Effects.fromId(prefs.getString(EFFECTS, ""));
    }

    void setEffects(Effects effects) {
        prefs.edit().putString(EFFECTS, effects.id).apply();
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

    /** The weather city, chosen by name in the settings. No location is ever read from the device. */
    boolean hasCity() {
        return prefs.contains(CITY_LAT) && !cityName().isEmpty();
    }

    String cityName() {
        return prefs.getString(CITY_NAME, "");
    }

    double cityLatitude() {
        return Double.longBitsToDouble(prefs.getLong(CITY_LAT, 0));
    }

    double cityLongitude() {
        return Double.longBitsToDouble(prefs.getLong(CITY_LON, 0));
    }

    void setCity(String name, double latitude, double longitude) {
        prefs.edit().putString(CITY_NAME, name)
                .putLong(CITY_LAT, Double.doubleToLongBits(latitude))
                .putLong(CITY_LON, Double.doubleToLongBits(longitude)).apply();
    }

    /** The app whose home-screen content the panel shows; empty means "find Nuvio". */
    String sourcePackage() {
        return prefs.getString(SOURCE, "");
    }

    void setSourcePackage(String packageName) {
        prefs.edit().putString(SOURCE, packageName == null ? "" : packageName).apply();
    }

    /** The TMDB key typed in Settings (a v3 key or a v4 read token); empty means the build's key, if any. */
    String tmdbKey() {
        return prefs.getString(TMDB_KEY, "");
    }

    void setTmdbKey(String key) {
        prefs.edit().putString(TMDB_KEY, key == null ? "" : key.replaceAll("\\s+", "")).apply();
    }

    /** The last outcome of an integration ("tmdb", "weather"), shown on the settings screen. */
    String status(String integration) {
        return prefs.getString(STATUS_PREFIX + integration, "");
    }

    void setStatus(String integration, String text) {
        prefs.edit().putString(STATUS_PREFIX + integration, text).apply();
    }
}
