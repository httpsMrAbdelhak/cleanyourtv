package com.cleanyourtv.launcher;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * The bar across the top: an avatar, the sections (Home, Library, My list, Apps, Games), search, and on the
 * right the theme, performance and settings buttons with a pill holding the weather and the time. The section
 * on screen is a filled pill; the focused item lights up. Down leaves the bar, Up does nothing.
 */
final class TopBar extends LinearLayout {
    static final int HOME = 0;
    static final int LIBRARY = 1;
    static final int MY_LIST = 2;
    static final int APPS = 3;
    static final int GAMES = 4;

    interface Listener {
        /** The focus rests on Home, Library or My list: show that page. */
        void onSectionFocused(int section);

        /** OK on a section (Apps and Games open the drawer). */
        void onSectionChosen(int section);

        void onSearch();

        void onTheme();

        void onEffects();

        void onSettings();

        void onWeather();

        /** Down was pressed in the bar. */
        void onLeaveDown();
    }

    private final TextView[] sections = new TextView[5];
    private final List<View> focusables = new ArrayList<>();
    private final ImageView weatherIcon;
    private final TextView weatherText;
    private final Listener listener;
    private int selected = HOME;

    TopBar(Context context, Prefs prefs, final Listener listener) {
        super(context);
        this.listener = listener;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setClipChildren(false);
        setClipToPadding(false);

        TextView avatar = Ui.text(context, 16, 0xFFFFFFFF, Ui.semibold(context));
        String name = prefs.name().trim();
        avatar.setText(name.isEmpty() ? "•" : name.substring(0, 1).toUpperCase(java.util.Locale.getDefault()));
        avatar.setGravity(Gravity.CENTER);
        GradientDrawable disc = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[] {prefs.accent().color, 0xFF8A5CF0});
        disc.setShape(GradientDrawable.OVAL);
        disc.setStroke(Ui.dp(context, 1.5f), 0xB3FFFFFF);
        avatar.setBackground(disc);
        LayoutParams avatarParams = new LayoutParams(Ui.dp(context, 34), Ui.dp(context, 34));
        avatarParams.rightMargin = Ui.dp(context, 14);
        addView(avatar, avatarParams);

        int[] names = {R.string.nav_home, R.string.nav_library, R.string.nav_my_list, R.string.nav_apps, R.string.nav_games};
        for (int i = 0; i < sections.length; i++) {
            final int index = i;
            TextView pill = Ui.text(context, 16, Palette.TEXT, Ui.medium(context));
            pill.setText(names[i]);
            pill.setGravity(Gravity.CENTER);
            pill.setPadding(Ui.dp(context, 16), Ui.dp(context, 7), Ui.dp(context, 16), Ui.dp(context, 7));
            pill.setFocusable(true);
            pill.setClickable(true);
            pill.setDefaultFocusHighlightEnabled(false);
            pill.setOnFocusChangeListener((view, hasFocus) -> {
                if (hasFocus && index <= MY_LIST) listener.onSectionFocused(index);
            });
            pill.setOnClickListener(view -> listener.onSectionChosen(index));
            sections[i] = pill;
            LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.rightMargin = Ui.dp(context, 4);
            addFocusable(pill, params);
        }
        View search = roundButton(context, Glyphs.search(Palette.TEXT), prefs, () -> listener.onSearch());
        LayoutParams searchParams = new LayoutParams(Ui.dp(context, 34), Ui.dp(context, 34));
        searchParams.leftMargin = Ui.dp(context, 8);
        addFocusable(search, searchParams);

        View spacer = new View(context);
        addView(spacer, new LayoutParams(0, 1, 1f));

        addFocusable(roundButton(context, Glyphs.moon(Palette.TEXT), prefs, () -> listener.onTheme()), roundParams(context));
        addFocusable(roundButton(context, Glyphs.bolt(Palette.TEXT), prefs, () -> listener.onEffects()), roundParams(context));
        addFocusable(roundButton(context, Glyphs.settings(Palette.TEXT), prefs, () -> listener.onSettings()), roundParams(context));

