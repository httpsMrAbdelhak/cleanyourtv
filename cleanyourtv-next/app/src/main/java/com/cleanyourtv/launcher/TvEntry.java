package com.cleanyourtv.launcher;

/**
 * One title another app published for the Android TV home screen: a film, a series, an episode. Everything
 * comes from the app itself, through the TV's content provider; nothing is added or guessed here.
 */
final class TvEntry {
    enum Kind { MOVIE, SERIES, OTHER }

    /** The row it was published in: "Continue watching", "My list", or the channel name the app gave it. */
    final String section;
    final String title;
    final String subtitle;
    /** The tall picture (the film's or series' poster when the app gives one), https:// or content://, or empty. */
    final String posterUri;
    /** The wide picture shown when the card is selected: landscape key art, never an episode still if avoidable. */
    final String wideUri;
    /** The intent the app wants started to open or resume the title, as an intent URI string. */
    final String intentUri;
    /** 0..1 for a started title, -1 when unknown or not started. */
    final float progress;
    /** Milliseconds left to watch, -1 when unknown. */
    final long remainingMs;
    /** "S38E01", "E04" or empty. */
    final String badge;
    /** The release year, or empty. */
    final String year;
    final Kind kind;
    final boolean resume;

    TvEntry(String section, String title, String subtitle, String posterUri, String wideUri, String intentUri,
            float progress, long remainingMs, String badge, String year, Kind kind, boolean resume) {
        this.section = section;
        this.title = title;
        this.subtitle = subtitle;
        this.posterUri = posterUri;
        this.wideUri = wideUri;
        this.intentUri = intentUri;
        this.progress = progress;
        this.remainingMs = remainingMs;
        this.badge = badge;
        this.year = year;
        this.kind = kind;
        this.resume = resume;
    }
}
