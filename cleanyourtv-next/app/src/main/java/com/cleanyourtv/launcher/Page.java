package com.cleanyourtv.launcher;

import android.content.Context;
import android.widget.FrameLayout;

/** One tab's content. Only the shown page is visible, so the others take no focus and no work. */
abstract class Page extends FrameLayout {
    /** What a page may ask of the screen that hosts it. */
    interface Host {
        /** Moves the focus up to the tab bar. */
        void focusTabs();

        void openSettings();

        void openLibrary();

        void openDetail(MediaItem item);
    }

    final Host host;
    final Prefs prefs;

    Page(Context context, Host host, Prefs prefs) {
        super(context);
        this.host = host;
        this.prefs = prefs;
        setClipChildren(false);
        setClipToPadding(false);
    }

    /** The page just became visible: load what it needs. */
    abstract void onShown();

    /** The page is hidden: stop work and free images. */
    abstract void onHidden();

    /** Puts the focus on the page's main control. False when it has nothing to focus. */
    abstract boolean enter();

    /** The look settings changed (accent, effects): rebuild what depends on them. */
    void onLookChanged() { }
}
