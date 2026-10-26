package com.cleanyourtv.launcher;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
 * The most popular videos of YouTube in the TV's country, through the YouTube Data API v3 (videos.list,
 * chart=mostPopular). It needs a free API key from Google Cloud (YouTube Data API v3): without one the panel
 * says so and shows nothing else. Cached for an hour; the last answer is kept and labelled when offline.
 */
final class YouTube {
    private YouTube() { }

    static final class Result {
        final List<YouTubeVideo> videos;
        final long fetchedAt;
        final boolean stale;
        final Tmdb.Error error;

        Result(List<YouTubeVideo> videos, long fetchedAt, boolean stale, Tmdb.Error error) {
            this.videos = videos;
            this.fetchedAt = fetchedAt;
            this.stale = stale;
            this.error = error;
        }
    }

    interface Callback {
        void onResult(Result result);
    }

    private static final long TTL_MS = 60L * 60 * 1000;
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "youtube");
        thread.setDaemon(true);
        return thread;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    static boolean hasKey(Prefs prefs) {
        return !prefs.youtubeKey().isEmpty();
    }

    static void load(final Context context, final boolean force, final Callback callback) {
        final Context app = context.getApplicationContext();
        final Prefs prefs = new Prefs(app);
        POOL.execute(() -> {
            final Result result = fetch(app, prefs, force);
            prefs.setStatus("youtube", result.error == Tmdb.Error.NONE
                    ? (result.stale ? "stale:" : "ok:") + result.fetchedAt : result.error.name().toLowerCase(Locale.ROOT));
            MAIN.post(() -> callback.onResult(result));
        });
    }

    private static Result fetch(Context context, Prefs prefs, boolean force) {
        if (!hasKey(prefs)) return new Result(new ArrayList<YouTubeVideo>(), 0, false, Tmdb.Error.NO_KEY);
        String region = Region.of(Locale.getDefault());
        String cacheKey = "youtube_popular_" + region;
        JsonCache.Entry cached = JsonCache.read(context, cacheKey);
        long now = System.currentTimeMillis();
        if (cached != null && !force && CachePolicy.isFresh(now, cached.fetchedAt, TTL_MS)) {
            List<YouTubeVideo> videos = parseOrNull(cached.body);
            if (videos != null) return new Result(videos, cached.fetchedAt, false, Tmdb.Error.NONE);
        }
        try {
            String url = "https://www.googleapis.com/youtube/v3/videos?part=snippet&chart=mostPopular&maxResults=15"
                    + "&regionCode=" + region + "&key=" + prefs.youtubeKey();
            String body = Http.text(url, null);
            List<YouTubeVideo> videos = YouTubeParser.parse(body);
            JsonCache.write(context, cacheKey, body);
            return new Result(videos, System.currentTimeMillis(), false, Tmdb.Error.NONE);
        } catch (IOException e) {
            String message = e.getMessage();
            boolean badKey = message != null && (message.startsWith("HTTP 400") || message.startsWith("HTTP 403"));
            Tmdb.Error error = badKey ? Tmdb.Error.BAD_KEY : Tmdb.errorOf(e);
            if (!badKey && cached != null) {
                List<YouTubeVideo> videos = parseOrNull(cached.body);
                if (videos != null) return new Result(videos, cached.fetchedAt, true, Tmdb.Error.NONE);
            }
            return new Result(new ArrayList<YouTubeVideo>(), 0, false, error);
        } catch (JSONException | RuntimeException e) {
            return new Result(new ArrayList<YouTubeVideo>(), 0, false, Tmdb.Error.FAILED);
        }
    }

    private static List<YouTubeVideo> parseOrNull(String body) {
        try {
            return YouTubeParser.parse(body);
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Opens a video in the YouTube app of the dock (a normal https watch link, the documented way to open a
     * video in YouTube). Falls back to any app that handles the link. False when nothing could open it.
     */
    static boolean open(Context context, String videoId, String preferredPackage) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + Uri.encode(videoId)))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            if (preferredPackage != null) {
                context.startActivity(new Intent(intent).setPackage(preferredPackage));
                return true;
            }
        } catch (ActivityNotFoundException | SecurityException e) {
            // try again without naming the app
        }
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }
}
