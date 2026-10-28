package com.cleanyourtv.launcher;

import android.content.Context;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * A row of cards. The selected card widens to landscape and stays at the left of the row; the others stay tall
 * posters. Pictures are loaded only near the selection (RailWindow) and freed far from it, and the wide picture
 * only for the selected card. Left and Right stop at the ends; Up and Down are handed to the screen.
 */
final class CardRail extends RailScroll {
    interface Listener {
        void onOpen(TvEntry entry);

        /** The text under the selected card's title. */
        String detail(TvEntry entry);

        boolean onUp();

        boolean onDown();

        /** The focus entered this rail (the screen may scroll it into view). */
        void onEntered(CardRail rail);
    }

    private static final int GAP = 14;

    private final LinearLayout strip;
    private final List<CardView> cards = new ArrayList<>();
    private final Prefs prefs;
    private final Listener listener;
    private final int narrow;
    private final int wideWidth;
    private final int height;
    private int focus;

    CardRail(Context context, Prefs prefs, Listener listener) {
        super(context);
        this.prefs = prefs;
        this.listener = listener;
        height = Ui.dp(context, 160);
        narrow = height * 2 / 3;
        wideWidth = height * 16 / 9;
        strip = new LinearLayout(context);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.TOP);
        strip.setClipChildren(false);
        int pad = Ui.dp(context, 8);
        strip.setPadding(0, pad, Ui.dp(context, 600), pad);
        addView(strip);
    }

    /** Dim empty cards while the answer is on its way: the row never flashes white or collapses. */
    void showLoading() {
        clear();
        for (int i = 0; i < 6; i++) {
            View card = new View(getContext());
            card.setBackground(Ui.round(Palette.PLACEHOLDER, Ui.dp(getContext(), 14)));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(i == 0 ? wideWidth : narrow, height);
            params.setMargins(0, 0, Ui.dp(getContext(), GAP), 0);
            strip.addView(card, params);
        }
    }

    /** New cards. A rail that has the focus keeps it on the same place (new posters arriving must not move it). */
    void setEntries(List<TvEntry> items) {
        boolean hadFocus = hasFocus();
        int keep = hadFocus ? focus : 0;
        clear();
        focus = Math.max(0, Math.min(keep, items.size() - 1));
        Context context = getContext();
        for (int i = 0; i < items.size(); i++) {
            final int index = i;
            final TvEntry entry = items.get(i);
            final CardView view = new CardView(context, entry, narrow, wideWidth, height);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(narrow, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, Ui.dp(context, GAP), 0);
            view.setOnClickListener(v -> listener.onOpen(entry));
            view.setOnFocusChangeListener((v, hasFocus) -> {
                view.setSelectedLook(hasFocus, hasFocus ? listener.detail(entry) : "");
                ImageLoader loader = ImageLoader.get(getContext());
                if (!hasFocus) {
                    loader.release(view.wide());
                    return;
                }
                focus = index;
                // The selected card goes to the start of the row: every card before it is narrow.
                smoothScrollTo(Math.max(0, index * (narrow + Ui.dp(getContext(), GAP))), 0);
                refreshImages();
                if (!entry.wideUri.isEmpty()) {
                    loader.load(entry.wideUri, view.wide(), prefs.effects().lightImages() ? 480 : 780,
                            prefs.effects().lightImages(), false);
                    view.wide().postDelayed(view::onWideLoaded, 250);
                }
                listener.onEntered(this);
            });
            view.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_UP: return listener.onUp();
                    case KeyEvent.KEYCODE_DPAD_DOWN: return listener.onDown();
                    case KeyEvent.KEYCODE_DPAD_LEFT: return index == 0;
                    case KeyEvent.KEYCODE_DPAD_RIGHT: return index == cards.size() - 1;
                    default: return false;
                }
            });
            cards.add(view);
            strip.addView(view, params);
        }
        refreshImages();
        if (hadFocus) focusFirst();
    }

    int count() {
        return cards.size();
    }

    boolean focusFirst() {
        return !cards.isEmpty() && cards.get(Math.min(focus, cards.size() - 1)).requestFocus();
    }

    /** Loads the posters near the selection, frees those far from it. */
    void refreshImages() {
        boolean light = prefs.effects().lightImages();
        ImageLoader loader = ImageLoader.get(getContext());
        for (int i = 0; i < cards.size(); i++) {
            CardView view = cards.get(i);
            if (RailWindow.shouldLoad(i, focus)) {
                String uri = view.entry.posterUri;
                if (!uri.isEmpty() && view.poster().getDrawable() == null && view.poster().getTag() == null) {
                    loader.load(uri, view.poster(), light ? 185 : 342, light, true);
                }
            } else if (RailWindow.shouldRelease(i, focus)) {
                loader.release(view.poster());
            }
        }
    }

    /** Frees every picture; the rail is hidden. */
    void releaseImages() {
        ImageLoader loader = ImageLoader.get(getContext());
        for (CardView view : cards) {
            loader.release(view.poster());
            loader.release(view.wide());
        }
    }

    private void clear() {
        releaseImages();
        strip.removeAllViews();
        cards.clear();
        focus = 0;
        scrollTo(0, 0);
    }
}
