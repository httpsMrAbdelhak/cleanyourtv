package com.cleanyourtv.launcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Reads Open-Meteo geocoding answers: "results" [{name, latitude, longitude, admin1, country}]. */
final class GeoParser {
    private GeoParser() { }

    static final class Place {
        final String name;
        final String detail;
        final double latitude;
        final double longitude;

        Place(String name, String detail, double latitude, double longitude) {
            this.name = name;
            this.detail = detail;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        /** "Lyon, Auvergne-Rhône-Alpes, France" */
        String label() {
            return detail.isEmpty() ? name : name + ", " + detail;
        }
    }

    /** The service omits "results" altogether when nothing matches: that is an empty list, not an error. */
    static List<Place> parse(String json) throws JSONException {
        List<Place> places = new ArrayList<>();
        JSONArray results = new JSONObject(json).optJSONArray("results");
        if (results == null) return places;
        for (int i = 0; i < results.length(); i++) {
            JSONObject entry = results.optJSONObject(i);
            if (entry == null || !entry.has("latitude") || !entry.has("longitude")) continue;
            String name = entry.optString("name", "");
            if (name.isEmpty()) continue;
            StringBuilder detail = new StringBuilder();
            for (String key : new String[] {"admin1", "country"}) {
                String part = entry.optString(key, "");
                if (part.isEmpty() || part.equals(name)) continue;
                if (detail.length() > 0) detail.append(", ");
                detail.append(part);
            }
            places.add(new Place(name, detail.toString(), entry.getDouble("latitude"), entry.getDouble("longitude")));
        }
        return places;
    }
}
