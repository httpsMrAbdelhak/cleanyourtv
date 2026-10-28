package com.cleanyourtv.launcher;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Reads what an app publishes for the Android TV home screen: its Watch Next entries (continue watching, next
 * episodes, new, and its watchlist) and its preview channels, through the TV's own content provider. This is the
 * documented way an app puts rows on the TV home screen; no accessibility service or notification access is
 * involved. It needs READ_TV_LISTINGS, which the user grants.
 */
final class TvChannels {
    private TvChannels() { }

    static final String PERMISSION = "android.permission.READ_TV_LISTINGS";

    enum Status { OK, NO_SOURCE, NO_PERMISSION, NOTHING, FAILED }

    /** The translated row names the reader needs. */
    static final class Labels {
        final String resume;
        final String next;
        final String fresh;
        final String myList;

        Labels(String resume, String next, String fresh, String myList) {
            this.resume = resume;
            this.next = next;
            this.fresh = fresh;
            this.myList = myList;
        }
    }

    static final class Result {
        final Status status;
        /** Home: continue watching, next, new, then the channels; the watchlist is left out. */
        final List<TvEntry> home;
        /** "My list": the app's watchlist. */
        final List<TvEntry> watchlist;
        /** Every row by name, in the app's order, for the Library page. */
        final Map<String, List<TvEntry>> sections;

        Result(Status status, List<TvEntry> home, List<TvEntry> watchlist, Map<String, List<TvEntry>> sections) {
            this.status = status;
            this.home = home;
            this.watchlist = watchlist;
            this.sections = sections;
        }

        static Result of(Status status) {
            return new Result(status, new ArrayList<TvEntry>(), new ArrayList<TvEntry>(), new LinkedHashMap<String, List<TvEntry>>());
        }
    }

    interface Callback {
        void onResult(Result result);
    }

