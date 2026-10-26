package com.cleanyourtv.launcher;

import android.content.Context;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * The row of cards above the dock: a wide card and posters for films and series, or video thumbnails. It
 * glides to follow the focus, loads pictures only near the focus (RailWindow) and frees the others. Left and
 * Right stop at the ends; Up and Down are handed to the screen, which knows what lies above and below.
 */
final class CardRail extends RailScroll {
    interface Listener {
        void onOpen(ContentCard card);

        /** Up was pressed on a card. Return true when handled. */
        boolean onUp();

        boolean onDown();
    }

    private static final int GAP = 16;

    private final LinearLayout strip;
    private final List<CardView> cards = new ArrayList<>();
    private final Prefs prefs;
    private final Listener listener;
    private int focus;

    CardRail(Context context, Prefs prefs, Listener listener) {
        super(context);
        this.prefs = prefs;
        this.listener = listener;
        strip = new LinearLayout(context);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.TOP);
        strip.setClipChildren(false);
        int pad = Ui.dp(context, 14);
        strip.setPadding(Ui.dp(context, 6), pad, Ui.dp(context, 200), pad);
        addView(strip);
    }

    /** Dim empty cards while the answer is on its way: the row never flashes white or collapses. */
    void showLoading() {
        clear();
        int w = Ui.dp(getContext(), 320);
        int h = Ui.dp(getContext(), 180);
        for (int i = 0; i < 4; i++) {
            View card = new View(getContext());
            card.setBackground(Ui.round(Palette.PLACEHOLDER, Ui.dp(getContext(), 16)));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(i == 0 ? w : w * 4 / 10, h);
            params.setMargins(0, 0, Ui.dp(getContext(), GAP), 0);
            strip.addView(card, params);
        }
    }

    void setCards(List<ContentCard> items) {
        boolean hadFocus = hasFocus();
        clear();
        Context context = getContext();
        for (int i = 0; i < items.size(); i++) {
            final int index = i;
            ContentCard card = items.get(i);
            int w;
            int h;
            switch (card.shape) {
                case WIDE: w = Ui.dp(context, 320); h = Ui.dp(context, 180); break;
                case VIDEO: w = Ui.dp(context, 288); h = Ui.dp(context, 162); break;
                default: w = Ui.dp(context, 120); h = Ui.dp(context, 180); break;
            }
            CardView view = new CardView(context, card, w, h);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    w, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, Ui.dp(context, GAP), 0);
            view.setOnClickListener(v -> listener.onOpen(card));
            final View.OnFocusChangeListener grow = view.getOnFocusChangeListener();
            view.setOnFocusChangeListener((v, hasFocus) -> {
                grow.onFocusChange(v, hasFocus);
                if (!hasFocus) return;
                focus = index;
                reveal(v);
                refreshImages();
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
        // New cards replaced the focused one: keep the focus in the rail rather than lose it.
        if (hadFocus) focusFirst();
    }

    int count() {
        return cards.size();
    }

    boolean focusFirst() {
        return !cards.isEmpty() && cards.get(Math.min(focus, cards.size() - 1)).requestFocus();
    }

    /** Loads the pictures near the focus, frees those far from it. */
    void refreshImages() {
        boolean light = prefs.effects().lightImages();
        ImageLoader loader = ImageLoader.get(getContext());
        for (int i = 0; i < cards.size(); i++) {
            CardView view = cards.get(i);
            if (RailWindow.shouldLoad(i, focus)) {
                if (view.picture().getDrawable() == null && view.picture().getTag() == null) {
                    int width = view.card.shape == ContentCard.Shape.POSTER ? 342 : 780;
                    loader.load(view.card.imageUrl, view.picture(), light ? width / 2 : width, light, true);
                }
            } else if (RailWindow.shouldRelease(i, focus)) {
                loader.release(view.picture());
            }
        }
    }

    /** Frees every picture; the rail is hidden. */
    void releaseImages() {
        ImageLoader loader = ImageLoader.get(getContext());
        for (CardView view : cards) loader.release(view.picture());
    }

    private void clear() {
        releaseImages();
        strip.removeAllViews();
        cards.clear();
        focus = 0;
    }
}
