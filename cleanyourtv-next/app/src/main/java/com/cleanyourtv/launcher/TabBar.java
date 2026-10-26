package com.cleanyourtv.launcher;

import android.content.Context;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The tab bar at the top: Home, Cinema, Weather, and the settings button. A glass pill; the focused item
 * lights up, and the item of the page on screen stays bright. Down enters the page, Up does nothing.
 */
final class TabBar extends LinearLayout {
    interface Listener {
        /** Down was pressed on a tab: move into the page. */
        void onEnterContent();

        void onSettings();
    }

    private final TextView[] tabs = new TextView[NavLogic.TAB_COUNT];
    private final ImageView gear;
    private int selected = -1;

    TabBar(Context context, Prefs prefs, final Listener listener) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setClipChildren(false);
        int pad = Ui.dp(context, 6);
        setPadding(pad, pad, pad, pad);
        setBackground(new GlassDrawable(Ui.dp(context, 34), Ui.dp(context, 1.5f), true, false, prefs.effects()));

        int[] labels = {R.string.tab_home, R.string.tab_cinema, R.string.tab_weather};
        for (int i = 0; i < tabs.length; i++) {
            TextView tab = Ui.text(context, 20, Palette.TEXT_DIM, Ui.medium(context));
            tab.setText(labels[i]);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(Ui.dp(context, 26), Ui.dp(context, 11), Ui.dp(context, 26), Ui.dp(context, 11));
            tab.setFocusable(true);
            tab.setClickable(true);
            tab.setDefaultFocusHighlightEnabled(false);
            tab.setBackground(Ui.focusGlass(context, 0x00000000, 0x55FFFFFF, Ui.dp(context, 28)));
            tab.setOnKeyListener((view, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    listener.onEnterContent();
                    return true;
                }
                return keyCode == KeyEvent.KEYCODE_DPAD_UP;
            });
            tab.setOnClickListener(view -> listener.onEnterContent());
            tabs[i] = tab;
            addView(tab);
        }

        gear = new ImageView(context);
        gear.setImageDrawable(Glyphs.settings(Palette.TEXT_DIM));
        gear.setFocusable(true);
        gear.setClickable(true);
        gear.setDefaultFocusHighlightEnabled(false);
        int gearPad = Ui.dp(context, 11);
        gear.setPadding(gearPad, gearPad, gearPad, gearPad);
        gear.setBackground(Ui.focusGlass(context, 0x00000000, 0x55FFFFFF, Ui.dp(context, 28)));
        gear.setOnClickListener(view -> listener.onSettings());
        gear.setOnKeyListener((view, keyCode, event) ->
                event.getAction() == KeyEvent.ACTION_DOWN
                        && (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT));
        addView(gear, new LayoutParams(Ui.dp(context, 50), Ui.dp(context, 46)));
    }

    int indexOf(View view) {
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] == view) return i;
        }
        return -1;
    }

    boolean contains(View view) {
        return view != null && (indexOf(view) >= 0 || view == gear);
    }

    void select(int tab) {
        selected = tab;
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setTextColor(i == tab ? Palette.TEXT : Palette.TEXT_DIM);
        }
    }

    int selected() {
        return selected;
    }

    /** Focuses the tab of the page on screen. */
    void focusSelected() {
        tabs[Math.max(0, selected)].requestFocus();
    }
}
