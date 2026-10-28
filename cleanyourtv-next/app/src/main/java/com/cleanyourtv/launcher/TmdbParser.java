package com.cleanyourtv.launcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reads the few fields CleanYourTV uses from TMDB answers: posters, backdrops, titles, years, ratings. */
final class TmdbParser {
    private TmdbParser() { }

    static final String POSTER = "https://image.tmdb.org/t/p/w342";
    static final String BACKDROP = "https://image.tmdb.org/t/p/w780";

    /** A poster and a backdrop, each a full https:// address or empty. */
    static final class Art {
        final String poster;
        final String backdrop;

        Art(String poster, String backdrop) {
            this.poster = poster;
            this.backdrop = backdrop;
        }

        boolean isEmpty() {
            return poster.isEmpty() && backdrop.isEmpty();
        }
    }

    /** One trending film or series. */
    static final class Title {
        final String title;
        final String year;
        final double vote;
        final boolean tv;
        final Art art;

        Title(String title, String year, double vote, boolean tv, Art art) {
            this.title = title;
            this.year = year;
            this.vote = vote;
            this.tv = tv;
            this.art = art;
        }
    }

    static Art art(JSONObject item) {
        return new Art(path(POSTER, item.optString("poster_path", "")), path(BACKDROP, item.optString("backdrop_path", "")));
    }

    private static String path(String base, String value) {
        if (value == null || value.isEmpty() || value.equals("null") || !value.startsWith("/")) return "";
        return base + value;
    }

    /** /movie/{id} or /tv/{id}. */
    static Art details(String json) throws JSONException {
        return art(new JSONObject(json));
    }

    /** /find/{imdb id}: the series or the film it names, the wanted kind first. */
    static Art find(String json, boolean tv) throws JSONException {
        JSONObject root = new JSONObject(json);
        String[] order = tv ? new String[] {"tv_results", "movie_results"} : new String[] {"movie_results", "tv_results"};
        for (String field : order) {
            JSONArray list = root.optJSONArray(field);
            if (list == null) continue;
            for (int i = 0; i < list.length(); i++) {
                Art art = art(list.getJSONObject(i));
                if (!art.poster.isEmpty()) return art;
            }
        }
        return null;
    }

    /**
     * /search/movie, /search/tv or /search/multi: the result whose title matches, the year breaking ties.
     * A result whose title has nothing in common with the wanted one is not taken: no poster beats a wrong one.
     */
    static Art bestMatch(String json, String wanted, String year) throws JSONException {
        JSONArray results = new JSONObject(json).optJSONArray("results");
        if (results == null) return null;
        String key = TitleKey.key(wanted);
        if (key.isEmpty()) return null;
        JSONObject best = null;
        int bestScore = 0;
        for (int i = 0; i < results.length() && i < 10; i++) {
            JSONObject item = results.getJSONObject(i);
            String type = item.optString("media_type", "");
            if (type.equals("person")) continue;
            if (path(POSTER, item.optString("poster_path", "")).isEmpty()) continue;
            String name = TitleKey.key(item.optString("title", item.optString("name", "")));
            String original = TitleKey.key(item.optString("original_title", item.optString("original_name", "")));
            int score;
            if (key.equals(name) || key.equals(original)) score = 4;
            else if (!name.isEmpty() && (name.contains(key) || key.contains(name))) score = 2;
            else if (!original.isEmpty() && (original.contains(key) || key.contains(original))) score = 2;
            else continue;
            if (year != null && !year.isEmpty() && year.equals(yearOf(item))) score += 1;
            if (score > bestScore) {
                best = item;
                bestScore = score;
            }
        }
        return best == null ? null : art(best);
    }

    /** /trending/movie/week or /trending/tv/week. */
    static List<Title> trending(String json, boolean tv) throws JSONException {
        JSONArray results = new JSONObject(json).optJSONArray("results");
        List<Title> out = new ArrayList<>();
        if (results == null) return out;
        for (int i = 0; i < results.length(); i++) {
            JSONObject item = results.getJSONObject(i);
            Art art = art(item);
            String title = item.optString(tv ? "name" : "title", "").trim();
            if (title.isEmpty() || art.poster.isEmpty()) continue;
            out.add(new Title(title, yearOf(item), item.optDouble("vote_average", 0), tv, art));
        }
        return out;
    }

    static String yearOf(JSONObject item) {
        String date = item.optString("release_date", item.optString("first_air_date", ""));
        return date.length() >= 4 && date.substring(0, 4).matches("\\d{4}") ? date.substring(0, 4) : "";
    }

    /** "★ 7.8", or empty for a title nobody rated yet. */
    static String rating(double vote, Locale locale) {
        if (vote <= 0) return "";
        return String.format(locale, "★ %.1f", vote);
    }
}
