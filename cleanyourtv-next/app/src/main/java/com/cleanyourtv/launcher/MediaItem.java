package com.cleanyourtv.launcher;

import java.io.Serializable;

/** A film or a series as TMDB describes it. Only what the list answers carry: no playback link exists. */
final class MediaItem implements Serializable {
    private static final long serialVersionUID = 1L;

    final long id;
    final String title;
    final String overview;
    final String posterPath;
    final String backdropPath;
    /** 0 when TMDB has no rating yet. */
    final double rating;
    /** The year, or an empty string. */
    final String year;
    final boolean series;

    MediaItem(long id, String title, String overview, String posterPath, String backdropPath,
              double rating, String year, boolean series) {
        this.id = id;
        this.title = title;
        this.overview = overview;
        this.posterPath = posterPath;
        this.backdropPath = backdropPath;
        this.rating = rating;
        this.year = year;
        this.series = series;
    }
}
