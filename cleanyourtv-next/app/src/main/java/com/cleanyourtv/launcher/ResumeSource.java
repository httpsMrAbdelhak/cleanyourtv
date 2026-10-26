package com.cleanyourtv.launcher;

import android.content.Context;

import java.util.List;

/**
 * A place "Continue watching" entries could come from. NONE is registered, on purpose: see
 * docs/PLAYBACK.md. Android gives a third-party home screen no way to read what other apps are playing
 * or have paused, so an honest section cannot exist yet. When a source that really can answer appears
 * (an app with a documented, permission-free provider), implement this and add it to ResumeRegistry.
 */
interface ResumeSource {
    final class Entry {
        final String title;
        final String subtitle;
        /** 0..1, or -1 when unknown. */
        final float progress;
        final String posterUrl;
        final String packageName;

        Entry(String title, String subtitle, float progress, String posterUrl, String packageName) {
            this.title = title;
            this.subtitle = subtitle;
            this.progress = progress;
            this.posterUrl = posterUrl;
            this.packageName = packageName;
        }
    }

    String id();

    /** Called off the main thread. Returns only entries the source really knows. */
    List<Entry> load(Context context);
}
