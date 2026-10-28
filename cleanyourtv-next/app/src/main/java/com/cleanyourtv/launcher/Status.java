package com.cleanyourtv.launcher;

/**
 * The outcome of the last use of an integration, saved as a short code ("ok:1700000000000", "offline") so
 * that the settings screen can show it in the language of the day. Pure Java.
 */
final class Status {
    private Status() { }

    enum Kind { NEVER, OK, STALE, NO_CITY, OFFLINE, FAILED, NO_SOURCE, NO_PERMISSION, NOTHING, NO_KEY, BAD_KEY }

    static final class Parsed {
        final Kind kind;
        /** Epoch millis for OK and STALE, else 0. */
        final long time;

        Parsed(Kind kind, long time) {
            this.kind = kind;
            this.time = time;
        }
    }

    static Parsed parse(String code) {
        if (code == null || code.isEmpty()) return new Parsed(Kind.NEVER, 0);
        int colon = code.indexOf(':');
        String head = colon < 0 ? code : code.substring(0, colon);
        long time = 0;
        if (colon >= 0) {
            try {
                time = Long.parseLong(code.substring(colon + 1));
            } catch (NumberFormatException e) {
                time = 0;
            }
        }
        switch (head) {
            case "ok": return new Parsed(Kind.OK, time);
            case "stale": return new Parsed(Kind.STALE, time);
            case "no_city": return new Parsed(Kind.NO_CITY, 0);
            case "offline": return new Parsed(Kind.OFFLINE, 0);
            case "failed": return new Parsed(Kind.FAILED, 0);
            case "no_source": return new Parsed(Kind.NO_SOURCE, 0);
            case "no_permission": return new Parsed(Kind.NO_PERMISSION, 0);
            case "nothing": return new Parsed(Kind.NOTHING, 0);
            case "no_key": return new Parsed(Kind.NO_KEY, 0);
            case "bad_key": return new Parsed(Kind.BAD_KEY, 0);
            default: return new Parsed(Kind.NEVER, 0);
        }
    }
}
