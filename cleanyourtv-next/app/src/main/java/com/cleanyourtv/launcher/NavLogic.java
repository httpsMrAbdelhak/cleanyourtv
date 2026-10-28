package com.cleanyourtv.launcher;

/**
 * The zones of the home screen, top to bottom: the top bar, the chips (Home only), the content, the dock.
 * Up and Down move one zone at a time; Back always goes down to the dock, and does nothing on it. Pure functions.
 */
final class NavLogic {
    private NavLogic() { }

    enum Zone { TOP, CHIPS, CONTENT, DOCK }

    enum BackAction { IGNORE, FOCUS_DOCK }

    /** chips: whether the chip row is on screen (the Home page). */
    static Zone up(Zone from, boolean chips) {
        switch (from) {
            case DOCK: return Zone.CONTENT;
            case CONTENT: return chips ? Zone.CHIPS : Zone.TOP;
            default: return Zone.TOP;
        }
    }

    /** Down on the dock is not a zone: it opens the app drawer. */
    static Zone down(Zone from, boolean chips) {
        switch (from) {
            case TOP: return chips ? Zone.CHIPS : Zone.CONTENT;
            case CHIPS: return Zone.CONTENT;
            default: return Zone.DOCK;
        }
    }

    static BackAction onBack(Zone zone) {
        return zone == Zone.DOCK ? BackAction.IGNORE : BackAction.FOCUS_DOCK;
    }
}
