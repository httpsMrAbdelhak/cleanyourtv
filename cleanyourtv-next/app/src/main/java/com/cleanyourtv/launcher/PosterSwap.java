package com.cleanyourtv.launcher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Which series or film a published card stands for (one lookup key for all its episodes), and the cards again
 * with the TMDB poster and backdrop in place of the app's pictures. Pure Java: the network is in Tmdb.
 */
final class PosterSwap {
    private PosterSwap() { }

    static List<TvEntry> all(TvChannels.Result result) {
        List<TvEntry> out = new ArrayList<>(result.home);
        out.addAll(result.watchlist);
        for (List<TvEntry> row : result.sections.values()) out.addAll(row);
        return out;
    }

    static boolean isSeries(TvEntry entry, TmdbIds.Ref ref) {
        if (ref.tv != null) return ref.tv;
        return entry.kind == TvEntry.Kind.SERIES || !entry.badge.isEmpty();
    }

    static boolean kindKnown(TvEntry entry, TmdbIds.Ref ref) {
        return ref.tv != null || entry.kind != TvEntry.Kind.OTHER || !entry.badge.isEmpty();
    }

    /** One key per series or film, so that ten episodes of a series cost one lookup. */
    static String lookupKey(TvEntry entry) {
        TmdbIds.Ref ref = TmdbIds.from(entry.intentUri);
        boolean tv = isSeries(entry, ref);
        if (ref.tmdbId != null) return "id_" + (tv ? "tv" : "movie") + ref.tmdbId;
        if (ref.imdbId != null) return "imdb_" + ref.imdbId;
        String kind = kindKnown(entry, ref) ? (tv ? "tv" : "movie") : "any";
        return "q_" + kind + "_" + TitleKey.key(TitleKey.query(entry.title)) + (tv ? "" : "_" + entry.year);
    }

    static TvChannels.Result apply(TvChannels.Result result, Map<String, TmdbParser.Art> found) {
        if (found.isEmpty()) return result;
        Map<String, List<TvEntry>> sections = new LinkedHashMap<>();
        for (Map.Entry<String, List<TvEntry>> row : result.sections.entrySet()) sections.put(row.getKey(), replace(row.getValue(), found));
        return new TvChannels.Result(result.status, replace(result.home, found), replace(result.watchlist, found), sections);
    }

    static List<TvEntry> replace(List<TvEntry> entries, Map<String, TmdbParser.Art> found) {
        List<TvEntry> out = new ArrayList<>(entries.size());
        for (TvEntry entry : entries) {
            TmdbParser.Art art = found.get(lookupKey(entry));
            out.add(art == null || art.isEmpty() ? entry : withArt(entry, art));
        }
        return out;
    }

    static TvEntry withArt(TvEntry entry, TmdbParser.Art art) {
        String poster = art.poster.isEmpty() ? entry.posterUri : art.poster;
        String wide = art.backdrop.isEmpty() ? entry.wideUri : art.backdrop;
        return new TvEntry(entry.section, entry.title, entry.subtitle, poster, wide, entry.intentUri, entry.progress,
                entry.remainingMs, entry.badge, entry.year, entry.kind, entry.resume);
    }
}
