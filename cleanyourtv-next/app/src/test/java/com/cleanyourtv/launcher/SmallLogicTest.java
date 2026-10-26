package com.cleanyourtv.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

public class SmallLogicTest {
    @Test public void greetingPeriods() {
        assertEquals(Greeting.Period.EVENING, Greeting.forHour(0));
        assertEquals(Greeting.Period.EVENING, Greeting.forHour(4));
        assertEquals(Greeting.Period.MORNING, Greeting.forHour(5));
        assertEquals(Greeting.Period.MORNING, Greeting.forHour(11));
        assertEquals(Greeting.Period.AFTERNOON, Greeting.forHour(12));
        assertEquals(Greeting.Period.AFTERNOON, Greeting.forHour(17));
        assertEquals(Greeting.Period.EVENING, Greeting.forHour(18));
        assertEquals(Greeting.Period.EVENING, Greeting.forHour(23));
    }

    @Test public void greetingWithAndWithoutName() {
        assertEquals("Bonjour", Greeting.compose("Bonjour", null));
        assertEquals("Bonjour", Greeting.compose("Bonjour", "   "));
        assertEquals("Bonjour, Sam", Greeting.compose("Bonjour", "  Sam "));
    }

    @Test public void cycleWraps() {
        assertEquals(1, Cycle.wrap(0, 3, 1));
        assertEquals(0, Cycle.wrap(2, 3, 1));
        assertEquals(2, Cycle.wrap(0, 3, -1));
        assertEquals(0, Cycle.wrap(0, 0, 1));
        assertEquals(0, Cycle.wrap(0, 1, -1));
    }

    @Test public void clockModeFromId() {
        assertEquals(ClockMode.H24, ClockMode.fromId("24"));
        assertEquals(ClockMode.H12, ClockMode.fromId("12"));
        assertEquals(ClockMode.SYSTEM, ClockMode.fromId("system"));
        assertEquals(ClockMode.SYSTEM, ClockMode.fromId("nonsense"));
        assertEquals(ClockMode.SYSTEM, ClockMode.fromId(null));
    }

    @Test public void appsSortIgnoringCaseWithAStableTieBreak() {
        List<AppEntry> apps = new ArrayList<>(Arrays.asList(
                new AppEntry("youtube", "com.y"),
                new AppEntry("Arte", "com.arte"),
                new AppEntry("netflix", "com.n2"),
                new AppEntry("Netflix", "com.n1")));
        AppEntry.sort(apps);
        assertEquals("com.arte", apps.get(0).packageName);
        assertEquals("com.n1", apps.get(1).packageName);
        assertEquals("com.n2", apps.get(2).packageName);
        assertEquals("com.y", apps.get(3).packageName);
    }

    @Test public void defaultDockUsesOnlyInstalledAppsAndLeavesARoom() {
        assertTrue(DockDefaults.pick(new HashSet<String>()).isEmpty());
        HashSet<String> everything = new HashSet<>(Arrays.asList(
                "com.google.android.youtube.tv", "com.netflix.ninja", "com.amazon.amazonvideo.livingroom",
                "com.disney.disneyplus", "com.spotify.tv.android", "org.videolan.vlc", "com.android.vending"));
        List<String> picked = DockDefaults.pick(everything);
        assertEquals(DockModel.MAX - 1, picked.size());
        assertEquals("com.google.android.youtube.tv", picked.get(0));
        for (String packageName : picked) assertTrue(everything.contains(packageName));
    }

    @Test public void accentLookupFallsBackToTheFirst() {
        assertEquals("blue", Accents.byId(null).id);
        assertEquals("blue", Accents.byId("nope").id);
        assertEquals("pink", Accents.byId("pink").id);
    }

    @Test public void accentAndWallpaperIdsAreUnique() {
        HashSet<String> accents = new HashSet<>();
        for (Accents.Option option : Accents.ALL) assertTrue(option.id, accents.add(option.id));
        HashSet<String> wallpapers = new HashSet<>();
        for (WallpaperSource source : Wallpapers.ALL) assertTrue(source.id(), wallpapers.add(source.id()));
    }

    @Test public void wallpaperLookupFallsBackToTheFirst() {
        assertEquals(Wallpapers.ALL[0].id(), Wallpapers.byId("nope").id());
        assertEquals(Wallpapers.ALL[0].id(), Wallpapers.byId(null).id());
        assertEquals("dusk", Wallpapers.byId("dusk").id());
    }

    @Test public void everySceneHasLightsAndABaseThatMatchesItsTheme() {
        int light = 0;
        for (WallpaperSource source : Wallpapers.ALL) {
            if (!(source instanceof SceneWallpaper)) continue;
            SceneSpec spec = ((SceneWallpaper) source).spec();
            assertTrue(source.id(), spec.blobs.length >= 2);
            if (spec.light) {
                light++;
                assertTrue(source.id() + " must be pale", brightness(spec.top) > 200 && brightness(spec.bottom) > 190);
            } else {
                assertTrue(source.id() + " must stay dark", brightness(spec.top) < 40 && brightness(spec.bottom) < 40);
            }
            for (SceneSpec.Blob blob : spec.blobs) {
                assertTrue(source.id(), blob.speedX >= 1 && blob.speedY >= 1);
                assertTrue(source.id(), blob.radius > 0.1f && blob.squash > 0.1f);
            }
        }
        assertTrue("the clean light look must be offered", light >= 2);
        assertTrue("and it is the default", Wallpapers.ALL[0].light());
    }

    private static int brightness(int argb) {
        return (((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF)) / 3;
    }

    @Test public void landscapesAreRegisteredAndStill() {
        int landscapes = 0;
        for (WallpaperSource source : Wallpapers.ALL) {
            if (source instanceof LandscapeWallpaper) {
                landscapes++;
                assertFalse(source.id() + " does not move", source.animatable());
            }
        }
        assertTrue("sunset, mauve dawn, lake and misty mountains", landscapes >= 4);
    }

    @Test public void landscapeThemeMatchesItsSky() {
        for (WallpaperSource source : Wallpapers.ALL) {
            if (!(source instanceof LandscapeWallpaper)) continue;
            LandscapeSpec spec = ((LandscapeWallpaper) source).spec();
            assertEquals(source.id() + " light flag must follow the sky", spec.skyLuma() >= 150f, spec.light);
            assertEquals(spec.sky.length, spec.skyStops.length);
            assertTrue(source.id(), spec.ranges.length >= 2);
            for (int i = 1; i < spec.skyStops.length; i++) assertTrue(spec.skyStops[i] > spec.skyStops[i - 1]);
        }
    }

    @Test public void ridgeIsRepeatableAndInRange() {
        for (int seed = 1; seed < 25; seed++) {
            for (float x = 0f; x <= 1f; x += 0.01f) {
                float h = Ridge.height(x, seed);
                assertTrue(h >= 0f && h <= 1f);
                assertEquals(h, Ridge.height(x, seed), 0f);
            }
        }
        assertTrue("two seeds give two outlines", Ridge.height(0.37f, 3) != Ridge.height(0.37f, 4));
    }
}
