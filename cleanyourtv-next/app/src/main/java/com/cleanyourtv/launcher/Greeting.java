package com.cleanyourtv.launcher;

/** The welcome line above the clock: a part of the day, and the name if one was set. */
final class Greeting {
    private Greeting() { }

    enum Period { MORNING, AFTERNOON, EVENING }

    static Period forHour(int hour) {
        if (hour >= 5 && hour < 12) return Period.MORNING;
        if (hour >= 12 && hour < 18) return Period.AFTERNOON;
        return Period.EVENING;
    }

    static String compose(String greeting, String name) {
        String trimmed = name == null ? "" : name.trim();
        return trimmed.isEmpty() ? greeting : greeting + ", " + trimmed;
    }
}
