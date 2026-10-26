package com.cleanyourtv.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.json.JSONException;
import org.junit.Test;

/**
 * The samples below are written from the documented answer shapes of TMDB and Open-Meteo. They are not
 * recordings: the build sandbox could not reach either service, so a real answer has still to be checked on a TV.
 */
public class ParsersTest {
    private static final String TRENDING = "{\"page\":1,\"results\":["
            + "{\"id\":1,\"media_type\":\"movie\",\"title\":\"Dune\",\"overview\":\"Sand.\",\"poster_path\":\"/p1.jpg\","
            + "\"backdrop_path\":\"/b1.jpg\",\"vote_average\":8.1,\"release_date\":\"2021-09-15\"},"
            + "{\"id\":2,\"media_type\":\"tv\",\"name\":\"Severance\",\"overview\":\"Work.\",\"poster_path\":\"/p2.jpg\","
            + "\"backdrop_path\":null,\"vote_average\":8.7,\"first_air_date\":\"2022-02-18\"},"
            + "{\"id\":3,\"media_type\":\"person\",\"name\":\"Someone\",\"poster_path\":\"/p3.jpg\"},"
            + "{\"id\":4,\"media_type\":\"movie\",\"title\":\"No poster\",\"poster_path\":null},"
            + "{\"id\":5,\"media_type\":\"movie\",\"title\":\"\",\"poster_path\":\"/p5.jpg\"}"
            + "]}";

    @Test public void tmdbKeepsFilmsAndSeriesWithPostersOnly() throws JSONException {
        List<MediaItem> items = TmdbParser.parse(TRENDING, false);
        assertEquals(2, items.size());
        MediaItem movie = items.get(0);
        assertEquals("Dune", movie.title);
        assertEquals("2021", movie.year);
        assertEquals(8.1, movie.rating, 0.001);
        assertFalse(movie.series);
        assertEquals("/b1.jpg", movie.backdropPath);
        MediaItem series = items.get(1);
        assertEquals("Severance", series.title);
        assertTrue(series.series);
        assertEquals("2022", series.year);
        assertEquals("a null backdrop becomes empty", "", series.backdropPath);
    }

    @Test public void tmdbUsesTheFeedTypeWhenMediaTypeIsAbsent() throws JSONException {
        String movies = "{\"results\":[{\"id\":9,\"title\":\"M\",\"poster_path\":\"/m.jpg\",\"release_date\":\"\"}]}";
        String tv = "{\"results\":[{\"id\":9,\"name\":\"S\",\"poster_path\":\"/s.jpg\",\"first_air_date\":\"2020-01-01\"}]}";
        assertFalse(TmdbParser.parse(movies, false).get(0).series);
        assertEquals("", TmdbParser.parse(movies, false).get(0).year);
        assertTrue(TmdbParser.parse(tv, true).get(0).series);
        assertEquals("S", TmdbParser.parse(tv, true).get(0).title);
    }

    @Test public void tmdbEmptyOrMissingResultsGiveNoItems() throws JSONException {
        assertTrue(TmdbParser.parse("{\"results\":[]}", false).isEmpty());
        assertTrue(TmdbParser.parse("{\"status_message\":\"Invalid API key\"}", false).isEmpty());
    }

    @Test public void tmdbRejectsGarbage() {
        try {
            TmdbParser.parse("<html>not json</html>", false);
            fail("expected a JSONException");
        } catch (JSONException expected) {
            // fine
        }
    }

    private static final String FORECAST = "{\"latitude\":48.86,\"longitude\":2.35,"
            + "\"current\":{\"time\":\"2026-10-26T14:00\",\"interval\":900,\"temperature_2m\":14.3,\"weather_code\":3,\"is_day\":1},"
            + "\"daily\":{\"time\":[\"2026-10-26\",\"2026-10-27\",\"2026-10-28\"],"
            + "\"weather_code\":[3,61,0],\"temperature_2m_max\":[15.2,13.0,17.9],\"temperature_2m_min\":[8.1,9.4,null]}}";

    @Test public void forecastReadsCurrentAndDays() throws JSONException {
        Forecast forecast = ForecastParser.parse(FORECAST);
        assertEquals(14.3, forecast.temperature, 0.001);
        assertEquals(3, forecast.code);
        assertTrue(forecast.day);
        assertEquals("a day with a missing value is dropped, not invented", 2, forecast.days.size());
        assertEquals("2026-10-26", forecast.days.get(0).date);
        assertEquals(15.2, forecast.days.get(0).max, 0.001);
        assertEquals(9.4, forecast.days.get(1).min, 0.001);
    }

    @Test public void forecastWithoutCurrentOrDaysIsAnError() {
        for (String bad : new String[] {"{}", "{\"current\":{\"temperature_2m\":1,\"weather_code\":0}}",
                "{\"current\":{\"temperature_2m\":1,\"weather_code\":0},\"daily\":{\"time\":[],\"weather_code\":[],"
                        + "\"temperature_2m_max\":[],\"temperature_2m_min\":[]}}"}) {
            try {
                ForecastParser.parse(bad);
                fail("expected a JSONException for " + bad);
            } catch (JSONException expected) {
                // fine
            }
        }
    }

    @Test public void geocodingBuildsReadableLabels() throws JSONException {
        String json = "{\"results\":[{\"name\":\"Lyon\",\"latitude\":45.75,\"longitude\":4.85,\"admin1\":\"Auvergne-Rhône-Alpes\",\"country\":\"France\"},"
                + "{\"name\":\"Paris\",\"latitude\":48.85,\"longitude\":2.35,\"admin1\":\"Paris\",\"country\":\"France\"},"
                + "{\"name\":\"Broken\"}]}";
        List<GeoParser.Place> places = GeoParser.parse(json);
        assertEquals(2, places.size());
        assertEquals("Lyon, Auvergne-Rhône-Alpes, France", places.get(0).label());
        assertEquals("a region equal to the city is not repeated", "Paris, France", places.get(1).label());
        assertEquals(45.75, places.get(0).latitude, 0.0001);
    }

    @Test public void geocodingNoMatchIsAnEmptyList() throws JSONException {
        assertTrue(GeoParser.parse("{\"generationtime_ms\":0.5}").isEmpty());
    }

    @Test public void wmoCodesFoldIntoLooks() {
        assertEquals(WeatherCode.CLEAR, WeatherCode.of(0));
        assertEquals(WeatherCode.PARTLY, WeatherCode.of(2));
        assertEquals(WeatherCode.CLOUDY, WeatherCode.of(3));
        assertEquals(WeatherCode.FOG, WeatherCode.of(45));
        assertEquals(WeatherCode.DRIZZLE, WeatherCode.of(53));
        assertEquals(WeatherCode.RAIN, WeatherCode.of(63));
        assertEquals(WeatherCode.SNOW, WeatherCode.of(73));
        assertEquals(WeatherCode.SHOWERS, WeatherCode.of(81));
        assertEquals(WeatherCode.SNOW, WeatherCode.of(86));
        assertEquals(WeatherCode.STORM, WeatherCode.of(95));
        assertEquals("an unknown code still shows something", WeatherCode.CLOUDY, WeatherCode.of(1234));
    }
}
