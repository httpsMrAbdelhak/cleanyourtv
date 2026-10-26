package com.cleanyourtv.launcher;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Network answers kept as files in the app's cache folder: the modification time is the download time.
 * Android may clear the folder when space runs low, which only costs one new download.
 */
final class JsonCache {
    private JsonCache() { }

    static final class Entry {
        final String body;
        final long fetchedAt;

        Entry(String body, long fetchedAt) {
            this.body = body;
            this.fetchedAt = fetchedAt;
        }
    }

    private static File file(Context context, String key) {
        File dir = new File(context.getCacheDir(), "json");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        return new File(dir, key.replaceAll("[^A-Za-z0-9_.-]", "_") + ".json");
    }

    /** The saved answer, or null. Old entries are still returned: the caller decides with CachePolicy. */
    static Entry read(Context context, String key) {
        File file = file(context, key);
        if (!file.isFile()) return null;
        try {
            return new Entry(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8), file.lastModified());
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    static void write(Context context, String key, String body) {
        File file = file(context, key);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }
}
