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
 * Trending films and series from The Movie Database (TMDB). Needs a key: nothing is shown without one, and
 * nothing is invented. Answers are cached for six hours; when the network fails the last answer is shown
 * and marked as old. This product uses the TMDB API but is not endorsed or certified by TMDB.
 */
final class Tmdb {
    private Tmdb() { }

    enum Feed {
        TRENDING("trending/all/week", false),
        MOVIES("movie/popular", false),
        SERIES("tv/popular", true),
        NEW("movie/now_playing", false);

        final String path;
        final boolean series;

        Feed(String path, boolean series) {
            this.path = path;
            this.series = series;
        }
    }

    /** What a load produced. items is empty with error set when there is nothing to show. */
    static final class Result {
        final List<MediaItem> items;
        /** Epoch millis of the data shown, 0 when none. */
        final long fetchedAt;
        /** True when the network failed and the items come from an old cache. */
        final boolean stale;
        final Error error;

        Result(List<MediaItem> items, long fetchedAt, boolean stale, Error error) {
            this.items = items;
            this.fetchedAt = fetchedAt;
            this.stale = stale;
            this.error = error;
        }
    }

    enum Error { NONE, NO_KEY, OFFLINE, BAD_KEY, FAILED }

    interface Callback {
        void onResult(Result result);
    }

    private static final String API = "https://api.themoviedb.org/3/";
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

    static void load(final Context context, final Feed feed, final boolean forceNetwork, final Callback callback) {
        final Context app = context.getApplicationContext();
        final Prefs prefs = new Prefs(app);
        POOL.execute(() -> {
            final Result result = fetch(app, prefs, feed, forceNetwork);
            prefs.setStatus("tmdb", statusOf(result));
            MAIN.post(() -> callback.onResult(result));
        });
    }

    private static String statusOf(Result result) {
        if (result.error == Error.NONE) return (result.stale ? "stale:" : "ok:") + result.fetchedAt;
        return result.error.name().toLowerCase(Locale.ROOT);
    }

    private static Result fetch(Context context, Prefs prefs, Feed feed, boolean forceNetwork) {
        String key = key(prefs);
        if (key.isEmpty()) return new Result(new ArrayList<MediaItem>(), 0, false, Error.NO_KEY);
        String cacheKey = "tmdb_" + feed.name() + "_" + language();
        JsonCache.Entry cached = JsonCache.read(context, cacheKey);
        long now = System.currentTimeMillis();
        if (cached != null && !forceNetwork && CachePolicy.isFresh(now, cached.fetchedAt, CachePolicy.TMDB_TTL_MS)) {
            List<MediaItem> items = parseOrNull(cached.body, feed);
            if (items != null) return new Result(items, cached.fetchedAt, false, Error.NONE);
        }
        try {
            boolean bearer = key.startsWith("eyJ");
            String url = API + feed.path + "?language=" + language() + (bearer ? "" : "&api_key=" + key);
            String body = Http.text(url, bearer ? key : null);
            List<MediaItem> items = TmdbParser.parse(body, feed.series);
            JsonCache.write(context, cacheKey, body);
            return new Result(items, System.currentTimeMillis(), false, Error.NONE);
        } catch (IOException e) {
            Error error = e.getMessage() != null && e.getMessage().startsWith("HTTP 401") ? Error.BAD_KEY
                    : (e.getMessage() != null && e.getMessage().startsWith("HTTP") ? Error.FAILED : Error.OFFLINE);
            if (error != Error.BAD_KEY && cached != null) {
                List<MediaItem> items = parseOrNull(cached.body, feed);
                if (items != null) return new Result(items, cached.fetchedAt, true, Error.NONE);
            }
            return new Result(new ArrayList<MediaItem>(), 0, false, error);
        } catch (JSONException | RuntimeException e) {
            return new Result(new ArrayList<MediaItem>(), 0, false, Error.FAILED);
        }
    }

    private static List<MediaItem> parseOrNull(String body, Feed feed) {
        try {
            return TmdbParser.parse(body, feed.series);
        } catch (JSONException e) {
            return null;
        }
    }
}
