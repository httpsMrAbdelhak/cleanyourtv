package com.cleanyourtv.launcher;

/** When a cached network answer may be shown without asking the server again. */
final class CachePolicy {
    private CachePolicy() { }

    static final long WEATHER_TTL_MS = 30L * 60 * 1000;
    /** Never refresh a forecast more often than this, whatever the user presses. */
    static final long WEATHER_MIN_GAP_MS = 60L * 1000;

    /** A clock that went backwards makes the age negative: such an entry is not trusted as fresh. */
    static boolean isFresh(long now, long fetchedAt, long ttlMs) {
        long age = now - fetchedAt;
        return fetchedAt > 0 && age >= 0 && age < ttlMs;
    }
}
