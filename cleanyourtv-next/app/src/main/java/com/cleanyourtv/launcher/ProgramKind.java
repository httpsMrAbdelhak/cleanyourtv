package com.cleanyourtv.launcher;

/** Maps the TV provider's program type numbers (TvContract.PreviewPrograms.TYPE_*) to what the tabs filter on. */
final class ProgramKind {
    private ProgramKind() { }

    static final int TYPE_MOVIE = 0;
    static final int TYPE_TV_SERIES = 1;
    static final int TYPE_TV_SEASON = 2;
    static final int TYPE_TV_EPISODE = 3;

    static TvEntry.Kind of(int type) {
        if (type == TYPE_MOVIE) return TvEntry.Kind.MOVIE;
        if (type == TYPE_TV_SERIES || type == TYPE_TV_SEASON || type == TYPE_TV_EPISODE) return TvEntry.Kind.SERIES;
        return TvEntry.Kind.OTHER;
    }

    /** Progress of a started title: position over duration, clamped; -1 when either is missing. */
    static float progress(long positionMs, long durationMs) {
        if (durationMs <= 0 || positionMs <= 0) return -1f;
        return Math.min(1f, (float) positionMs / durationMs);
    }
}
