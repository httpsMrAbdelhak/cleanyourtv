package com.cleanyourtv.launcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Reads an Open-Meteo forecast: "current" {temperature_2m, weather_code, is_day} and "daily" arrays
 * {time, weather_code, temperature_2m_max, temperature_2m_min}. Throws when the answer has no usable
 * current block or no day, so the caller shows an error instead of a made-up forecast.
 */
final class ForecastParser {
    private ForecastParser() { }

    static Forecast parse(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONObject current = root.getJSONObject("current");
        Forecast forecast = new Forecast(current.getDouble("temperature_2m"),
                current.getInt("weather_code"), current.optInt("is_day", 1) == 1);
        JSONObject daily = root.getJSONObject("daily");
        JSONArray time = daily.getJSONArray("time");
        JSONArray code = daily.getJSONArray("weather_code");
        JSONArray max = daily.getJSONArray("temperature_2m_max");
        JSONArray min = daily.getJSONArray("temperature_2m_min");
        int count = Math.min(Math.min(time.length(), code.length()), Math.min(max.length(), min.length()));
        for (int i = 0; i < count; i++) {
            if (code.isNull(i) || max.isNull(i) || min.isNull(i)) continue;
            forecast.days.add(new Forecast.Day(time.getString(i), code.getInt(i), max.getDouble(i), min.getDouble(i)));
        }
        if (forecast.days.isEmpty()) throw new JSONException("no daily data");
        return forecast;
    }
}
