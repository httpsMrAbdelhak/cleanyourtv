package com.cleanyourtv.launcher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Seven-day weather from Open-Meteo, which needs no key. The city is searched by name (Open-Meteo
 * geocoding) and chosen in the settings: the device location is never read. Answers are cached for 30
 * minutes and never fetched more than once a minute. Weather data by Open-Meteo.com (CC BY 4.0).
 */
final class Weather {
    private Weather() { }

    enum Error { NONE, NO_CITY, OFFLINE, FAILED }

    static final class Result {
        final Forecast forecast;
        final boolean stale;
        final Error error;

        Result(Forecast forecast, boolean stale, Error error) {
            this.forecast = forecast;
            this.stale = stale;
            this.error = error;
        }
    }

    interface Callback {
        void onResult(Result result);
    }

    interface SearchCallback {
        void onPlaces(List<GeoParser.Place> places, boolean failed);
    }

    private static final String FORECAST = "https://api.open-meteo.com/v1/forecast";
    private static final String GEOCODING = "https://geocoding-api.open-meteo.com/v1/search";
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "weather");
        thread.setDaemon(true);
        return thread;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    static void load(final Context context, final boolean forceNetwork, final Callback callback) {
        final Context app = context.getApplicationContext();
        final Prefs prefs = new Prefs(app);
        POOL.execute(() -> {
            final Result result = fetch(app, prefs, forceNetwork);
            prefs.setStatus("weather", result.error == Error.NONE
                    ? (result.stale ? "stale:" : "ok:") + result.forecast.fetchedAt
                    : result.error.name().toLowerCase(Locale.ROOT));
            MAIN.post(() -> callback.onResult(result));
        });
    }

    static void search(final String name, final SearchCallback callback) {
        POOL.execute(() -> {
            List<GeoParser.Place> places = new ArrayList<>();
            boolean failed = false;
            try {
                String language = Locale.getDefault().getLanguage().equals("fr") ? "fr" : "en";
                String url = GEOCODING + "?name=" + URLEncoder.encode(name, "UTF-8")
                        + "&count=5&format=json&language=" + language;
                places = GeoParser.parse(Http.text(url, null));
            } catch (IOException | JSONException | RuntimeException e) {
                failed = true;
            }
            final List<GeoParser.Place> found = places;
            final boolean error = failed;
            MAIN.post(() -> callback.onPlaces(found, error));
        });
    }

    private static Result fetch(Context context, Prefs prefs, boolean forceNetwork) {
        if (!prefs.hasCity()) return new Result(null, false, Error.NO_CITY);
        String cacheKey = String.format(Locale.ROOT, "weather_%.2f_%.2f", prefs.cityLatitude(), prefs.cityLongitude());
        JsonCache.Entry cached = JsonCache.read(context, cacheKey);
        long now = System.currentTimeMillis();
        if (cached != null) {
            long age = now - cached.fetchedAt;
            boolean fresh = CachePolicy.isFresh(now, cached.fetchedAt, CachePolicy.WEATHER_TTL_MS);
            boolean tooSoon = age >= 0 && age < CachePolicy.WEATHER_MIN_GAP_MS;
            if (fresh && !forceNetwork || tooSoon) {
                Forecast forecast = parseOrNull(cached);
                if (forecast != null) return new Result(forecast, false, Error.NONE);
            }
        }
        try {
            String url = FORECAST + String.format(Locale.ROOT,
                    "?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code,is_day"
                            + "&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=7",
                    prefs.cityLatitude(), prefs.cityLongitude());
            String body = Http.text(url, null);
            Forecast forecast = ForecastParser.parse(body);
            forecast.fetchedAt = System.currentTimeMillis();
            JsonCache.write(context, cacheKey, body);
            return new Result(forecast, false, Error.NONE);
        } catch (IOException e) {
            Forecast old = cached == null ? null : parseOrNull(cached);
            if (old != null) return new Result(old, true, Error.NONE);
            return new Result(null, false, e.getMessage() != null && e.getMessage().startsWith("HTTP") ? Error.FAILED : Error.OFFLINE);
        } catch (JSONException | RuntimeException e) {
            return new Result(null, false, Error.FAILED);
        }
    }

    private static Forecast parseOrNull(JsonCache.Entry entry) {
        try {
            Forecast forecast = ForecastParser.parse(entry.body);
            forecast.fetchedAt = entry.fetchedAt;
            return forecast;
        } catch (JSONException e) {
            return null;
        }
    }
}
