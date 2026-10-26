package com.cleanyourtv.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class AlgorithmsTest {
    private static int[] fill(int count, int argb) {
        int[] pixels = new int[count];
        Arrays.fill(pixels, argb);
        return pixels;
    }

    // --- DominantColor ---

    @Test public void vividIconGivesItsHue() {
        int[] red = fill(576, 0xFFE02020);
        int picked = DominantColor.pick(red, 0);
        assertTrue("red channel dominates", ((picked >> 16) & 0xFF) > 200);
        assertTrue(((picked >> 8) & 0xFF) < 90 && (picked & 0xFF) < 90);
        assertEquals("opaque", 0xFF, picked >>> 24);
    }

    @Test public void greyOrBlackOrWhiteIconFallsBack() {
        assertEquals(0x1234, DominantColor.pick(fill(576, 0xFF808080), 0x1234));
        assertEquals(0x1234, DominantColor.pick(fill(576, 0xFF000000), 0x1234));
        assertEquals(0x1234, DominantColor.pick(fill(576, 0xFFFFFFFF), 0x1234));
    }

    @Test public void transparentPixelsAreIgnored() {
        int[] pixels = fill(576, 0x00FF0000);
        assertEquals(7, DominantColor.pick(pixels, 7));
    }

    @Test public void theMostVividHueWinsOverASmallAccent() {
        int[] pixels = new int[576];
        Arrays.fill(pixels, 0xFFFFFFFF);
        for (int i = 0; i < 300; i++) pixels[i] = 0xFF2060F0;
        for (int i = 300; i < 330; i++) pixels[i] = 0xFFF02020;
        int picked = DominantColor.pick(pixels, 0);
        assertTrue("blue wins", (picked & 0xFF) > ((picked >> 16) & 0xFF));
    }

    @Test public void aDullColourIsLiftedToGlowBrightness() {
        int picked = DominantColor.pick(fill(576, 0xFF5A2A2A), 0);
        int max = Math.max((picked >> 16) & 0xFF, Math.max((picked >> 8) & 0xFF, picked & 0xFF));
        assertTrue(max >= 200);
    }

    @Test public void emptyInputFallsBack() {
        assertEquals(9, DominantColor.pick(new int[0], 9));
        assertEquals(9, DominantColor.pick(null, 9));
    }

    // --- BoxBlur ---

    @Test public void blurKeepsAUniformImageUniform() {
        int[] pixels = fill(40 * 30, 0xFF336699);
        BoxBlur.blur(pixels, 40, 30, 4);
        for (int pixel : pixels) assertEquals(0xFF336699, pixel);
    }

    @Test public void blurSpreadsAPoint() {
        int w = 21;
        int h = 21;
        int[] pixels = fill(w * h, 0xFF000000);
        pixels[10 * w + 10] = 0xFFFFFFFF;
        BoxBlur.blur(pixels, w, h, 2);
        int center = pixels[10 * w + 10] & 0xFF;
        int near = pixels[10 * w + 11] & 0xFF;
        assertTrue("the point is dimmer", center < 255);
        assertTrue("its neighbour is brighter than black", near > 0);
        assertEquals("far pixels stay black", 0, pixels[0] & 0xFF);
    }

    @Test public void blurWithRadiusZeroChangesNothing() {
        int[] pixels = {0xFF000000, 0xFFFFFFFF, 0xFF000000, 0xFFFFFFFF};
        int[] copy = pixels.clone();
        BoxBlur.blur(pixels, 2, 2, 0);
        assertTrue(Arrays.equals(copy, pixels));
    }

    // --- NavLogic ---

    @Test public void upClimbsFromDockToContentToHeader() {
        assertEquals(NavLogic.Zone.CONTENT, NavLogic.up(NavLogic.Zone.DOCK));
        assertEquals(NavLogic.Zone.HEADER, NavLogic.up(NavLogic.Zone.CONTENT));
        assertEquals(NavLogic.Zone.HEADER, NavLogic.up(NavLogic.Zone.HEADER));
    }

    @Test public void downDescendsAndStopsAtTheDock() {
        assertEquals(NavLogic.Zone.CONTENT, NavLogic.down(NavLogic.Zone.HEADER));
        assertEquals(NavLogic.Zone.DOCK, NavLogic.down(NavLogic.Zone.CONTENT));
        assertEquals("no app grid below the dock", NavLogic.Zone.DOCK, NavLogic.down(NavLogic.Zone.DOCK));
    }

    @Test public void backGoesDownToTheDockThenStops() {
        assertEquals(NavLogic.BackAction.FOCUS_DOCK, NavLogic.onBack(NavLogic.Zone.HEADER));
        assertEquals(NavLogic.BackAction.FOCUS_DOCK, NavLogic.onBack(NavLogic.Zone.CONTENT));
        assertEquals(NavLogic.BackAction.IGNORE, NavLogic.onBack(NavLogic.Zone.DOCK));
    }

    // --- RailWindow ---

    @Test public void railKeepsTheFirstPostersAndThoseAroundTheFocus() {
        assertTrue(RailWindow.shouldLoad(0, 15));
        assertTrue(RailWindow.shouldLoad(5, 15));
        assertFalse(RailWindow.shouldLoad(8, 15));
        assertTrue(RailWindow.shouldLoad(13, 15));
        assertTrue(RailWindow.shouldLoad(21, 15));
        assertFalse(RailWindow.shouldLoad(23, 15));
    }

    @Test public void railFreesOnlyFarPostersAndNeverTheFirstOnes() {
        assertFalse(RailWindow.shouldRelease(0, 15));
        assertFalse(RailWindow.shouldRelease(5, 15));
        assertTrue(RailWindow.shouldRelease(8, 15));
        assertFalse(RailWindow.shouldRelease(12, 15));
        assertTrue(RailWindow.shouldRelease(24, 15));
    }

    @Test public void aPosterIsNeverBothLoadedAndFreed() {
        for (int focus = 0; focus < 30; focus++) {
            for (int i = 0; i < 40; i++) {
                assertFalse(RailWindow.shouldLoad(i, focus) && RailWindow.shouldRelease(i, focus));
            }
        }
    }

    // --- CachePolicy ---

    @Test public void cacheFreshnessFollowsTheTtl() {
        long now = 1_000_000_000L;
        assertTrue(CachePolicy.isFresh(now, now - 1000, CachePolicy.WEATHER_TTL_MS));
        assertFalse(CachePolicy.isFresh(now, now - CachePolicy.WEATHER_TTL_MS - 1, CachePolicy.WEATHER_TTL_MS));
        assertFalse("never fetched", CachePolicy.isFresh(now, 0, CachePolicy.TMDB_TTL_MS));
        assertFalse("clock went backwards", CachePolicy.isFresh(now, now + 5000, CachePolicy.TMDB_TTL_MS));
    }

    // --- SceneMath ---

    @Test public void sceneLoopIsSeamless() {
        for (int speed = 1; speed <= 2; speed++) {
            assertEquals(SceneMath.wave(0f, speed, 0.3f), SceneMath.wave(1f, speed, 0.3f), 1e-4f);
        }
        assertEquals(0f, SceneMath.phaseAt(0), 0f);
        assertEquals(0f, SceneMath.phaseAt(SceneMath.LOOP_MS), 0f);
        assertTrue(SceneMath.phaseAt(SceneMath.LOOP_MS / 2) > 0.49f && SceneMath.phaseAt(SceneMath.LOOP_MS / 2) < 0.51f);
    }

    @Test public void waveStaysInRange() {
        for (float p = 0; p < 1f; p += 0.05f) {
            float v = SceneMath.wave(p, 2, 0.7f);
            assertTrue(v >= -1.0001f && v <= 1.0001f);
        }
    }

    // --- Effects, WallpaperStore, Status ---

    @Test public void effectsLevels() {
        assertEquals(Effects.QUALITY, Effects.fromId(null));
        assertEquals(Effects.PERFORMANCE, Effects.fromId("performance"));
        assertTrue(Effects.QUALITY.frostedGlass() && Effects.QUALITY.allowsAnimatedWallpaper());
        assertFalse(Effects.PERFORMANCE.frostedGlass() || Effects.PERFORMANCE.allowsAnimatedWallpaper());
        assertTrue(Effects.PERFORMANCE.lightImages());
    }

    @Test public void wallpaperSampleSizeKeepsEnoughPixels() {
        assertEquals(1, WallpaperStore.sampleSize(1920, 1080, 1920, 1080));
        assertEquals(2, WallpaperStore.sampleSize(4000, 2250, 1920, 1080));
        assertEquals(4, WallpaperStore.sampleSize(8000, 4500, 1920, 1080));
        assertEquals(1, WallpaperStore.sampleSize(100, 100, 1920, 1080));
    }

    @Test public void statusCodes() {
        assertEquals(Status.Kind.NEVER, Status.parse("").kind);
        assertEquals(Status.Kind.NEVER, Status.parse(null).kind);
        assertEquals(Status.Kind.OK, Status.parse("ok:1700000000000").kind);
        assertEquals(1700000000000L, Status.parse("ok:1700000000000").time);
        assertEquals(Status.Kind.STALE, Status.parse("stale:5").kind);
        assertEquals(Status.Kind.OFFLINE, Status.parse("offline").kind);
        assertEquals(Status.Kind.NO_KEY, Status.parse("no_key").kind);
        assertEquals(Status.Kind.BAD_KEY, Status.parse("bad_key").kind);
        assertEquals(Status.Kind.NEVER, Status.parse("garbage").kind);
        assertEquals(0, Status.parse("ok:notanumber").time);
    }

    // --- Luma ---

    @Test public void lumaTellsPaleFromDark() {
        assertTrue(Luma.isLight(fill(100, 0xFFF0F0F0)));
        assertFalse(Luma.isLight(fill(100, 0xFF101010)));
        assertFalse("mid grey keeps light text", Luma.isLight(fill(100, 0xFF808080)));
        assertEquals(0f, Luma.mean(fill(10, 0x00FFFFFF)), 0f);
        assertEquals(255f, Luma.mean(fill(10, 0xFFFFFFFF)), 0.5f);
    }
}
