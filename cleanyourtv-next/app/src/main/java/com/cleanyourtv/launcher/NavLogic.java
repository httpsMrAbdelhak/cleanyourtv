package com.cleanyourtv.launcher;

/**
 * The three zones of the home screen and how the keys move between them: the header (tabs and buttons) on
 * top, the content panel in the middle, the dock at the bottom. Pure functions, so the rules are tested.
 */
final class NavLogic {
    private NavLogic() { }

    enum Zone { HEADER, CONTENT, DOCK }

    enum BackAction { IGNORE, FOCUS_DOCK }

    /** Where Up leads from a zone: the dock goes to the content, the content to the header, the header nowhere. */
    static Zone up(Zone from) {
        switch (from) {
            case DOCK: return Zone.CONTENT;
            case CONTENT: return Zone.HEADER;
            default: return Zone.HEADER;
        }
    }

    static Zone down(Zone from) {
        switch (from) {
            case HEADER: return Zone.CONTENT;
            case CONTENT: return Zone.DOCK;
            default: return Zone.DOCK;
        }
    }

    /** Back climbs down to the dock; on the dock it does nothing, as on any home screen. */
    static BackAction onBack(Zone zone) {
        return zone == Zone.DOCK ? BackAction.IGNORE : BackAction.FOCUS_DOCK;
    }
}
