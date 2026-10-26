package com.cleanyourtv.launcher;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.junit.Test;

/** Which content the panel shows for which app, and the pure parts that feed it. */
public class ContentRoutingTest {
    @Test public void knownStreamingAppsHaveProviderNames() {
        assertNotNull(ProviderMap.namesFor("com.netflix.ninja"));
        assertNotNull("any Netflix build", ProviderMap.namesFor("com.netflix.mediaclient"));
        assertNotNull(ProviderMap.namesFor("com.amazon.amazonvideo.livingroom"));
        assertNotNull(ProviderMap.namesFor("com.disney.disneyplus"));
        assertNull("a file manager is not a service", ProviderMap.namesFor("com.android.documentsui"));
        assertNull(ProviderMap.namesFor(null));
        assertNull("YouTube has its own panel", ProviderMap.namesFor("com.google.android.youtube.tv"));
    }

    @Test public void youtubeIsRecognisedByPackage() {
        assertTrue(ProviderMap.isYouTube("com.google.android.youtube.tv"));
        assertTrue(ProviderMap.isYouTube("com.liskovsoft.smarttubetv"));
        assertFalse(ProviderMap.isYouTube("com.netflix.ninja"));
        assertFalse(ProviderMap.isYouTube(null));
    }

    @Test public void providerNamesMatchWithoutCaseOrSpaces() {
        String[] disney = ProviderMap.namesFor("com.disney.disneyplus");
        assertTrue(ProviderMap.matches("Disney Plus", disney));
        assertTrue(ProviderMap.matches("Disney+", disney));
        assertFalse(ProviderMap.matches("Disney Channel", disney));
        String[] prime = ProviderMap.namesFor("com.amazon.amazonvideo.livingroom");
        assertTrue(ProviderMap.matches("Amazon Prime Video", prime));
        assertFalse("another service", ProviderMap.matches("Netflix", prime));
        String[] canal = ProviderMap.namesFor("com.canal.android.canal");
        assertTrue(ProviderMap.matches("Canal+", canal));
        assertFalse(ProviderMap.matches("Canal+ Series", canal));
    }

    @Test public void providerListFindsTheRightId() throws JSONException {
        String json = "{\"results\":[{\"provider_id\":337,\"provider_name\":\"Disney Plus\"},"
                + "{\"provider_id\":8,\"provider_name\":\"Netflix\"},{\"provider_id\":119,\"provider_name\":\"Amazon Prime Video\"}]}";
        assertEquals(8, ProviderParser.find(json, ProviderMap.namesFor("com.netflix.ninja")));
        assertEquals(337, ProviderParser.find(json, ProviderMap.namesFor("com.disney.disneyplus")));
        assertEquals("no match gives 0, never a guess", 0, ProviderParser.find(json, ProviderMap.namesFor("com.canal.android.canal")));
        assertEquals(0, ProviderParser.find("{}", ProviderMap.namesFor("com.netflix.ninja")));
    }

    @Test public void regionComesFromTheLocale() {
        assertEquals("FR", Region.of(new Locale("fr", "FR")));
        assertEquals("CA", Region.of(new Locale("fr", "CA")));
        assertEquals("FR", Region.of(new Locale("fr")));
        assertEquals("US", Region.of(new Locale("en")));
        assertEquals("GB", Region.of(new Locale("en", "GB")));
    }

    @Test public void feedSpecsBuildTheDocumentedPaths() {
        assertEquals("trending/all/week", FeedSpec.trending().path);
        assertEquals("movie/popular", FeedSpec.popular(false).path);
        assertEquals("tv/popular", FeedSpec.popular(true).path);
        assertEquals("discover/movie", FeedSpec.onService("com.netflix.ninja", false).path);
        assertEquals("discover/tv", FeedSpec.onService("com.netflix.ninja", true).path);
        assertTrue(FeedSpec.onService("com.netflix.ninja", true).series);
        assertEquals("com.netflix.ninja", FeedSpec.onService("com.netflix.ninja", false).providerPackage);
        assertNull(FeedSpec.trending().providerPackage);
    }

