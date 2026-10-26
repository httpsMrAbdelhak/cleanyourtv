package com.cleanyourtv.launcher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Films and series from The Movie Database (TMDB). Needs a key: nothing is shown without one, and nothing is
 * invented. Answers are cached for six hours; when the network fails the last answer is shown and marked
 * as old. For a streaming app, the list is the service's own catalogue, found by matching the app to TMDB's
 * list of watch providers by name. This product uses the TMDB API but is not endorsed or certified by TMDB.
 */
final class Tmdb {
    private Tmdb() { }

    /** What a load produced. items is empty with error set when there is nothing to show. */
    static final class Result {
        final List<MediaItem> items;
        /** Epoch millis of the data shown, 0 when none. */
        final long fetchedAt;
        /** True when the network failed and the items come from an old cache. */
        final boolean stale;
        final Error error;
        /** True when an app was asked for but TMDB has no matching service, so general lists were used. */
        final boolean providerMissing;

        Result(List<MediaItem> items, long fetchedAt, boolean stale, Error error, boolean providerMissing) {
            this.items = items;
            this.fetchedAt = fetchedAt;
            this.stale = stale;
            this.error = error;
            this.providerMissing = providerMissing;
        }
    }

    enum Error { NONE, NO_KEY, OFFLINE, BAD_KEY, FAILED }

    interface Callback {
        void onResult(Result result);
    }

    private static final String API = "https://api.themoviedb.org/3/";
    private static final long PROVIDERS_TTL_MS = 7L * 24 * 60 * 60 * 1000;
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "tmdb");
        thread.setDaemon(true);
        return thread;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** The key to use: the one typed in the settings, else the one built into the APK (may be empty). */
    static String key(Prefs prefs) {
        String typed = prefs.tmdbKey();
        return typed.isEmpty() ? BuildConfig.TMDB_KEY : typed;
    }

    static boolean hasKey(Prefs prefs) {
        return !key(prefs).isEmpty();
    }

    static String language() {
        return Locale.getDefault().getLanguage().equals("fr") ? "fr-FR" : "en-US";
    }

    /** Poster and backdrop URLs. Sizes are TMDB's documented w185, w342 and w780. */
    static String posterUrl(String path, boolean light) {
        return "https://image.tmdb.org/t/p/" + (light ? "w185" : "w342") + path;
    }

    static String backdropUrl(String path) {
        return "https://image.tmdb.org/t/p/w780" + path;
    }

    static void load(final Context context, final FeedSpec spec, final boolean forceNetwork, final Callback callback) {
        final Context app = context.getApplicationContext();
        final Prefs prefs = new Prefs(app);
        POOL.execute(() -> {
            final Result result = fetch(app, prefs, spec, forceNetwork);
            prefs.setStatus("tmdb", statusOf(result));
            MAIN.post(() -> callback.onResult(result));
        });
    }

    private static String statusOf(Result result) {
        if (result.error == Error.NONE) return (result.stale ? "stale:" : "ok:") + result.fetchedAt;
        return result.error.name().toLowerCase(Locale.ROOT);
    }

    private static Result fetch(Context context, Prefs prefs, FeedSpec spec, boolean forceNetwork) {
        String key = key(prefs);
        if (key.isEmpty()) return new Result(new ArrayList<MediaItem>(), 0, false, Error.NO_KEY, false);
        String region = Region.of(Locale.getDefault());
        String cacheKey = spec.cacheKey(language(), region);
        JsonCache.Entry cached = JsonCache.read(context, cacheKey);
        long now = System.currentTimeMillis();
        if (cached != null && !forceNetwork && CachePolicy.isFresh(now, cached.fetchedAt, CachePolicy.TMDB_TTL_MS)) {
            List<MediaItem> items = parseOrNull(cached.body, spec);
            if (items != null) return new Result(items, cached.fetchedAt, false, Error.NONE, false);
        }
        try {
            String extra = spec.query;
            if (spec.providerPackage != null) {
                int provider = providerId(context, key, spec.providerPackage, region);
                if (provider == 0) {
                    // TMDB has no service by that name here: say so and fall back to the general list.
                    Result general = fetch(context, prefs, spec.series ? FeedSpec.popular(true) : FeedSpec.trending(), forceNetwork);
                    return new Result(general.items, general.fetchedAt, general.stale, general.error, true);
                }
                extra += "&with_watch_providers=" + provider + "&watch_region=" + region;
            }
            String body = get(key, spec.path, "&language=" + language() + extra);
            List<MediaItem> items = TmdbParser.parse(body, spec.series);
            JsonCache.write(context, cacheKey, body);
            return new Result(items, System.currentTimeMillis(), false, Error.NONE, false);
        } catch (IOException e) {
            Error error = errorOf(e);
            if (error != Error.BAD_KEY && cached != null) {
                List<MediaItem> items = parseOrNull(cached.body, spec);
                if (items != null) return new Result(items, cached.fetchedAt, true, Error.NONE, false);
            }
            return new Result(new ArrayList<MediaItem>(), 0, false, error, false);
        } catch (JSONException | RuntimeException e) {
            return new Result(new ArrayList<MediaItem>(), 0, false, Error.FAILED, false);
        }
    }

    /** The TMDB id of the streaming service behind an app, from TMDB's own provider list; 0 when none matches. */
    private static int providerId(Context context, String key, String packageName, String region) throws IOException, JSONException {
        String[] names = ProviderMap.namesFor(packageName);
        if (names == null) return 0;
        String cacheKey = "tmdb_providers_" + region;
        JsonCache.Entry cached = JsonCache.read(context, cacheKey);
        String body;
        if (cached != null && CachePolicy.isFresh(System.currentTimeMillis(), cached.fetchedAt, PROVIDERS_TTL_MS)) {
            body = cached.body;
        } else {
            body = get(key, "watch/providers/movie", "&language=en-US&watch_region=" + region);
            JsonCache.write(context, cacheKey, body);
        }
        return ProviderParser.find(body, names);
    }

    private static String get(String key, String path, String params) throws IOException {
        boolean bearer = key.startsWith("eyJ");
        String url = API + path + "?" + (bearer ? "" : "api_key=" + key) + params;
        return Http.text(url, bearer ? key : null);
    }

    static Error errorOf(IOException e) {
        String message = e.getMessage();
        if (message != null && message.startsWith("HTTP 401")) return Error.BAD_KEY;
        if (message != null && message.startsWith("HTTP")) return Error.FAILED;
        return Error.OFFLINE;
    }

    private static List<MediaItem> parseOrNull(String body, FeedSpec spec) {
        try {
            return TmdbParser.parse(body, spec.series);
        } catch (JSONException e) {
            return null;
        }
    }
}
