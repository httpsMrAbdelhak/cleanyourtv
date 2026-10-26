package com.cleanyourtv.launcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Reads TMDB's list of watch providers ("results": [{provider_id, provider_name}]) and finds a service's id. */
final class ProviderParser {
    private ProviderParser() { }

    /** The id of the first provider whose name matches, or 0 when none does. */
    static int find(String json, String[] wantedNames) throws JSONException {
        JSONArray results = new JSONObject(json).optJSONArray("results");
        if (results == null) return 0;
        for (int i = 0; i < results.length(); i++) {
            JSONObject entry = results.optJSONObject(i);
            if (entry == null) continue;
            if (ProviderMap.matches(entry.optString("provider_name", ""), wantedNames)) {
                return entry.optInt("provider_id", 0);
            }
        }
        return 0;
    }
}
