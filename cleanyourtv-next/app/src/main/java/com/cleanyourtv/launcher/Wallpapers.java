package com.cleanyourtv.launcher;

import static com.cleanyourtv.launcher.LandscapeSpec.Range;
import static com.cleanyourtv.launcher.SceneSpec.Blob;

/** The registry of built-in wallpapers, plus the picture the user chose from the device. */
final class Wallpapers {
    private Wallpapers() { }

    /** Id saved when the wallpaper is a picture from the device (see WallpaperStore). */
    static final String CUSTOM = "custom";

    static final WallpaperSource[] ALL = {
            new SceneWallpaper("pearl", R.string.wallpaper_pearl, new SceneSpec(true, 0xFFF6F8FC, 0xFFE4E9F2,
                    new Blob(0.20f, 0.25f, 0.55f, 0.80f, 0xFFB9D2FF, 0.06f, 0.03f, 1, 1, 0.00f),
                    new Blob(0.85f, 0.20f, 0.45f, 0.75f, 0xFFE3CCFF, 0.05f, 0.04f, 1, 2, 0.30f),
                    new Blob(0.60f, 0.95f, 0.65f, 0.55f, 0xFFFFD9C2, 0.07f, 0.02f, 1, 1, 0.60f))),
            new SceneWallpaper("cloud", R.string.wallpaper_cloud, new SceneSpec(true, 0xFFF2F6FA, 0xFFDDE7F0,
                    new Blob(0.30f, 0.35f, 0.60f, 0.70f, 0xFFA8E0F0, 0.06f, 0.03f, 1, 1, 0.10f),
                    new Blob(0.80f, 0.75f, 0.55f, 0.65f, 0xFFC8D8FF, 0.06f, 0.02f, 1, 2, 0.50f))),
            new SceneWallpaper("dawn", R.string.wallpaper_dawn, new SceneSpec(true, 0xFFFBF3EE, 0xFFF0E1E6,
                    new Blob(0.25f, 0.80f, 0.60f, 0.60f, 0xFFFFC9A8, 0.06f, 0.03f, 1, 1, 0.20f),
                    new Blob(0.80f, 0.30f, 0.50f, 0.80f, 0xFFD9C8FF, 0.05f, 0.04f, 1, 2, 0.55f),
                    new Blob(0.55f, 0.00f, 0.50f, 0.45f, 0xFFFFE3A8, 0.07f, 0.02f, 2, 1, 0.85f))),
            new LandscapeWallpaper("sunset", R.string.wallpaper_sunset, new LandscapeSpec(false,
                    new int[] {0xFF2B1B54, 0xFF6E3A92, 0xFFE0607C, 0xFFFF9A5C, 0xFFFFD08A},
                    new float[] {0f, 0.35f, 0.62f, 0.84f, 1f},
                    0.68f, 0.63f, 0.045f, 0xFFFFF0C8, 0xFFFFB066, 0xFFFFC27A,
                    new Range[] {new Range(0.72f, 0.07f, 3, 0xFF8A4A86), new Range(0.80f, 0.09f, 7, 0xFF4E2A5E),
                            new Range(0.92f, 0.08f, 11, 0xFF221036)},
                    0f, false, new float[][] {{0.25f, 0.30f, 0.35f, 0.06f}, {0.70f, 0.42f, 0.40f, 0.05f}, {0.45f, 0.52f, 0.30f, 0.04f}}, 0xFFFF8FA0)),
            new LandscapeWallpaper("mauve", R.string.wallpaper_mauve, new LandscapeSpec(true,
                    new int[] {0xFFB7A3E8, 0xFFD6B2E6, 0xFFF6C4D2, 0xFFFFE2C2},
                    new float[] {0f, 0.4f, 0.75f, 1f},
                    0.30f, 0.70f, 0.05f, 0xFFFFF6DC, 0xFFFFD2AA, 0xFFFFE2D2,
                    new Range[] {new Range(0.74f, 0.07f, 5, 0xFFCDB8E6), new Range(0.82f, 0.08f, 9, 0xFFB195D2),
                            new Range(0.93f, 0.07f, 2, 0xFF8C71B8)},
                    0f, false, new float[][] {{0.60f, 0.28f, 0.40f, 0.05f}, {0.20f, 0.40f, 0.30f, 0.04f}}, 0xFFFFF0F4)),
            new LandscapeWallpaper("lake", R.string.wallpaper_lake, new LandscapeSpec(false,
                    new int[] {0xFF0E1236, 0xFF3B2A6B, 0xFFB4527E, 0xFFFFA06B},
                    new float[] {0f, 0.42f, 0.7f, 0.86f},
                    0.50f, 0.64f, 0.035f, 0xFFFFE9C0, 0xFFFF9C6A, 0xFFFFA97A,
                    new Range[] {new Range(0.70f, 0.07f, 13, 0xFF4A3470), new Range(0.75f, 0.06f, 4, 0xFF1D1234)},
                    0.74f, true, new float[][] {{0.30f, 0.50f, 0.35f, 0.05f}}, 0xFFFFA0A8)),
            new LandscapeWallpaper("mist", R.string.wallpaper_mist, new LandscapeSpec(false,
                    new int[] {0xFF27406E, 0xFF6C8DB8, 0xFFC9D3E4, 0xFFF2E3D0},
                    new float[] {0f, 0.5f, 0.8f, 1f},
                    0.76f, 0.56f, 0.04f, 0xFFFFF4DC, 0xFFFFDCAC, 0xFFEFE2D6,
                    new Range[] {new Range(0.62f, 0.07f, 21, 0xFFB9C6DA), new Range(0.72f, 0.08f, 6, 0xFF8EA3C2),
                            new Range(0.82f, 0.09f, 15, 0xFF5F7799), new Range(0.94f, 0.08f, 8, 0xFF33496C)},
                    0f, false, new float[][] {{0.30f, 0.34f, 0.40f, 0.04f}}, 0xFFFFFFFF)),
            new SceneWallpaper("nebula", R.string.wallpaper_nebula, new SceneSpec(false, 0xFF07060F, 0xFF0B0A1C,
                    new Blob(0.22f, 0.30f, 0.62f, 0.80f, 0xFF6A3DF0, 0.06f, 0.03f, 1, 1, 0.00f),
                    new Blob(0.82f, 0.18f, 0.46f, 0.75f, 0xFFC13CA8, 0.05f, 0.04f, 1, 2, 0.30f),
                    new Blob(0.62f, 0.95f, 0.70f, 0.55f, 0xFF1F5BFF, 0.07f, 0.02f, 1, 1, 0.60f))),
            new SceneWallpaper("aurora", R.string.wallpaper_aurora, new SceneSpec(false, 0xFF04090C, 0xFF051418,
                    new Blob(0.20f, 0.85f, 0.66f, 0.60f, 0xFF14C8A0, 0.07f, 0.03f, 1, 1, 0.10f),
                    new Blob(0.75f, 0.55f, 0.52f, 0.85f, 0xFF2E7BFF, 0.06f, 0.04f, 1, 2, 0.45f),
                    new Blob(0.50f, 0.10f, 0.50f, 0.50f, 0xFF7B5CFF, 0.08f, 0.02f, 2, 1, 0.80f))),
            new SceneWallpaper("ember", R.string.wallpaper_ember, new SceneSpec(false, 0xFF0B0504, 0xFF150707,
                    new Blob(0.78f, 0.95f, 0.66f, 0.55f, 0xFFFF6A1F, 0.06f, 0.02f, 1, 1, 0.20f),
                    new Blob(0.18f, 0.72f, 0.52f, 0.80f, 0xFFD1213B, 0.05f, 0.04f, 1, 2, 0.55f),
                    new Blob(0.50f, 1.05f, 0.40f, 0.60f, 0xFFFFB347, 0.07f, 0.02f, 2, 1, 0.90f))),
            new SceneWallpaper("ocean", R.string.wallpaper_ocean, new SceneSpec(false, 0xFF030812, 0xFF041A2C,
                    new Blob(0.28f, 0.40f, 0.60f, 0.80f, 0xFF1E8BFF, 0.06f, 0.04f, 1, 1, 0.05f),
                    new Blob(0.82f, 0.85f, 0.55f, 0.60f, 0xFF1CE0E0, 0.07f, 0.02f, 1, 2, 0.40f),
                    new Blob(0.55f, 0.00f, 0.60f, 0.45f, 0xFF0A3FA8, 0.05f, 0.03f, 2, 1, 0.75f))),
            new SceneWallpaper("dusk", R.string.wallpaper_dusk, new SceneSpec(false, 0xFF0A0712, 0xFF170B1D,
                    new Blob(0.25f, 0.40f, 0.60f, 0.85f, 0xFF7B3FD1, 0.06f, 0.03f, 1, 1, 0.15f),
                    new Blob(0.72f, 0.80f, 0.60f, 0.60f, 0xFFFF5E8A, 0.06f, 0.03f, 1, 2, 0.50f),
                    new Blob(0.90f, 0.40f, 0.30f, 0.90f, 0xFFFFA24A, 0.04f, 0.04f, 2, 1, 0.85f))),
            new SceneWallpaper("graphite", R.string.wallpaper_graphite, new SceneSpec(false, 0xFF0A0B0D, 0xFF13151A,
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
