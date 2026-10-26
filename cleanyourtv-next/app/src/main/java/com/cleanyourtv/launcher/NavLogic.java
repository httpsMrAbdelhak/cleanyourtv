package com.cleanyourtv.launcher;

/** The Back key and the tab order of the home screen, as plain functions. */
final class NavLogic {
    private NavLogic() { }

    static final int HOME = 0;
    static final int CINEMA = 1;
    static final int WEATHER = 2;
    static final int TAB_COUNT = 3;

    enum BackAction { IGNORE, FOCUS_TABS, SELECT_HOME }

    /**
     * Back from content goes up to the tab bar; Back on a tab other than Home goes to Home; Back on Home
     * does nothing, as on any home screen.
     */
    static BackAction onBack(int selectedTab, boolean focusInTabs) {
        if (!focusInTabs) return BackAction.FOCUS_TABS;
        if (selectedTab != HOME) return BackAction.SELECT_HOME;
        return BackAction.IGNORE;
    }
}
