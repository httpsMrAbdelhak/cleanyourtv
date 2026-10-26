package com.cleanyourtv.launcher;

import static com.cleanyourtv.launcher.SceneSpec.Blob;

/** The registry of built-in wallpapers, plus the picture the user chose from the device. */
final class Wallpapers {
    private Wallpapers() { }

    /** Id saved when the wallpaper is a picture from the device (see WallpaperStore). */
    static final String CUSTOM = "custom";

    static final WallpaperSource[] ALL = {
            new SceneWallpaper("nebula", R.string.wallpaper_nebula, new SceneSpec(0xFF07060F, 0xFF0B0A1C,
                    new Blob(0.22f, 0.30f, 0.62f, 0.80f, 0xFF6A3DF0, 0.06f, 0.03f, 1, 1, 0.00f),
                    new Blob(0.82f, 0.18f, 0.46f, 0.75f, 0xFFC13CA8, 0.05f, 0.04f, 1, 2, 0.30f),
                    new Blob(0.62f, 0.95f, 0.70f, 0.55f, 0xFF1F5BFF, 0.07f, 0.02f, 1, 1, 0.60f))),
            new SceneWallpaper("aurora", R.string.wallpaper_aurora, new SceneSpec(0xFF04090C, 0xFF051418,
                    new Blob(0.20f, 0.85f, 0.66f, 0.60f, 0xFF14C8A0, 0.07f, 0.03f, 1, 1, 0.10f),
                    new Blob(0.75f, 0.55f, 0.52f, 0.85f, 0xFF2E7BFF, 0.06f, 0.04f, 1, 2, 0.45f),
                    new Blob(0.50f, 0.10f, 0.50f, 0.50f, 0xFF7B5CFF, 0.08f, 0.02f, 2, 1, 0.80f))),
            new SceneWallpaper("ember", R.string.wallpaper_ember, new SceneSpec(0xFF0B0504, 0xFF150707,
                    new Blob(0.78f, 0.95f, 0.66f, 0.55f, 0xFFFF6A1F, 0.06f, 0.02f, 1, 1, 0.20f),
                    new Blob(0.18f, 0.72f, 0.52f, 0.80f, 0xFFD1213B, 0.05f, 0.04f, 1, 2, 0.55f),
                    new Blob(0.50f, 1.05f, 0.40f, 0.60f, 0xFFFFB347, 0.07f, 0.02f, 2, 1, 0.90f))),
            new SceneWallpaper("ocean", R.string.wallpaper_ocean, new SceneSpec(0xFF030812, 0xFF041A2C,
                    new Blob(0.28f, 0.40f, 0.60f, 0.80f, 0xFF1E8BFF, 0.06f, 0.04f, 1, 1, 0.05f),
                    new Blob(0.82f, 0.85f, 0.55f, 0.60f, 0xFF1CE0E0, 0.07f, 0.02f, 1, 2, 0.40f),
                    new Blob(0.55f, 0.00f, 0.60f, 0.45f, 0xFF0A3FA8, 0.05f, 0.03f, 2, 1, 0.75f))),
            new SceneWallpaper("dusk", R.string.wallpaper_dusk, new SceneSpec(0xFF0A0712, 0xFF170B1D,
                    new Blob(0.25f, 0.40f, 0.60f, 0.85f, 0xFF7B3FD1, 0.06f, 0.03f, 1, 1, 0.15f),
                    new Blob(0.72f, 0.80f, 0.60f, 0.60f, 0xFFFF5E8A, 0.06f, 0.03f, 1, 2, 0.50f),
                    new Blob(0.90f, 0.40f, 0.30f, 0.90f, 0xFFFFA24A, 0.04f, 0.04f, 2, 1, 0.85f))),
            new SceneWallpaper("graphite", R.string.wallpaper_graphite, new SceneSpec(0xFF0A0B0D, 0xFF13151A,
                    new Blob(0.30f, 0.30f, 0.60f, 0.80f, 0xFF5A6578, 0.05f, 0.03f, 1, 1, 0.00f),
                    new Blob(0.80f, 0.85f, 0.55f, 0.60f, 0xFF2D3646, 0.06f, 0.02f, 1, 2, 0.50f))),
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
