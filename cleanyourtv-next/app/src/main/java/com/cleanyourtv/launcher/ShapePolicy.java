package com.cleanyourtv.launcher;

/**
 * Which picture an entry shows and how the card is shaped. The poster of the film or series comes first: apps
 * often publish an episode's still as the thumbnail, which is not what is wanted on the shelf. Pure Java.
 */
final class ShapePolicy {
    private ShapePolicy() { }

    static boolean usePoster(boolean hasPoster) {
        return hasPoster;
    }

    /**
     * A poster shown as such is a tall card. If the app's "poster" is itself landscape (some apps publish a
     * 16:9 key art there), the card follows it; with no poster at all, the thumbnail is landscape.
     */
    static TvEntry.Shape shape(boolean usingPoster, boolean posterIsLandscape) {
        if (!usingPoster) return TvEntry.Shape.LANDSCAPE;
        return posterIsLandscape ? TvEntry.Shape.LANDSCAPE : TvEntry.Shape.POSTER;
    }
}
