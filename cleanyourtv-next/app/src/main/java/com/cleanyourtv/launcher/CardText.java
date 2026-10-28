package com.cleanyourtv.launcher;

/** The small texts of a card, as plain functions (the screen supplies the translated words). Pure Java. */
final class CardText {
    private CardText() { }

    /** "S38E01" from the display numbers the app gave; "E04" with no season; empty when there is no episode number. */
    static String badge(String season, String episode) {
        String s = season == null ? "" : season.trim();
        String e = episode == null ? "" : episode.trim();
        if (e.isEmpty()) return "";
        String ep = e.matches("\\d") ? "0" + e : e;
        return s.isEmpty() ? "E" + ep : "S" + s + "E" + ep;
    }

    /** Minutes left, rounded up (19 min 10 s left is "20 min"); -1 stays -1. */
    static long minutesLeft(long remainingMs) {
        if (remainingMs < 0) return -1;
        return (remainingMs + 59_999) / 60_000;
    }

    static long remaining(long positionMs, long durationMs) {
        if (durationMs <= 0 || positionMs <= 0 || positionMs >= durationMs) return -1;
        return durationMs - positionMs;
    }

    /** The year from "2021-09-15", "2021" or an epoch-millis string; empty when it cannot be read. */
    static String year(String release) {
        if (release == null) return "";
        String r = release.trim();
        if (r.length() >= 4 && r.substring(0, 4).matches("\\d{4}") && (r.length() == 4 || r.charAt(4) == '-')) return r.substring(0, 4);
        return "";
    }

    /** Joins the non-empty parts with " · ". */
    static String join(String... parts) {
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isEmpty()) continue;
            if (out.length() > 0) out.append(" · ");
            out.append(part);
        }
        return out.toString();
    }
}
