package com.cleanyourtv.launcher;

/** The registry of wallpapers. See {@link WallpaperSource} to add one. */
final class Wallpapers {
    private Wallpapers() { }

    static final WallpaperSource[] ALL = {
            new GradientWallpaper("aurora", R.string.wallpaper_aurora, 0xFF0F2027, 0xFF203A43, 0xFF2C5364),
            new GradientWallpaper("midnight", R.string.wallpaper_midnight, 0xFF0B0D12, 0xFF1A1F3A),
            new GradientWallpaper("graphite", R.string.wallpaper_graphite, 0xFF121212, 0xFF2A2A2E),
            new GradientWallpaper("dusk", R.string.wallpaper_dusk, 0xFF1B1035, 0xFF4A2260, 0xFF8C3F63),
            new GradientWallpaper("forest", R.string.wallpaper_forest, 0xFF0A1F1A, 0xFF14452F),
    };

    static int indexOf(String id) {
        for (int i = 0; i < ALL.length; i++) {
            if (ALL[i].id().equals(id)) return i;
        }
        return 0;
    }

    static WallpaperSource byId(String id) {
        return ALL[indexOf(id)];
    }
}
