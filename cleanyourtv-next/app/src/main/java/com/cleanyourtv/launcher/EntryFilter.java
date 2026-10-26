package com.cleanyourtv.launcher;

import java.util.ArrayList;
import java.util.List;

/** The three tabs over what the app published: Explore shows everything, Movies and Series filter by kind. */
final class EntryFilter {
    private EntryFilter() { }

    static final int EXPLORE = 0;
    static final int MOVIES = 1;
    static final int SERIES = 2;

    /**
     * Explore shows the films and series (not clips, channels or music), one card per title: several
     * episodes of the same series would otherwise repeat one poster. If the app published only other kinds,
     * those are shown rather than an empty shelf. Movies and Series filter by kind.
     */
    static List<TvEntry> apply(List<TvEntry> entries, int tab) {
        List<TvEntry> out = new ArrayList<>();
        if (tab == EXPLORE) {
            for (TvEntry entry : entries) {
                if (entry.kind != TvEntry.Kind.OTHER) out.add(entry);
            }
            if (out.isEmpty()) out.addAll(entries);
            return oneCardPerTitle(out);
        }
        TvEntry.Kind wanted = tab == MOVIES ? TvEntry.Kind.MOVIE : TvEntry.Kind.SERIES;
        for (TvEntry entry : entries) {
            if (entry.kind == wanted) out.add(entry);
        }
        return oneCardPerTitle(out);
    }

    /** Keeps the first card of each title (case ignored), so "Continue watching" wins over the same title in a row. */
    static List<TvEntry> oneCardPerTitle(List<TvEntry> entries) {
        List<TvEntry> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (TvEntry entry : entries) {
            String key = entry.title.trim().toLowerCase(java.util.Locale.ROOT);
            if (key.isEmpty() || seen.add(key)) out.add(entry);
        }
        return out;
    }
}