        LinearLayout weather = new LinearLayout(context);
        weather.setOrientation(HORIZONTAL);
        weather.setGravity(Gravity.CENTER_VERTICAL);
        weather.setPadding(Ui.dp(context, 14), 0, Ui.dp(context, 14), 0);
        weather.setFocusable(true);
        weather.setClickable(true);
        weather.setDefaultFocusHighlightEnabled(false);
        weather.setBackground(focusPill(context, prefs, Ui.dp(context, 17)));
        weather.setOnClickListener(view -> listener.onWeather());
        weatherIcon = new ImageView(context);
        weatherIcon.setVisibility(GONE);
        weather.addView(weatherIcon, new LayoutParams(Ui.dp(context, 20), Ui.dp(context, 20)));
        weatherText = Ui.text(context, 15, Palette.TEXT, Ui.semibold(context));
        weatherText.setVisibility(GONE);
        LayoutParams tempParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tempParams.leftMargin = Ui.dp(context, 6);
        tempParams.rightMargin = Ui.dp(context, 12);
        weather.addView(weatherText, tempParams);
        TextClock clock = new TextClock(context);
        clock.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 15 * Ui.unit(context));
        clock.setTextColor(Palette.TEXT);
        clock.setTypeface(Ui.semibold(context));
        String pattern = prefs.clockMode().pattern;
        clock.setFormat12Hour(pattern);
        clock.setFormat24Hour(pattern);
        weather.addView(clock);
        LayoutParams weatherParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(context, 34));
        weatherParams.leftMargin = Ui.dp(context, 10);
        addFocusable(weather, weatherParams);

        select(HOME);
    }

    private LayoutParams roundParams(Context context) {
        LayoutParams params = new LayoutParams(Ui.dp(context, 34), Ui.dp(context, 34));
        params.leftMargin = Ui.dp(context, 10);
        return params;
    }

    private void addFocusable(final View view, LayoutParams params) {
        focusables.add(view);
        view.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            int at = focusables.indexOf(v);
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP: return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    listener.onLeaveDown();
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT: return at == 0;
                case KeyEvent.KEYCODE_DPAD_RIGHT: return at == focusables.size() - 1;
                default: return false;
            }
        });
        addView(view, params);
    }

    private static View roundButton(Context context, Drawable glyph, Prefs prefs, final Runnable action) {
        ImageView button = new ImageView(context);
        int pad = Ui.dp(context, 9);
        button.setPadding(pad, pad, pad, pad);
        button.setImageDrawable(glyph);
        button.setFocusable(true);
        button.setClickable(true);
        button.setDefaultFocusHighlightEnabled(false);
        button.setBackground(focusPill(context, prefs, Ui.dp(context, 17)));
        button.setOnClickListener(view -> action.run());
        button.setOnFocusChangeListener((view, hasFocus) -> {
            float scale = hasFocus ? 1.1f : 1f;
            view.animate().scaleX(scale).scaleY(scale).setDuration(140).start();
        });
        return button;
    }

    /** A glass pill with a thin light edge, brighter with a ring when focused. */
    private static StateListDrawable focusPill(Context context, Prefs prefs, float radius) {
        GradientDrawable lit = Ui.round(Palette.TILE_FOCUSED, radius);
        lit.setStroke(Ui.dp(context, 2), Palette.RING);
        GradientDrawable rest = Ui.round(Palette.TILE, radius);
        rest.setStroke(Ui.dp(context, 1), 0x59FFFFFF);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_focused}, lit);
        states.addState(new int[] {}, rest);
        return states;
    }

    /** Marks the section on screen: a filled pill, bolder text. */
    void select(int section) {
        selected = section;
        Context context = getContext();
        for (int i = 0; i < sections.length; i++) {
            boolean on = i == section;
            float radius = Ui.dp(context, 17);
            GradientDrawable lit = Ui.round(on ? Palette.TILE_FOCUSED : Palette.TILE, radius);
            lit.setStroke(Ui.dp(context, 2), Palette.RING);
            GradientDrawable rest = Ui.round(on ? (Palette.light ? 0x33000000 : 0x40FFFFFF) : 0x00000000, radius);
            if (on) rest.setStroke(Ui.dp(context, 1), 0x59FFFFFF);
            StateListDrawable states = new StateListDrawable();
            states.addState(new int[] {android.R.attr.state_focused}, new LayerDrawable(new Drawable[] {lit}));
            states.addState(new int[] {}, rest);
            sections[i].setBackground(states);
            sections[i].setTypeface(on ? Ui.semibold(context) : Ui.medium(context));
            sections[i].setTextColor(on ? Palette.TEXT : Palette.TEXT_DIM);
        }
    }

    int selected() {
        return selected;
    }

    void focusSelected() {
        sections[Math.min(selected, MY_LIST)].requestFocus();
    }

    boolean contains(View view) {
        return view != null && focusables.contains(view);
    }

    /** The current temperature and condition in the weather pill; null hides them, leaving the time. */
    void setWeather(Forecast forecast) {
        if (forecast == null) {
            weatherIcon.setVisibility(GONE);
            weatherText.setVisibility(GONE);
            return;
        }
        weatherIcon.setImageDrawable(new WeatherGlyph(WeatherCode.of(forecast.code), forecast.day));
        weatherIcon.setVisibility(VISIBLE);
        weatherText.setText(WeatherPanel.degrees(forecast.temperature));
        weatherText.setVisibility(VISIBLE);
    }
}
