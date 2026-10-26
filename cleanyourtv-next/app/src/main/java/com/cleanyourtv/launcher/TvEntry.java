package com.cleanyourtv.launcher;

/**
 * One title another app published for the Android TV home screen: a film, an episode, a channel entry.
 * Everything comes from the app itself, through the TV's content provider; nothing is added or guessed here.
 */
final class TvEntry {
    enum Kind { MOVIE, SERIES, OTHER }

    enum Shape { POSTER, LANDSCAPE }

    /** The row it was published in: "Continue watching", or the channel name the app gave it. */
    final String section;
    final String title;
    final String subtitle;
    /** An https:// or content:// address of the picture, or empty. */
    final String imageUri;
    /** The intent the app wants started to open or resume the title, as an intent URI string. */
    final String intentUri;
    /** 0..1 for a started title, -1 when unknown or not started. */
    final float progress;
    final Kind kind;
    final Shape shape;
    final boolean resume;

    TvEntry(String section, String title, String subtitle, String imageUri, String intentUri,
            float progress, Kind kind, Shape shape, boolean resume) {
        this.section = section;
        this.title = title;
        this.subtitle = subtitle;
        this.imageUri = imageUri;
        this.intentUri = intentUri;
        this.progress = progress;
        this.kind = kind;
        this.shape = shape;
        this.resume = resume;
    }
}
