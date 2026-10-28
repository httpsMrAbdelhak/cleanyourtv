package com.cleanyourtv.launcher;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The TMDB or IMDb id an app may have put in the intent it published for a title (for instance
 * "tmdb:1668" or "tt0108778"). With one, the poster is looked up by id rather than by title. Pure Java.
 */
final class TmdbIds {
    private TmdbIds() { }

    private static final String SEP = "(?:[_:/=.-]|%3a|%2f|%3d)*";
    private static final Pattern TMDB = Pattern.compile("(?i)tmdb(?:_?id)?" + SEP + "(?:(movie|tv|show|series)" + SEP + ")?(\\d{1,9})(?!\\d)");
    private static final Pattern IMDB = Pattern.compile("(?<![a-z0-9])(tt\\d{7,10})(?!\\d)");

    static final class Ref {
        final String tmdbId;
        final String imdbId;
        /** True for a series, false for a film, null when the intent does not say. */
        final Boolean tv;

        Ref(String tmdbId, String imdbId, Boolean tv) {
            this.tmdbId = tmdbId;
            this.imdbId = imdbId;
            this.tv = tv;
        }
    }

    static Ref from(String intentUri) {
        if (intentUri == null || intentUri.isEmpty()) return new Ref(null, null, null);
        String lower = intentUri.toLowerCase(Locale.ROOT);
        Boolean tv = null;
        String tmdb = null;
        Matcher m = TMDB.matcher(intentUri);
        if (m.find()) {
            tmdb = m.group(2);
            if (m.group(1) != null) tv = !m.group(1).equalsIgnoreCase("movie");
        }
        if (tv == null) {
            if (lower.matches(".*(?:[/=:]|%2f|%3a)movies?(?:[/?&#:]|%2f|%3a|$).*")) tv = false;
            else if (lower.matches(".*(?:[/=:]|%2f|%3a)(?:tv|series|shows?)(?:[/?&#:]|%2f|%3a|$).*")) tv = true;
        }
        Matcher i = IMDB.matcher(lower);
        String imdb = i.find() ? i.group(1) : null;
        return new Ref(tmdb, imdb, tv);
    }
}
