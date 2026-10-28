package com.cleanyourtv.launcher;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** One installed app as the launcher lists it. */
final class AppEntry {
    final String label;
    final String packageName;
    /** The app declares itself a game (ApplicationInfo.CATEGORY_GAME): listed under "Games". */
    final boolean game;

    AppEntry(String label, String packageName) {
        this(label, packageName, false);
    }

    AppEntry(String label, String packageName, boolean game) {
        this.label = label;
        this.packageName = packageName;
        this.game = game;
    }

    private static final Comparator<AppEntry> BY_LABEL = (a, b) -> {
        int byLabel = String.CASE_INSENSITIVE_ORDER.compare(a.label, b.label);
        return byLabel != 0 ? byLabel : a.packageName.compareTo(b.packageName);
    };

    /** Alphabetical, ignoring case; the package name settles ties so the order never jumps between runs. */
    static void sort(List<AppEntry> apps) {
        Collections.sort(apps, BY_LABEL);
    }
}
