package com.cleanyourtv.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

/** TMDB: title cleaning, ids in intents, the parsers on documented answers, and the poster swap. */
public class TmdbTest {
    private static TvEntry card(String title, String badge, TvEntry.Kind kind, String intent, String year) {
        return new TvEntry("Row", title, "", "content://app/still.jpg", "content://app/still_wide.jpg", intent,
                -1f, -1L, badge, year, kind, false);
    }

    @Test public void titlesAreCleanedForTheSearch() {
        assertEquals("Friends", TitleKey.query("Friends - S01E10"));
        assertEquals("Friends", TitleKey.query("Friends S1E10 Celui qui..."));
        assertEquals("Friends", TitleKey.query("Friends (1994)"));
        assertEquals("Friends", TitleKey.query("Friends : Saison 2"));
        assertEquals("Naruto Shippuden", TitleKey.query("Naruto Shippuden · Épisode 211"));
        assertEquals("Là-haut", TitleKey.query("Là-haut"));
        assertEquals("", TitleKey.query(null));
    }

    @Test public void titleKeysIgnoreCaseAccentsAndPunctuation() {
        assertEquals(TitleKey.key("Là-haut"), TitleKey.key("LA HAUT"));
        assertEquals("bobleponge", TitleKey.key("Bob l'éponge"));
        assertNotEquals(TitleKey.key("Friends"), TitleKey.key("Fiends"));
    }

    @Test public void idsAreFoundInPublishedIntents() {
        TmdbIds.Ref a = TmdbIds.from("nuvio://details/tv/tmdb:1668#Intent;end");
        assertEquals("1668", a.tmdbId);
        assertEquals(Boolean.TRUE, a.tv);
        TmdbIds.Ref b = TmdbIds.from("intent://play?tmdb_id=550&type=movie#Intent;scheme=nuvio;end");
        assertEquals("550", b.tmdbId);
        assertEquals(Boolean.FALSE, b.tv);
        TmdbIds.Ref c = TmdbIds.from("nuvio://meta/series/tt0108778:1:10");
        assertNull(c.tmdbId);
        assertEquals("tt0108778", c.imdbId);
        assertEquals(Boolean.TRUE, c.tv);
        TmdbIds.Ref d = TmdbIds.from("intent:#Intent;component=dev.nuvio/.Main;end");
        assertNull(d.tmdbId);
        assertNull(d.imdbId);
        assertNull(d.tv);
        assertNull(TmdbIds.from("").tv);
    }

    @Test public void searchTakesTheMatchingTitleAndNeverAStranger() throws Exception {
        String json = "{\"results\":["
                + "{\"name\":\"Friends: The Reunion\",\"poster_path\":\"/r.jpg\",\"first_air_date\":\"2021-05-27\"},"
                + "{\"name\":\"Friends\",\"original_name\":\"Friends\",\"poster_path\":\"/f.jpg\",\"backdrop_path\":\"/fb.jpg\",\"first_air_date\":\"1994-09-22\"}]}";
        TmdbParser.Art art = TmdbParser.bestMatch(json, "Friends", "");
        assertEquals(TmdbParser.POSTER + "/f.jpg", art.poster);
        assertEquals(TmdbParser.BACKDROP + "/fb.jpg", art.backdrop);

        String other = "{\"results\":[{\"title\":\"Something Else\",\"poster_path\":\"/x.jpg\"}]}";
        assertNull(TmdbParser.bestMatch(other, "Friends", ""));
        String noPoster = "{\"results\":[{\"title\":\"Friends\",\"poster_path\":null}]}";
        assertNull(TmdbParser.bestMatch(noPoster, "Friends", ""));
        String person = "{\"results\":[{\"media_type\":\"person\",\"name\":\"Friends\",\"poster_path\":\"/p.jpg\"}]}";
        assertNull(TmdbParser.bestMatch(person, "Friends", ""));
    }

    @Test public void theYearBreaksATie() throws Exception {
        String json = "{\"results\":["
                + "{\"title\":\"Dune\",\"poster_path\":\"/old.jpg\",\"release_date\":\"1984-12-14\"},"
                + "{\"title\":\"Dune\",\"poster_path\":\"/new.jpg\",\"release_date\":\"2021-09-15\"}]}";
        assertEquals(TmdbParser.POSTER + "/new.jpg", TmdbParser.bestMatch(json, "Dune", "2021").poster);
        assertEquals(TmdbParser.POSTER + "/old.jpg", TmdbParser.bestMatch(json, "Dune", "").poster);
    }

