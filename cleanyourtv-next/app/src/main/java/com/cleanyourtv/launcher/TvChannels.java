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
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Reads what an app publishes for the Android TV home screen: its "Continue watching" entries (Watch Next)
 * and its preview channels, through the TV's own content provider. This is the documented way an app puts
 * rows on the TV home screen; it is not a private data of the app, and no accessibility service or
 * notification access is involved. It needs the READ_TV_LISTINGS permission, which the user grants.
 *
 * What the TV answers is not known in advance: some firmware may refuse a third-party home screen even with
 * the permission, and an app publishes nothing unless its own settings turn that on. Each case has its own
 * status so that the screen can say which it is.
 */
final class TvChannels {
    private TvChannels() { }

    static final String PERMISSION = "android.permission.READ_TV_LISTINGS";

    enum Status { OK, NO_SOURCE, NO_PERMISSION, NOTHING, FAILED }

    static final class Result {
        final Status status;
        final List<TvEntry> entries;
        final int channels;
        final int watchNext;

        Result(Status status, List<TvEntry> entries, int channels, int watchNext) {
            this.status = status;
            this.entries = entries;
            this.channels = channels;
            this.watchNext = watchNext;
        }
    }

    interface Callback {
        void onResult(Result result);
    }

    private static final int MAX_CHANNELS = 6;
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

    static void load(final Context context, final String sourcePackage, final String continueLabel, final Callback callback) {
        final Context app = context.getApplicationContext();
        POOL.execute(() -> {
            final Result result = query(app, sourcePackage, continueLabel);
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

    private static Result query(Context context, String sourcePackage, String continueLabel) {
        if (sourcePackage == null) return new Result(Status.NO_SOURCE, new ArrayList<TvEntry>(), 0, 0);
        if (!permissionGranted(context)) return new Result(Status.NO_PERMISSION, new ArrayList<TvEntry>(), 0, 0);
        ContentResolver resolver = context.getContentResolver();
        List<TvEntry> entries = new ArrayList<>();
        int channels = 0;
        int watchNext = 0;
        try {
            watchNext = readWatchNext(resolver, sourcePackage, continueLabel, entries);
            for (Channel channel : previewChannels(resolver, sourcePackage)) {
                channels++;
                readChannel(resolver, channel, entries);
            }
        } catch (RuntimeException e) {
            // SecurityException included: the permission is declared but this TV refuses a third-party home screen.
            return new Result(Status.FAILED, new ArrayList<TvEntry>(), channels, watchNext);
        }
        if (entries.isEmpty()) return new Result(Status.NOTHING, entries, channels, watchNext);
        return new Result(Status.OK, entries, channels, watchNext);
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

    /** Columns read from every program row. The last one differs by table (see projection). */
    private static final String[] PROGRAM_COLUMNS = {
            TvContract.PreviewPrograms.COLUMN_TITLE,
            TvContract.PreviewPrograms.COLUMN_SHORT_DESCRIPTION,
            TvContract.PreviewPrograms.COLUMN_EPISODE_TITLE,
            TvContract.PreviewPrograms.COLUMN_POSTER_ART_URI,
            TvContract.PreviewPrograms.COLUMN_THUMBNAIL_URI,
            TvContract.PreviewPrograms.COLUMN_INTENT_URI,
            TvContract.PreviewPrograms.COLUMN_TYPE,
            TvContract.PreviewPrograms.COLUMN_DURATION_MILLIS,
            TvContract.PreviewPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
            TvContract.PreviewPrograms.COLUMN_POSTER_ART_ASPECT_RATIO,
            TvContract.PreviewPrograms.COLUMN_PACKAGE_NAME,
    };
    private static final int SORT = 11;

    private static String[] projection(String sortColumn) {
        String[] columns = java.util.Arrays.copyOf(PROGRAM_COLUMNS, PROGRAM_COLUMNS.length + 1);
        columns[SORT] = sortColumn;
        return columns;
    }

    private static int readWatchNext(ContentResolver resolver, String sourcePackage, String label, List<TvEntry> out) {
        List<Row> rows = new ArrayList<>();
        String sortColumn = TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS;
        try (Cursor cursor = resolver.query(TvContract.WatchNextPrograms.CONTENT_URI, projection(sortColumn), null, null, null)) {
            if (cursor == null) return 0;
            while (cursor.moveToNext()) {
                if (!sourcePackage.equals(cursor.getString(10))) continue;
                rows.add(new Row(entryOf(cursor, label, true), cursor.getLong(SORT)));
            }
        }
        return take(rows, out);
    }

    private static void readChannel(ContentResolver resolver, Channel channel, List<TvEntry> out) {
        Uri uri = TvContract.buildPreviewProgramsUriForChannel(channel.id);
        List<Row> rows = new ArrayList<>();
        try (Cursor cursor = resolver.query(uri, projection(TvContract.PreviewPrograms.COLUMN_WEIGHT), null, null, null)) {
            if (cursor == null) return;
            while (cursor.moveToNext()) rows.add(new Row(entryOf(cursor, channel.name, false), cursor.getLong(SORT)));
        }
        take(rows, out);
    }

    private static final class Row {
        final TvEntry entry;
        final long order;

        Row(TvEntry entry, long order) {
            this.entry = entry;
            this.order = order;
        }
    }

    /** Heaviest (previews) or most recent (Watch Next) first, up to a row's worth. Returns how many were kept. */
    private static int take(List<Row> rows, List<TvEntry> out) {
        Collections.sort(rows, new Comparator<Row>() {
            @Override public int compare(Row a, Row b) {
                return Long.compare(b.order, a.order);
            }
        });
        int kept = 0;
        for (Row row : rows) {
            if (kept >= MAX_PER_ROW) break;
            out.add(row.entry);
            kept++;
        }
        return kept;
    }

    /** Builds an entry from a row of PROGRAM_COLUMNS; null-safe on every text column. */
    private static TvEntry entryOf(Cursor c, String section, boolean resume) {
        String title = nonNull(c.getString(0));
        String episode = nonNull(c.getString(2));
        String subtitle = episode.isEmpty() ? nonNull(c.getString(1)) : episode;
        int aspect = c.isNull(9) ? TvContract.PreviewPrograms.ASPECT_RATIO_2_3 : c.getInt(9);
        boolean landscape = aspect == TvContract.PreviewPrograms.ASPECT_RATIO_16_9
                || aspect == TvContract.PreviewPrograms.ASPECT_RATIO_3_2
                || aspect == TvContract.PreviewPrograms.ASPECT_RATIO_4_3;
        String poster = nonNull(c.getString(3));
        String thumb = nonNull(c.getString(4));
        // A landscape card prefers the thumbnail; a poster card prefers the poster.
        String image = landscape ? (thumb.isEmpty() ? poster : thumb) : (poster.isEmpty() ? thumb : poster);
        float progress = ProgramKind.progress(c.getLong(8), c.getLong(7));
        return new TvEntry(section, title, subtitle, image, nonNull(c.getString(5)), progress,
                ProgramKind.of(c.getInt(6)), landscape || resume ? TvEntry.Shape.LANDSCAPE : TvEntry.Shape.POSTER, resume);
    }

    private static String nonNull(String text) {
        return text == null ? "" : text;
    }
}
