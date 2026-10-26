package com.cleanyourtv.launcher;

import android.content.Context;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * A titled row of posters. Only the posters near the focus hold a picture (see RailWindow), the strip glides
 * to follow the focus, and Up on the first rail goes back to the tab bar.
 */
final class RailBlock extends LinearLayout {
    interface Listener {
        void onFocused(MediaItem item, RailBlock rail);

        void onOpen(MediaItem item);

        /** Up was pressed on this rail: false lets the framework look for the rail above. */
        boolean onUp(RailBlock rail);
    }

    private final TextView title;
    private final RailScroll scroll;
    private final LinearLayout strip;
    private final List<PosterView> posters = new ArrayList<>();
    private final Prefs prefs;
    private final Listener listener;
    private final int posterW;
    private final int posterH;
    private int focus;

    RailBlock(Context context, Prefs prefs, int titleRes, Listener listener) {
        super(context);
        this.prefs = prefs;
        this.listener = listener;
        setOrientation(VERTICAL);
        setClipChildren(false);
        setClipToPadding(false);
        posterW = Ui.dp(context, 100);
        posterH = Ui.dp(context, 150);

        title = Ui.text(context, 22, Palette.TEXT, Ui.medium(context));
        title.setText(titleRes);
        title.setPadding(Ui.dp(context, 4), 0, 0, Ui.dp(context, 2));
        addView(title);

        scroll = new RailScroll(context);
        strip = new LinearLayout(context);
        strip.setOrientation(HORIZONTAL);
        strip.setGravity(Gravity.CENTER_VERTICAL);
        strip.setClipChildren(false);
        int pad = Ui.dp(context, 14);
        strip.setPadding(Ui.dp(context, 4), pad, Ui.dp(context, 160), pad);
        scroll.addView(strip);
        addView(scroll, new LayoutParams(LayoutParams.MATCH_PARENT, posterH + 2 * pad));
    }

    /** Dim empty cards while the answer is on its way: the row never flashes white or collapses. */
    void showLoading() {
        clear();
        for (int i = 0; i < 8; i++) {
            View card = new View(getContext());
            card.setBackground(Ui.round(0x14FFFFFF, Ui.dp(getContext(), 14)));
            LayoutParams params = new LayoutParams(posterW, posterH);
            params.setMargins(0, 0, Ui.dp(getContext(), 14), 0);
            strip.addView(card, params);
        }
    }

    void setItems(List<MediaItem> items) {
        clear();
        boolean light = prefs.effects().lightImages();
        for (int i = 0; i < items.size(); i++) {
            final int index = i;
            MediaItem item = items.get(i);
            PosterView poster = new PosterView(getContext(), item, posterW, posterH);
            LayoutParams params = (LayoutParams) poster.getLayoutParams();
            params.setMargins(0, 0, Ui.dp(getContext(), 14), 0);
            poster.setOnClickListener(view -> listener.onOpen(item));
            poster.setOnFocusChangeListener(new OnFocusChangeListener() {
                @Override public void onFocusChange(View view, boolean hasFocus) {
                    float scale = hasFocus ? 1.1f : 1f;
                    view.animate().scaleX(scale).scaleY(scale).setDuration(160).start();
                    view.setElevation(hasFocus ? Ui.dp(getContext(), 10) : 0);
                    if (!hasFocus) return;
                    focus = index;
                    scroll.reveal(view);
                    refreshImages();
                    listener.onFocused(item, RailBlock.this);
                }
            });
            poster.setOnKeyListener((view, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) return listener.onUp(this);
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) return index == 0;
                return keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && index == posters.size() - 1;
            });
            posters.add(poster);
            strip.addView(poster);
        }
        refreshImages();
        if (light) strip.setLayerType(LAYER_TYPE_NONE, null);
    }

    int count() {
        return posters.size();
    }

    boolean focusFirst() {
        return !posters.isEmpty() && posters.get(Math.min(focus, posters.size() - 1)).requestFocus();
    }

    /** Loads the posters near the focus, frees those far from it. */
    void refreshImages() {
        boolean light = prefs.effects().lightImages();
        ImageLoader loader = ImageLoader.get(getContext());
        for (int i = 0; i < posters.size(); i++) {
            PosterView poster = posters.get(i);
            if (RailWindow.shouldLoad(i, focus)) {
                if (poster.getDrawable() == null && poster.getTag() == null) {
                    loader.load(Tmdb.posterUrl(poster.item.posterPath, light), poster, posterW, light, true);
                }
            } else if (RailWindow.shouldRelease(i, focus)) {
                loader.release(poster);
            }
        }
    }

    /** Frees every picture; the rail is hidden. */
    void releaseImages() {
        ImageLoader loader = ImageLoader.get(getContext());
        for (PosterView poster : posters) loader.release(poster);
    }

    private void clear() {
        strip.removeAllViews();
        posters.clear();
        focus = 0;
    }
}
