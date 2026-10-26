package com.cleanyourtv.launcher;

/** One card in the panel above the dock: a poster, a wide picture, or a video thumbnail. */
final class ContentCard {
    enum Shape { WIDE, POSTER, VIDEO }

    final Shape shape;
    final String title;
    final String subtitle;
    final String imageUrl;
    /** Set for a film or series (opens the detail screen). */
    final MediaItem media;
    /** Set for a YouTube video (opens it in the YouTube app). */
    final String videoId;

    ContentCard(Shape shape, String title, String subtitle, String imageUrl, MediaItem media, String videoId) {
        this.shape = shape;
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.media = media;
        this.videoId = videoId;
    }

    /** "Film  •  2021  •  ★ 8.1" built from what TMDB gave; kind is the already translated word. */
    static String meta(String kind, MediaItem item) {
        StringBuilder out = new StringBuilder(kind);
        if (!item.year.isEmpty()) out.append("  •  ").append(item.year);
        if (item.rating > 0) out.append("  •  ★ ").append(String.format(java.util.Locale.ROOT, "%.1f", item.rating));
        return out.toString();
    }
}