    @Test public void cacheKeysNeverCollide() {
        String a = FeedSpec.onService("com.netflix.ninja", false).cacheKey("fr-FR", "FR");
        String b = FeedSpec.onService("com.netflix.ninja", true).cacheKey("fr-FR", "FR");
        String c = FeedSpec.onService("com.disney.disneyplus", false).cacheKey("fr-FR", "FR");
        String d = FeedSpec.onService("com.netflix.ninja", false).cacheKey("en-US", "US");
        assertEquals(4, new java.util.HashSet<>(java.util.Arrays.asList(a, b, c, d)).size());
        assertFalse(a.contains("/"));
    }

    @Test public void cardMetaListsKindYearAndRating() {
        MediaItem item = new MediaItem(1, "T", "", "/p.jpg", "", 7.84, "2021", false);
        assertEquals("Film  •  2021  •  ★ 7.8", ContentCard.meta("Film", item));
        MediaItem bare = new MediaItem(2, "T", "", "/p.jpg", "", 0, "", true);
        assertEquals("Série", ContentCard.meta("Série", bare));
    }

    private static final String VIDEOS = "{\"items\":["
            + "{\"id\":\"abc123\",\"snippet\":{\"title\":\"A video\",\"channelTitle\":\"A channel\","
            + "\"thumbnails\":{\"default\":{\"url\":\"https://i.ytimg.com/vi/abc123/default.jpg\"},"
            + "\"high\":{\"url\":\"https://i.ytimg.com/vi/abc123/hqdefault.jpg\"}}}},"
            + "{\"id\":\"nothumb\",\"snippet\":{\"title\":\"No picture\",\"thumbnails\":{}}},"
            + "{\"id\":\"\",\"snippet\":{\"title\":\"No id\",\"thumbnails\":{\"high\":{\"url\":\"https://x/y.jpg\"}}}},"
            + "{\"id\":\"http\",\"snippet\":{\"title\":\"Plain http\",\"thumbnails\":{\"high\":{\"url\":\"http://x/y.jpg\"}}}}]}";

    @Test public void youtubeKeepsVideosWithAnIdATitleAndAnHttpsThumbnail() throws JSONException {
        List<YouTubeVideo> videos = YouTubeParser.parse(VIDEOS);
        assertEquals(1, videos.size());
        YouTubeVideo video = videos.get(0);
        assertEquals("abc123", video.id);
        assertEquals("A channel", video.channel);
        assertEquals("the biggest available thumbnail is used", "https://i.ytimg.com/vi/abc123/hqdefault.jpg", video.thumbnail);
    }

    @Test public void youtubeEmptyAndGarbage() throws JSONException {
        assertTrue(YouTubeParser.parse("{\"error\":{\"code\":403}}").isEmpty());
        try {
            YouTubeParser.parse("nope");
            fail("expected a JSONException");
        } catch (JSONException expected) {
            // fine
        }
    }

    @Test public void providerNamesAreWhatTmdbListsForTheSupportedServices() {
        // Guards against an accidental edit of the table: every service has at least one name to match.
        for (String pkg : new String[] {"com.netflix.ninja", "com.amazon.amazonvideo.livingroom", "com.disney.disneyplus",
                "com.hbo.hbonow", "com.apple.atve.androidtv.appletv", "com.cbs.app", "com.canal.android.canal",
                "com.crunchyroll.crunchyroid"}) {
            String[] names = ProviderMap.namesFor(pkg);
            assertNotNull(pkg, names);
            assertTrue(pkg, names.length >= 1);
            for (String name : names) assertEquals(name, ProviderMap.normalize(name));
        }
        assertArrayEquals(new String[] {"netflix"}, ProviderMap.namesFor("com.netflix.ninja"));
    }
}
