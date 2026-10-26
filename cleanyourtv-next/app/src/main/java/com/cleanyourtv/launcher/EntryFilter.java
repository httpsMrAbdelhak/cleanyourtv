package com.cleanyourtv.launcher;

import java.util.ArrayList;
import java.util.List;

/** The three tabs over what the app published: Explore shows everything, Movies and Series filter by kind. */
final class EntryFilter {
    private EntryFilter() { }

    static final int EXPLORE = 0;
    static final int MOVIES = 1;
    static final int SERIES = 2;

    static List<TvEntry> apply(List<TvEntry> entries, int tab) {
        if (tab == EXPLORE) return new ArrayList<>(entries);
        TvEntry.Kind wanted = tab == MOVIES ? TvEntry.Kind.MOVIE : TvEntry.Kind.SERIES;
        List<TvEntry> out = new ArrayList<>();
        for (TvEntry entry : entries) {
            if (entry.kind == wanted) out.add(entry);
        }
        return out;
    }
}
