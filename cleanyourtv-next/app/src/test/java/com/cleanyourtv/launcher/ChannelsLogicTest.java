package com.cleanyourtv.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/** The pure parts of reading what a streaming app publishes for the Android TV home screen. */
public class ChannelsLogicTest {
    private static TvEntry entry(String title, TvEntry.Kind kind, boolean resume) {
        return new TvEntry(resume ? "Continue" : "Row", title, "", "", "", resume ? 0.4f : -1f, kind, TvEntry.Shape.POSTER, resume);
    }

    @Test public void programTypesMapToTabs() {
        assertEquals(TvEntry.Kind.MOVIE, ProgramKind.of(0));
        assertEquals(TvEntry.Kind.SERIES, ProgramKind.of(1));
        assertEquals(TvEntry.Kind.SERIES, ProgramKind.of(2));
        assertEquals(TvEntry.Kind.SERIES, ProgramKind.of(3));
        assertEquals(TvEntry.Kind.OTHER, ProgramKind.of(4));
        assertEquals(TvEntry.Kind.OTHER, ProgramKind.of(11));
        assertEquals(TvEntry.Kind.OTHER, ProgramKind.of(-5));
    }

    @Test public void typeNumbersMatchTheAndroidConstants() {
        // Compiled against the platform: a change in Android's numbers would break the build here.
        assertEquals(android.media.tv.TvContract.PreviewPrograms.TYPE_MOVIE, ProgramKind.TYPE_MOVIE);
        assertEquals(android.media.tv.TvContract.PreviewPrograms.TYPE_TV_SERIES, ProgramKind.TYPE_TV_SERIES);
        assertEquals(android.media.tv.TvContract.PreviewPrograms.TYPE_TV_SEASON, ProgramKind.TYPE_TV_SEASON);
        assertEquals(android.media.tv.TvContract.PreviewPrograms.TYPE_TV_EPISODE, ProgramKind.TYPE_TV_EPISODE);
    }

    @Test public void progressIsPositionOverDuration() {
        assertEquals(0.25f, ProgramKind.progress(1000, 4000), 1e-6f);
        assertEquals(1f, ProgramKind.progress(9000, 4000), 0f);
        assertEquals(-1f, ProgramKind.progress(0, 4000), 0f);
        assertEquals(-1f, ProgramKind.progress(500, 0), 0f);
        assertEquals(-1f, ProgramKind.progress(-1, -1), 0f);
    }

    @Test public void tabsFilterByKind() {
        List<TvEntry> all = new ArrayList<>(Arrays.asList(
                entry("A film", TvEntry.Kind.MOVIE, false),
                entry("A series", TvEntry.Kind.SERIES, false),
                entry("A clip", TvEntry.Kind.OTHER, false),
                entry("Resumed episode", TvEntry.Kind.SERIES, true)));
        assertEquals(4, EntryFilter.apply(all, EntryFilter.EXPLORE).size());
        List<TvEntry> movies = EntryFilter.apply(all, EntryFilter.MOVIES);
        assertEquals(1, movies.size());
        assertEquals("A film", movies.get(0).title);
        List<TvEntry> series = EntryFilter.apply(all, EntryFilter.SERIES);
        assertEquals(2, series.size());
        assertTrue("a resumed episode shows in the Series tab", series.get(1).resume);
        assertEquals("the original list is not changed", 4, all.size());
    }

    @Test public void aPublishedIntentMayOnlyOpenTheChosenApp() {
        assertTrue(IntentGuard.allowed("com.nuvio.tv", "com.nuvio.tv", null));
        assertTrue(IntentGuard.allowed("com.nuvio.tv", null, "com.nuvio.tv"));
        assertTrue(IntentGuard.allowed("com.nuvio.tv", "com.nuvio.tv", "com.nuvio.tv"));
        assertFalse("another app's component", IntentGuard.allowed("com.nuvio.tv", "com.evil", null));
        assertFalse("another app's package", IntentGuard.allowed("com.nuvio.tv", null, "com.evil"));
        assertFalse("a mix of the two", IntentGuard.allowed("com.nuvio.tv", "com.nuvio.tv", "com.evil"));
        assertFalse("an implicit intent could go anywhere", IntentGuard.allowed("com.nuvio.tv", null, null));
        assertFalse("no source chosen", IntentGuard.allowed(null, "com.nuvio.tv", null));
    }

    @Test public void theSourceIsTheSavedAppElseNuvio() {
        List<AppEntry> apps = Arrays.asList(new AppEntry("Netflix", "com.netflix.ninja"),
                new AppEntry("Nuvio", "com.nuvio.tv"), new AppEntry("Other", "org.other"));
        assertEquals("com.nuvio.tv", SourceApp.resolve("", apps));
        assertEquals("com.nuvio.tv", SourceApp.resolve(null, apps));
        assertEquals("a saved choice wins", "com.netflix.ninja", SourceApp.resolve("com.netflix.ninja", apps));
        assertEquals("a saved app that was uninstalled falls back", "com.nuvio.tv", SourceApp.resolve("gone.app", apps));
        assertNull("nothing looks like Nuvio", SourceApp.resolve("", Arrays.asList(new AppEntry("Netflix", "com.netflix.ninja"))));
        assertEquals("matched by label too", "org.x.app",
                SourceApp.resolve("", Arrays.asList(new AppEntry("NUVIO Player", "org.x.app"))));
    }

    @Test public void theDefaultDockPutsNuvioFirst() {
        java.util.Set<String> installed = new java.util.HashSet<>(Arrays.asList(
                "com.google.android.youtube.tv", "com.netflix.ninja", "dev.nuvio.android"));
        List<String> picked = DockDefaults.pick(installed);
        assertEquals("dev.nuvio.android", picked.get(0));
        assertEquals("no duplicate", picked.size(), new java.util.HashSet<>(picked).size());
    }
}
