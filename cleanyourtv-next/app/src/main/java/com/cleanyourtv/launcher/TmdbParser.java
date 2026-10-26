package com.cleanyourtv.launcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads TMDB list answers ("results": [...]). Entries without a title or a poster are dropped, and so are
 * people (trending/all mixes them in). Written from the documented fields: title or name, overview,
 * poster_path, backdrop_path, vote_average, release_date or first_air_date, media_type.
 */
final class TmdbParser {
    private TmdbParser() { }

    /** defaultSeries: the type of entries that carry no media_type (movie/popular has none, tv/popular has none). */
    static List<MediaItem> parse(String json, boolean defaultSeries) throws JSONException {
        List<MediaItem> items = new ArrayList<>();
        JSONArray results = new JSONObject(json).optJSONArray("results");
        if (results == null) return items;
        for (int i = 0; i < results.length(); i++) {
            JSONObject entry = results.optJSONObject(i);
            if (entry == null) continue;
            String type = entry.optString("media_type", "");
            if (type.equals("person")) continue;
            boolean series = type.isEmpty() ? defaultSeries : type.equals("tv");
            String title = entry.optString(series ? "name" : "title", "");
            if (title.isEmpty()) title = entry.optString(series ? "title" : "name", "");
            String poster = entry.isNull("poster_path") ? "" : entry.optString("poster_path", "");
            if (title.isEmpty() || poster.isEmpty()) continue;
            String backdrop = entry.isNull("backdrop_path") ? "" : entry.optString("backdrop_path", "");
            String date = entry.optString(series ? "first_air_date" : "release_date", "");
            String year = date.length() >= 4 ? date.substring(0, 4) : "";
            items.add(new MediaItem(entry.optLong("id"), title, entry.optString("overview", ""),
                    poster, backdrop, entry.optDouble("vote_average", 0), year, series));
        }
        return items;
    }
}
