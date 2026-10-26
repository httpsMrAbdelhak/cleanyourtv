package com.cleanyourtv.launcher;

/** How the clock shows the hour. SYSTEM follows the TV's own 12/24 hour setting. */
enum ClockMode {
    SYSTEM("system", null),
    H24("24", "HH:mm"),
    H12("12", "h:mm");

    final String id;
    /** A TextClock pattern, or null to leave the system default. */
    final String pattern;

    ClockMode(String id, String pattern) {
        this.id = id;
        this.pattern = pattern;
    }

    static ClockMode fromId(String id) {
        for (ClockMode mode : values()) {
            if (mode.id.equals(id)) return mode;
        }
        return SYSTEM;
    }
}
