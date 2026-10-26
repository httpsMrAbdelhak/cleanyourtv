package com.cleanyourtv.launcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** The dock of a first run: well-known TV apps that happen to be installed, nothing otherwise. */
final class DockDefaults {
    private DockDefaults() { }

    private static final String[] PREFERRED = {
            "com.google.android.youtube.tv",
            "com.netflix.ninja",
            "com.amazon.amazonvideo.livingroom",
            "com.disney.disneyplus",
            "com.spotify.tv.android",
            "com.google.android.youtube.tvmusic",
            "org.videolan.vlc",
            "com.plexapp.android",
            "org.xbmc.kodi",
            "tv.twitch.android.app",
            "com.android.vending",
    };

    /** Up to {@link DockModel#MAX} minus one apps, so there is always a free slot to add one. */
    static List<String> pick(Set<String> installed) {
        List<String> out = new ArrayList<>();
        // The streaming app whose content the panel shows comes first, whatever its exact package name.
        for (String packageName : installed) {
            if (packageName.toLowerCase(java.util.Locale.ROOT).contains("nuvio") && out.size() < DockModel.MAX - 1) {
                out.add(packageName);
                break;
            }
        }
        for (String packageName : PREFERRED) {
            if (out.size() >= DockModel.MAX - 1) break;
            if (installed.contains(packageName) && !out.contains(packageName)) out.add(packageName);
        }
        return out;
    }
}