    private static final int MAX_CHANNELS = 8;
    private static final int MAX_PER_ROW = 24;
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "tv-channels");
        thread.setDaemon(true);
        return thread;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    static boolean permissionGranted(Context context) {
        return context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    static void load(final Context context, final String sourcePackage, final Labels labels, final Callback callback) {
        final Context app = context.getApplicationContext();
        POOL.execute(() -> {
            final Result result = query(app, sourcePackage, labels);
            new Prefs(app).setStatus("channels", statusCode(result.status));
            MAIN.post(() -> callback.onResult(result));
        });
    }

    private static String statusCode(Status status) {
        switch (status) {
            case OK: return "ok:" + System.currentTimeMillis();
            case NO_SOURCE: return "no_source";
            case NO_PERMISSION: return "no_permission";
            case NOTHING: return "nothing";
            default: return "failed";
        }
    }

    private static Result query(Context context, String sourcePackage, Labels labels) {
        if (sourcePackage == null) return Result.of(Status.NO_SOURCE);
        if (!permissionGranted(context)) return Result.of(Status.NO_PERMISSION);
        ContentResolver resolver = context.getContentResolver();
        Map<String, List<TvEntry>> sections = new LinkedHashMap<>();
        List<TvEntry> resume = new ArrayList<>();
        List<TvEntry> next = new ArrayList<>();
        List<TvEntry> fresh = new ArrayList<>();
        List<TvEntry> watchlist = new ArrayList<>();
        List<TvEntry> home = new ArrayList<>();
        try {
            readWatchNext(resolver, sourcePackage, labels, resume, next, fresh, watchlist);
            put(sections, labels.resume, resume);
            put(sections, labels.next, next);
            put(sections, labels.fresh, fresh);
            put(sections, labels.myList, watchlist);
            home.addAll(resume);
            home.addAll(next);
            home.addAll(fresh);
            for (Channel channel : previewChannels(resolver, sourcePackage)) {
                List<TvEntry> row = readChannel(resolver, channel);
                put(sections, channel.name, row);
                home.addAll(row);
            }
        } catch (RuntimeException e) {
            // SecurityException included: the permission is declared but this TV refuses a third-party home screen.
            return Result.of(Status.FAILED);
        }
        if (sections.isEmpty()) return Result.of(Status.NOTHING);
        return new Result(Status.OK, home, watchlist, sections);
    }

    private static void put(Map<String, List<TvEntry>> sections, String name, List<TvEntry> row) {
        if (row.isEmpty()) return;
        List<TvEntry> existing = sections.get(name);
        if (existing == null) sections.put(name, row);
        else existing.addAll(row);
    }

    private static final class Channel {
        final long id;
        final String name;

        Channel(long id, String name) {
            this.id = id;
            this.name = name == null ? "" : name;
        }
    }

    private static List<Channel> previewChannels(ContentResolver resolver, String sourcePackage) {
        List<Channel> found = new ArrayList<>();
        String[] projection = {TvContract.Channels._ID, TvContract.Channels.COLUMN_PACKAGE_NAME,
                TvContract.Channels.COLUMN_DISPLAY_NAME, TvContract.Channels.COLUMN_TYPE};
        try (Cursor cursor = resolver.query(TvContract.Channels.CONTENT_URI, projection, null, null, null)) {
            if (cursor == null) return found;
            while (cursor.moveToNext() && found.size() < MAX_CHANNELS) {
                // The provider may ignore a selection, so the app and the channel type are checked here.
                if (!sourcePackage.equals(cursor.getString(1))) continue;
                if (!TvContract.Channels.TYPE_PREVIEW.equals(cursor.getString(3))) continue;
                found.add(new Channel(cursor.getLong(0), cursor.getString(2)));
            }
        }
        return found;
    }

    /** Columns every program table has (documented in TvContract; the season, episode and date ones by name). */
    private static final String[] COMMON = {
            TvContract.PreviewPrograms.COLUMN_TITLE,
            TvContract.PreviewPrograms.COLUMN_SHORT_DESCRIPTION,
            TvContract.PreviewPrograms.COLUMN_EPISODE_TITLE,
            "season_display_number",
            "episode_display_number",
            TvContract.PreviewPrograms.COLUMN_POSTER_ART_URI,
            TvContract.PreviewPrograms.COLUMN_POSTER_ART_ASPECT_RATIO,
            TvContract.PreviewPrograms.COLUMN_THUMBNAIL_URI,
            TvContract.PreviewPrograms.COLUMN_THUMBNAIL_ASPECT_RATIO,
            TvContract.PreviewPrograms.COLUMN_INTENT_URI,
            TvContract.PreviewPrograms.COLUMN_TYPE,
            TvContract.PreviewPrograms.COLUMN_DURATION_MILLIS,
            TvContract.PreviewPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
            "release_date",
            TvContract.PreviewPrograms.COLUMN_PACKAGE_NAME,
    };

    private static String[] with(String... extra) {
        String[] columns = java.util.Arrays.copyOf(COMMON, COMMON.length + extra.length);
        System.arraycopy(extra, 0, columns, COMMON.length, extra.length);
        return columns;
    }

    private static void readWatchNext(ContentResolver resolver, String sourcePackage, Labels labels,
                                      List<TvEntry> resume, List<TvEntry> next, List<TvEntry> fresh, List<TvEntry> watchlist) {
        String[] projection = with(TvContract.WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE,
                TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS);
        List<Row> rows = new ArrayList<>();
        try (Cursor c = resolver.query(TvContract.WatchNextPrograms.CONTENT_URI, projection, null, null, null)) {
            if (c == null) return;
            int pkg = c.getColumnIndex(TvContract.PreviewPrograms.COLUMN_PACKAGE_NAME);
            int type = c.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE);
            int engaged = c.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS);
            while (c.moveToNext()) {
                if (!sourcePackage.equals(c.getString(pkg))) continue;
                int kind = c.isNull(type) ? TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE : c.getInt(type);
                String section;
                if (kind == TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_WATCHLIST) section = labels.myList;
                else if (kind == TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_NEXT) section = labels.next;
                else if (kind == TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_NEW) section = labels.fresh;
                else section = labels.resume;
                rows.add(new Row(entryOf(c, section, kind == TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE),
                        c.isNull(engaged) ? 0 : c.getLong(engaged), kind));
            }
        }
        sortByOrder(rows);
        for (Row row : rows) {
            List<TvEntry> into;
            if (row.kind == TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_WATCHLIST) into = watchlist;
            else if (row.kind == TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_NEXT) into = next;
            else if (row.kind == TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_NEW) into = fresh;
            else into = resume;
            if (into.size() < MAX_PER_ROW) into.add(row.entry);
        }
    }

    private static List<TvEntry> readChannel(ContentResolver resolver, Channel channel) {
        Uri uri = TvContract.buildPreviewProgramsUriForChannel(channel.id);
        List<Row> rows = new ArrayList<>();
        try (Cursor c = resolver.query(uri, with(TvContract.PreviewPrograms.COLUMN_WEIGHT), null, null, null)) {
            if (c != null) {
                int weight = c.getColumnIndex(TvContract.PreviewPrograms.COLUMN_WEIGHT);
                while (c.moveToNext()) rows.add(new Row(entryOf(c, channel.name, false), c.isNull(weight) ? 0 : c.getLong(weight), 0));
            }
        }
        sortByOrder(rows);
        List<TvEntry> out = new ArrayList<>();
        for (Row row : rows) {
            if (out.size() >= MAX_PER_ROW) break;
            out.add(row.entry);
        }
        return out;
    }

    private static final class Row {
        final TvEntry entry;
        final long order;
        final int kind;

        Row(TvEntry entry, long order, int kind) {
            this.entry = entry;
            this.order = order;
            this.kind = kind;
        }
    }

    /** Heaviest (previews) or most recently watched (Watch Next) first. */
    private static void sortByOrder(List<Row> rows) {
        Collections.sort(rows, (a, b) -> Long.compare(b.order, a.order));
    }

    private static TvEntry entryOf(Cursor c, String section, boolean resume) {
        String title = text(c, TvContract.PreviewPrograms.COLUMN_TITLE);
        String episode = text(c, TvContract.PreviewPrograms.COLUMN_EPISODE_TITLE);
        String description = text(c, TvContract.PreviewPrograms.COLUMN_SHORT_DESCRIPTION);
        String poster = text(c, TvContract.PreviewPrograms.COLUMN_POSTER_ART_URI);
        String thumb = text(c, TvContract.PreviewPrograms.COLUMN_THUMBNAIL_URI);
        int type = number(c, TvContract.PreviewPrograms.COLUMN_TYPE, -1);
        boolean episodeType = type == ProgramKind.TYPE_TV_EPISODE;
        boolean posterWide = isLandscape(number(c, TvContract.PreviewPrograms.COLUMN_POSTER_ART_ASPECT_RATIO,
                TvContract.PreviewPrograms.ASPECT_RATIO_2_3));
        boolean thumbWide = isLandscape(number(c, TvContract.PreviewPrograms.COLUMN_THUMBNAIL_ASPECT_RATIO,
                TvContract.PreviewPrograms.ASPECT_RATIO_16_9));
        long duration = longNumber(c, TvContract.PreviewPrograms.COLUMN_DURATION_MILLIS);
        long position = longNumber(c, TvContract.PreviewPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS);
        return new TvEntry(section, title, episode.isEmpty() ? description : episode,
                ShapePolicy.poster(poster, thumb), ShapePolicy.wide(poster, posterWide, thumb, thumbWide, episodeType),
                text(c, TvContract.PreviewPrograms.COLUMN_INTENT_URI),
                ProgramKind.progress(position, duration), CardText.remaining(position, duration),
                CardText.badge(text(c, "season_display_number"), text(c, "episode_display_number")),
                CardText.year(text(c, "release_date")), ProgramKind.of(type), resume);
    }

    private static boolean isLandscape(int aspect) {
        return aspect == TvContract.PreviewPrograms.ASPECT_RATIO_16_9
                || aspect == TvContract.PreviewPrograms.ASPECT_RATIO_3_2
                || aspect == TvContract.PreviewPrograms.ASPECT_RATIO_4_3;
    }

    private static String text(Cursor c, String column) {
        int i = c.getColumnIndex(column);
        if (i < 0 || c.isNull(i)) return "";
        String value = c.getString(i);
        return value == null ? "" : value;
    }

    private static int number(Cursor c, String column, int fallback) {
        int i = c.getColumnIndex(column);
        return i < 0 || c.isNull(i) ? fallback : c.getInt(i);
    }

    private static long longNumber(Cursor c, String column) {
        int i = c.getColumnIndex(column);
        return i < 0 || c.isNull(i) ? -1 : c.getLong(i);
    }
}
