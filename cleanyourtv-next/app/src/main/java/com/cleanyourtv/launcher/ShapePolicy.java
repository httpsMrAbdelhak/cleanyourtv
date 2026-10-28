package com.cleanyourtv.launcher;

/**
 * Which picture a card shows. Collapsed, a card is a tall poster: the film's or series' poster, never an
 * episode still. Selected, it widens and shows landscape art: a landscape poster, or the thumbnail when it
 * is not an episode's (an episode's thumbnail is usually a still of that episode), else the poster cropped.
 * Pure Java.
 */
final class ShapePolicy {
    private ShapePolicy() { }

    static String poster(String poster, String thumbnail) {
        return !poster.isEmpty() ? poster : thumbnail;
    }

    static String wide(String poster, boolean posterLandscape, String thumbnail, boolean thumbnailLandscape, boolean episode) {
        if (!poster.isEmpty() && posterLandscape) return poster;
        if (!thumbnail.isEmpty() && thumbnailLandscape && !episode) return thumbnail;
        return !poster.isEmpty() ? poster : thumbnail;
    }
}
