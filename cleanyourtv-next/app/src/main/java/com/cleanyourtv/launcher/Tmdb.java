package com.cleanyourtv.launcher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The Movie Database, used for two things: the trending films and series of the Explore page, and the poster of
 * the series or film behind each card the streaming app published (the app often gives the still of an episode).
 * The key is the user's own: typed in Settings, or given to the build as TMDB_API_KEY. It is never in the
 * repository. Without a key nothing is asked and the app's own pictures stay.
 */
final class Tmdb {
    private Tmdb() { }

    enum Error { NONE, NO_KEY, BAD_KEY, OFFLINE, FAILED }

    static final class Trending {
        final Error error;
        final boolean stale;
        final List<TvEntry> movies;
        final List<TvEntry> series;

        Trending(Error error, boolean stale, List<TvEntry> movies, List<TvEntry> series) {
            this.error = error;
            this.stale = stale;
            this.movies = movies;
            this.series = series;
        }
    }

    interface TrendingCallback {
        void onResult(Trending result);
    }

    interface PostersCallback {
        /** Called once with what the cache knows, and once more if the network changed anything. */
        void onResult(TvChannels.Result result);
    }

    private static final String API = "https://api.themoviedb.org/3";
    private static final long TRENDING_TTL_MS = 6L * 60 * 60 * 1000;
    private static final long ART_TTL_MS = 30L * 24 * 60 * 60 * 1000;
    private static final long MISS_TTL_MS = 3L * 24 * 60 * 60 * 1000;
    /** At most this many titles are looked up on the network per load; the next load does the rest. */
    private static final int MAX_LOOKUPS = 60;

    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "tmdb");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });
    private static final ExecutorService LOOKUPS = Executors.newFixedThreadPool(3, runnable -> {
        Thread thread = new Thread(runnable, "tmdb-lookup");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<String, TmdbParser.Art> MEMORY = new HashMap<>();
    private static final TmdbParser.Art NONE = new TmdbParser.Art("", "");

    /** The key typed in Settings, else the one given to the build, else empty. */
    static String key(Prefs prefs) {
        String typed = prefs.tmdbKey();
        if (!typed.isEmpty()) return typed;
        return BuildConfig.TMDB_KEY == null ? "" : BuildConfig.TMDB_KEY.trim();
    }

    static boolean hasKey(Prefs prefs) {
        return !key(prefs).isEmpty();
    }

    private static String language() {
        return Locale.getDefault().getLanguage().equals("fr") ? "fr-FR" : "en-US";
    }

    /** A v3 key goes in the address; a v4 read token (a long "eyJ..." string) goes in the Authorization header. */
    private static String get(String key, String path) throws IOException {
        boolean token = key.length() > 40;
        String url = API + path + (path.contains("?") ? "&" : "?") + "language=" + language()
                + (token ? "" : "&api_key=" + URLEncoder.encode(key, "UTF-8"));
        return Http.text(url, token ? key : null);
    }

    private static Error errorOf(IOException e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        if (message.equals("HTTP 401")) return Error.BAD_KEY;
        return message.startsWith("HTTP") ? Error.FAILED : Error.OFFLINE;
    }

    private static String statusCode(Error error, long time) {
        switch (error) {
            case NONE: return "ok:" + time;
            case NO_KEY: return "no_key";
            case BAD_KEY: return "bad_key";
            case OFFLINE: return "offline";
            default: return "failed";
        }
    }

    // --- trending ---------------------------------------------------------------------------------------

    static void trending(final Context context, final String movieSection, final String seriesSection,
                         final TrendingCallback callback) {
        final Context app = context.getApplicationContext();
        final Prefs prefs = new Prefs(app);
        POOL.execute(() -> {
            String key = key(prefs);
            Trending result;
            if (key.isEmpty()) {
                result = new Trending(Error.NO_KEY, false, new ArrayList<TvEntry>(), new ArrayList<TvEntry>());
            } else {
                Locale locale = Locale.getDefault();
                Part movies = trendingPart(app, key, false, movieSection, locale);
                Part series = trendingPart(app, key, true, seriesSection, locale);
                Error error = movies.entries.isEmpty() && series.entries.isEmpty()
                        ? (movies.error != Error.NONE ? movies.error : series.error) : Error.NONE;
                result = new Trending(error, movies.stale || series.stale, movies.entries, series.entries);
            }
            prefs.setStatus("tmdb", result.stale ? "stale:" + System.currentTimeMillis()
                    : statusCode(result.error, System.currentTimeMillis()));
            final Trending done = result;
            MAIN.post(() -> callback.onResult(done));
        });
    }

    private static final class Part {
        final List<TvEntry> entries;
        final boolean stale;
        final Error error;

        Part(List<TvEntry> entries, boolean stale, Error error) {
            this.entries = entries;
            this.stale = stale;
            this.error = error;
        }
    }

    private static Part trendingPart(Context context, String key, boolean tv, String section, Locale locale) {
        String cacheKey = "tmdb_trending_" + (tv ? "tv" : "movie") + "_" + language();
        JsonCache.Entry cached = JsonCache.read(context, cacheKey);
        long now = System.currentTimeMillis();
        if (cached != null && CachePolicy.isFresh(now, cached.fetchedAt, TRENDING_TTL_MS)) {
            List<TvEntry> entries = parseTrending(cached.body, tv, section, locale);
            if (entries != null) return new Part(entries, false, Error.NONE);
        }
        try {
            String body = get(key, "/trending/" + (tv ? "tv" : "movie") + "/week");
            List<TvEntry> entries = parseTrending(body, tv, section, locale);
            if (entries == null) return new Part(new ArrayList<TvEntry>(), false, Error.FAILED);
            JsonCache.write(context, cacheKey, body);
            return new Part(entries, false, Error.NONE);
        } catch (IOException e) {
            List<TvEntry> old = cached == null ? null : parseTrending(cached.body, tv, section, locale);
            if (old != null && !old.isEmpty()) return new Part(old, true, Error.NONE);
            return new Part(new ArrayList<TvEntry>(), false, errorOf(e));
        }
    }

    private static List<TvEntry> parseTrending(String body, boolean tv, String section, Locale locale) {
        try {
            List<TvEntry> out = new ArrayList<>();
            for (TmdbParser.Title title : TmdbParser.trending(body, tv)) {
                // No intent: OK on a trending title opens the streaming app, where the user searches for it.
                out.add(new TvEntry(section, title.title, "", title.art.poster,
                        title.art.backdrop.isEmpty() ? title.art.poster : title.art.backdrop, "", -1f, -1L,
                        TmdbParser.rating(title.vote, locale), title.year,
                        tv ? TvEntry.Kind.SERIES : TvEntry.Kind.MOVIE, false));
            }
            return out;
        } catch (JSONException | RuntimeException e) {
            return null;
        }
    }

    // --- posters for the streaming app's cards ----------------------------------------------------------

    /**
     * Replaces the pictures of the app's cards with the TMDB poster and backdrop of their series or film. The
     * callback gets the cached answers first (at once when every title was seen before), then, if any title had
     * to be looked up, the result with the new pictures. Titles TMDB does not know keep the app's pictures.
     */
    static void posters(final Context context, final TvChannels.Result result, final PostersCallback callback) {
        final Context app = context.getApplicationContext();
        final Prefs prefs = new Prefs(app);
        POOL.execute(() -> {
            final String key = key(prefs);
            if (key.isEmpty() || result.status != TvChannels.Status.OK) {
                MAIN.post(() -> callback.onResult(result));
                return;
            }
            Map<String, TvEntry> wanted = new LinkedHashMap<>();
            for (TvEntry entry : PosterSwap.all(result)) wanted.put(PosterSwap.lookupKey(entry), entry);

            Map<String, TmdbParser.Art> found = new HashMap<>();
            List<String> missing = new ArrayList<>();
            for (Map.Entry<String, TvEntry> item : wanted.entrySet()) {
                TmdbParser.Art art = cached(app, item.getKey());
                if (art != null) found.put(item.getKey(), art);
                else missing.add(item.getKey());
            }
            final TvChannels.Result first = PosterSwap.apply(result, found);
            MAIN.post(() -> callback.onResult(first));
            if (missing.isEmpty()) return;

            List<Future<TmdbParser.Art>> jobs = new ArrayList<>();
            final List<String> asked = missing.subList(0, Math.min(missing.size(), MAX_LOOKUPS));
            for (final String lookup : asked) {
                final TvEntry entry = wanted.get(lookup);
                jobs.add(LOOKUPS.submit((Callable<TmdbParser.Art>) () -> lookup(app, key, lookup, entry)));
            }
            boolean changed = false;
            for (int i = 0; i < jobs.size(); i++) {
                try {
                    TmdbParser.Art art = jobs.get(i).get();
                    if (art != null && !art.isEmpty()) {
                        found.put(asked.get(i), art);
                        changed = true;
                    }
                } catch (Exception e) {
                    // that title keeps the app's own picture
                }
            }
            if (!changed) return;
            final TvChannels.Result second = PosterSwap.apply(result, found);
            MAIN.post(() -> callback.onResult(second));
        });
    }

    private static TmdbParser.Art cached(Context context, String lookup) {
        synchronized (MEMORY) {
            TmdbParser.Art art = MEMORY.get(lookup);
            if (art != null) return art;
        }
        JsonCache.Entry entry = JsonCache.read(context, "tmdb_art_" + language() + "_" + lookup);
        if (entry == null) return null;
        try {
            JSONObject saved = new JSONObject(entry.body);
            TmdbParser.Art art = new TmdbParser.Art(saved.optString("p", ""), saved.optString("b", ""));
            long ttl = art.isEmpty() ? MISS_TTL_MS : ART_TTL_MS;
            if (!CachePolicy.isFresh(System.currentTimeMillis(), entry.fetchedAt, ttl)) return null;
            remember(lookup, art);
            return art;
        } catch (JSONException e) {
            return null;
        }
    }

    private static void remember(String lookup, TmdbParser.Art art) {
        synchronized (MEMORY) {
            if (MEMORY.size() > 400) MEMORY.clear();
            MEMORY.put(lookup, art);
        }
    }

    private static TmdbParser.Art lookup(Context context, String key, String lookup, TvEntry entry) throws IOException {
        TmdbIds.Ref ref = TmdbIds.from(entry.intentUri);
        boolean tv = PosterSwap.isSeries(entry, ref);
        TmdbParser.Art art;
        try {
            if (ref.tmdbId != null) {
                art = TmdbParser.details(get(key, "/" + (tv ? "tv" : "movie") + "/" + ref.tmdbId));
            } else if (ref.imdbId != null) {
                art = TmdbParser.find(get(key, "/find/" + ref.imdbId + "?external_source=imdb_id"), tv);
            } else {
                String query = TitleKey.query(entry.title);
                if (query.isEmpty()) return null;
                String q = URLEncoder.encode(query, "UTF-8");
                String path;
                if (!PosterSwap.kindKnown(entry, ref)) path = "/search/multi?query=" + q;
                else if (tv) path = "/search/tv?query=" + q;
                else path = "/search/movie?query=" + q + (entry.year.isEmpty() ? "" : "&year=" + entry.year);
                art = TmdbParser.bestMatch(get(key, path), query, entry.year);
            }
        } catch (JSONException e) {
            art = null;
        }
        TmdbParser.Art saved = art == null ? NONE : art;
        remember(lookup, saved);
        try {
            JSONObject json = new JSONObject();
            json.put("p", saved.poster);
            json.put("b", saved.backdrop);
            JsonCache.write(context, "tmdb_art_" + language() + "_" + lookup, json.toString());
        } catch (JSONException e) {
            // not cached: looked up again next time
        }
        return art;
    }

}
