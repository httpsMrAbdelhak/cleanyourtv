package com.cleanyourtv.launcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads a YouTube Data API v3 "videos" list: items[].id (a string for this endpoint), items[].snippet.title,
 * channelTitle and thumbnails.{maxres,standard,high,medium,default}.url. Videos without an id, a title or a
 * thumbnail are dropped. Written from the documented shape, not from a recorded answer.
 */
final class YouTubeParser {
    private YouTubeParser() { }

    private static final String[] SIZES = {"maxres", "standard", "high", "medium", "default"};

    static List<YouTubeVideo> parse(String json) throws JSONException {
        List<YouTubeVideo> videos = new ArrayList<>();
        JSONArray items = new JSONObject(json).optJSONArray("items");
        if (items == null) return videos;
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            String id = item.optString("id", "");
            JSONObject snippet = item.optJSONObject("snippet");
            if (id.isEmpty() || snippet == null) continue;
            String title = snippet.optString("title", "");
            JSONObject thumbs = snippet.optJSONObject("thumbnails");
            String thumb = "";
            if (thumbs != null) {
                for (String size : SIZES) {
                    JSONObject entry = thumbs.optJSONObject(size);
                    if (entry != null && !entry.optString("url", "").isEmpty()) {
                        thumb = entry.optString("url");
                        break;
                    }
                }
            }
            if (title.isEmpty() || !thumb.startsWith("https://")) continue;
            videos.add(new YouTubeVideo(id, title, snippet.optString("channelTitle", ""), thumb));
        }
        return videos;
    }
}
