package com.cleanyourtv.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.json.JSONException;
import org.junit.Test;

/**
 * The samples below are written from the documented answer shapes of Open-Meteo. They are not recordings:
 * the build sandbox could not reach the service, so a real answer has still to be checked on a TV.
 */
public class ParsersTest {
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
