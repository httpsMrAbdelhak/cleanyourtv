package com.cleanyourtv.launcher;

import java.util.List;
import java.util.Locale;

/** Finds the app whose home-screen content the panel shows: the saved choice, else an installed Nuvio. */
final class SourceApp {
    private SourceApp() { }

    /** The package to use, or null when none is chosen and no app looks like Nuvio. */
    static String resolve(String saved, List<AppEntry> installed) {
        if (saved != null && !saved.isEmpty()) {
            for (AppEntry app : installed) {
                if (app.packageName.equals(saved)) return saved;
            }
        }
        for (AppEntry app : installed) {
            if (app.packageName.toLowerCase(Locale.ROOT).contains("nuvio") || app.label.toLowerCase(Locale.ROOT).contains("nuvio")) {
                return app.packageName;
            }
        }
        return null;
    }
}
