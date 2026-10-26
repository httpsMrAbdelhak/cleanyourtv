package com.cleanyourtv.launcher;

/**
 * Which posters of a rail hold a bitmap. A rail can have twenty posters; only those near the focus (and the
 * first few, which stay visible) are loaded, so memory stays flat however far the user scrolls.
 */
final class RailWindow {
    private RailWindow() { }

    static final int ALWAYS = 6;
    private static final int BEHIND = 2;
    private static final int AHEAD = 6;
    private static final int KEEP_BEHIND = 4;
    private static final int KEEP_AHEAD = 8;

    static boolean shouldLoad(int index, int focus) {
        return index < ALWAYS || (index >= focus - BEHIND && index <= focus + AHEAD);
    }

    static boolean shouldRelease(int index, int focus) {
        return index >= ALWAYS && (index < focus - KEEP_BEHIND || index > focus + KEEP_AHEAD);
    }
}
