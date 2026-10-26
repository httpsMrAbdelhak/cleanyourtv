package com.cleanyourtv.launcher;

import java.util.ArrayList;
import java.util.List;

/** One answer of the weather service, with when it was fetched. */
final class Forecast {
    static final class Day {
        final String date;
        final int code;
        final double max;
        final double min;

        Day(String date, int code, double max, double min) {
            this.date = date;
            this.code = code;
            this.max = max;
            this.min = min;
        }
    }

    final double temperature;
    final int code;
    final boolean day;
    final List<Day> days = new ArrayList<>();
    /** Epoch millis of the download. Filled in by the loader, not by the parser. */
    long fetchedAt;

    Forecast(double temperature, int code, boolean day) {
        this.temperature = temperature;
        this.code = code;
        this.day = day;
    }
}