    @Test public void findAndDetailsGiveThePoster() throws Exception {
        String find = "{\"movie_results\":[],\"tv_results\":[{\"name\":\"Friends\",\"poster_path\":\"/f.jpg\"}]}";
        assertEquals(TmdbParser.POSTER + "/f.jpg", TmdbParser.find(find, true).poster);
        assertEquals(TmdbParser.POSTER + "/f.jpg", TmdbParser.find(find, false).poster);
        assertNull(TmdbParser.find("{\"movie_results\":[],\"tv_results\":[]}", true));
        TmdbParser.Art details = TmdbParser.details("{\"poster_path\":\"/p.jpg\",\"backdrop_path\":\"/b.jpg\"}");
        assertEquals(TmdbParser.BACKDROP + "/b.jpg", details.backdrop);
    }

    @Test public void trendingKeepsTitlesWithAPoster() throws Exception {
        String json = "{\"results\":["
                + "{\"title\":\"Deadpool & Wolverine\",\"poster_path\":\"/d.jpg\",\"backdrop_path\":\"/db.jpg\",\"release_date\":\"2024-07-24\",\"vote_average\":7.7},"
                + "{\"title\":\"No picture\",\"poster_path\":null},"
                + "{\"title\":\"\",\"poster_path\":\"/e.jpg\"}]}";
        List<TmdbParser.Title> movies = TmdbParser.trending(json, false);
        assertEquals(1, movies.size());
        assertEquals("Deadpool & Wolverine", movies.get(0).title);
        assertEquals("2024", movies.get(0).year);
        assertFalse(movies.get(0).tv);
        assertEquals("★ 7.7", TmdbParser.rating(movies.get(0).vote, Locale.ROOT));
        assertEquals("★ 7,7", TmdbParser.rating(7.7, Locale.FRANCE));
        assertEquals("", TmdbParser.rating(0, Locale.ROOT));
        List<TmdbParser.Title> series = TmdbParser.trending("{\"results\":[{\"name\":\"Paradise\",\"poster_path\":\"/p.jpg\",\"first_air_date\":\"2025-01-26\"}]}", true);
        assertEquals("Paradise", series.get(0).title);
        assertTrue(series.get(0).tv);
    }

    @Test public void episodesOfOneSeriesShareOneLookup() {
        TvEntry e10 = card("Friends", "S01E10", TvEntry.Kind.SERIES, "", "");
        TvEntry e11 = card("Friends - S01E11", "S01E11", TvEntry.Kind.OTHER, "", "");
        assertEquals(PosterSwap.lookupKey(e10), PosterSwap.lookupKey(e11));
        TvEntry movie = card("Dune", "", TvEntry.Kind.MOVIE, "", "2021");
        assertTrue(PosterSwap.lookupKey(movie).startsWith("q_movie_"));
        TvEntry unknown = card("Clip", "", TvEntry.Kind.OTHER, "", "");
        assertTrue(PosterSwap.lookupKey(unknown).startsWith("q_any_"));
        assertEquals("id_tv1668", PosterSwap.lookupKey(card("Friends", "", TvEntry.Kind.OTHER, "nuvio://tv/tmdb:1668", "")));
    }

    @Test public void theSwapPutsTheSeriesPosterInPlaceOfTheEpisodeStill() {
        TvEntry episode = card("Friends", "S01E10", TvEntry.Kind.SERIES, "", "");
        TvEntry unknown = card("Home video", "", TvEntry.Kind.OTHER, "", "");
        Map<String, List<TvEntry>> sections = new LinkedHashMap<>();
        sections.put("Row", Arrays.asList(episode, unknown));
        TvChannels.Result result = new TvChannels.Result(TvChannels.Status.OK, Arrays.asList(episode, unknown),
                new ArrayList<TvEntry>(), sections);

        assertSame("nothing found: the same result, nothing redrawn", result, PosterSwap.apply(result, new HashMap<String, TmdbParser.Art>()));

        Map<String, TmdbParser.Art> found = new HashMap<>();
        found.put(PosterSwap.lookupKey(episode), new TmdbParser.Art("https://image.tmdb.org/t/p/w342/f.jpg", ""));
        TvChannels.Result swapped = PosterSwap.apply(result, found);
        TvEntry card = swapped.home.get(0);
        assertEquals("https://image.tmdb.org/t/p/w342/f.jpg", card.posterUri);
        assertEquals("without a backdrop the app's wide picture stays", "content://app/still_wide.jpg", card.wideUri);
        assertEquals("S01E10", card.badge);
        assertEquals("content://app/still.jpg", swapped.home.get(1).posterUri);
        assertEquals("https://image.tmdb.org/t/p/w342/f.jpg", swapped.sections.get("Row").get(0).posterUri);
    }

    @Test public void theNewStatusesAreRead() {
        assertEquals(Status.Kind.NO_KEY, Status.parse("no_key").kind);
        assertEquals(Status.Kind.BAD_KEY, Status.parse("bad_key").kind);
    }
}
