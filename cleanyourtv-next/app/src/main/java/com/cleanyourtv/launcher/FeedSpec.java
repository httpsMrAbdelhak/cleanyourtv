package com.cleanyourtv.launcher;

/**
 * One TMDB request: which list, and whether it holds films or series. Built by the static factories so that
 * the cache key and the URL stay in step. The three tabs of the home screen map to Explorer, Films, Series.
 */
final class FeedSpec {
    final String path;
    final String query;
    final boolean series;
    /** The installed app whose service the list is filtered to, or null for the general lists. */
    final String providerPackage;

    private FeedSpec(String path, String query, boolean series, String providerPackage) {
        this.path = path;
        this.query = query;
        this.series = series;
        this.providerPackage = providerPackage;
    }

    static FeedSpec trending() {
        return new FeedSpec("trending/all/week", "", false, null);
    }

    static FeedSpec popular(boolean series) {
        return new FeedSpec(series ? "tv/popular" : "movie/popular", "", series, null);
    }

    static FeedSpec nowPlaying() {
        return new FeedSpec("movie/now_playing", "", false, null);
    }

    /** The most popular titles of one streaming service in the TV's country. The provider id is added later. */
    static FeedSpec onService(String packageName, boolean series) {
        return new FeedSpec(series ? "discover/tv" : "discover/movie", "&sort_by=popularity.desc", series, packageName);
    }

    /** A file-safe key; the language and the country change the answer, so they are part of it. */
    String cacheKey(String language, String region) {
        return ("tmdb_" + path.replace('/', '_') + "_" + (providerPackage == null ? "" : providerPackage + "_")
                + language + "_" + region);
    }
}
