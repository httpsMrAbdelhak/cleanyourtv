package com.cleanyourtv.launcher;

import java.util.Locale;

/**
 * Which streaming service an installed app belongs to, so that the panel above the dock can show that
 * service's films and series. No numeric id is written here: TMDB's own list of providers is fetched and
 * matched by name (see Tmdb), because an id that is wrong would silently show another service's catalogue.
 */
final class ProviderMap {
    private ProviderMap() { }

    private static final String[][] SERVICES = {
            // package prefix, then the names TMDB may use for the service
            {"com.netflix.", "netflix"},
            {"com.amazon.amazonvideo.livingroom", "amazonprimevideo", "primevideo"},
            {"com.amazon.avod.thirdpartyclient", "amazonprimevideo", "primevideo"},
            {"com.disney.disneyplus", "disneyplus", "disney"},
            {"com.hbo.hbonow", "max", "hbomax"},
            {"com.wbd.stream", "max", "hbomax"},
            {"com.apple.atve.androidtv.appletv", "appletvplus", "appletv"},
            {"com.cbs.app", "paramountplus"},
            {"com.cbs.ca", "paramountplus"},
            {"com.canal.android.canal", "canal"},
            {"com.crunchyroll.crunchyroid", "crunchyroll"},
    };

    private static final String[] YOUTUBE = {
            "com.google.android.youtube.tv",
            "com.liskovsoft.smarttubetv",
            "com.liskovsoft.smarttubetv.beta",
    };

    /** The provider names to look for in TMDB's list, or null when the app is not a known streaming service. */
    static String[] namesFor(String packageName) {
        if (packageName == null) return null;
        for (String[] service : SERVICES) {
            if (packageName.equals(service[0]) || (service[0].endsWith(".") && packageName.startsWith(service[0]))) {
                String[] names = new String[service.length - 1];
                System.arraycopy(service, 1, names, 0, names.length);
                return names;
            }
        }
        return null;
    }

    static boolean isYouTube(String packageName) {
        if (packageName == null) return false;
        for (String known : YOUTUBE) {
            if (known.equals(packageName)) return true;
        }
        return false;
    }

    /** TMDB provider names compared without case, spaces or punctuation: "Disney Plus" == "disneyplus". */
    static String normalize(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** True when a TMDB provider name is one of the acceptable names. "Canal+" must not match "Canal+ Séries". */
    static boolean matches(String tmdbName, String[] wanted) {
        String normalized = normalize(tmdbName.replace("+", "plus"));
        for (String want : wanted) {
            if (normalized.equals(want) || (want.equals("canal") && normalized.equals("canalplus"))) return true;
        }
        return false;
    }
}
