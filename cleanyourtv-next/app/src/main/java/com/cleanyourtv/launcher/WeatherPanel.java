package com.cleanyourtv.launcher;

import android.content.Context;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The weather in the panel above the dock: one glass card with the current weather on the left and seven
 * days on the right. The card takes the focus (OK refreshes, at most once a minute). It only draws what it
 * is given: the states without data (no city, offline) are the content panel's message box.
 */
final class WeatherPanel extends FrameLayout {
    interface Listener {
        void onRefresh();

        boolean onUp();

        boolean onDown();
    }

    private final LinearLayout card;
    private final TextView city;
    private final TextView temperature;
    private final TextView condition;
    private final TextView range;
    private final ImageView bigGlyph;
    private final LinearLayout days;
    private final TextView footer;

    WeatherPanel(Context context, Prefs prefs, final Listener listener) {
        super(context);
        setClipChildren(false);

        card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setFocusable(true);
        card.setClickable(true);
        card.setDefaultFocusHighlightEnabled(false);
        card.setPadding(Ui.dp(context, 30), Ui.dp(context, 20), Ui.dp(context, 30), Ui.dp(context, 20));
        card.setBackground(Ui.focusGlass(context, Ui.dp(context, 30), true, prefs.effects()));
        card.setOnClickListener(view -> listener.onRefresh());
        card.setOnKeyListener((view, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) return listener.onUp();
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) return listener.onDown();
            return keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT;
        });

        LinearLayout left = new LinearLayout(context);
        left.setOrientation(LinearLayout.VERTICAL);
        city = Ui.text(context, 18, Palette.TEXT_DIM, Ui.medium(context));
        temperature = Ui.text(context, 64, Palette.TEXT, Ui.light(context));
        condition = Ui.text(context, 20, Palette.TEXT, Ui.regular(context));
        range = Ui.text(context, 15, Palette.TEXT_DIM, Ui.regular(context));
        range.setPadding(0, Ui.dp(context, 4), 0, 0);
        left.addView(city);
        left.addView(temperature);
        left.addView(condition);
        left.addView(range);
        card.addView(left, new LinearLayout.LayoutParams(Ui.dp(context, 250), ViewGroup.LayoutParams.WRAP_CONTENT));
        bigGlyph = new ImageView(context);
        card.addView(bigGlyph, new LinearLayout.LayoutParams(Ui.dp(context, 96), Ui.dp(context, 96)));

        days = new LinearLayout(context);
        days.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams daysParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        daysParams.leftMargin = Ui.dp(context, 24);
        card.addView(days, daysParams);
        addView(card, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(context, 190)));

        footer = Ui.text(context, 12, Palette.TEXT_FAINT, Ui.regular(context));
        LayoutParams footerParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        footerParams.topMargin = Ui.dp(context, 202);
        footerParams.leftMargin = Ui.dp(context, 8);
        addView(footer, footerParams);
    }

    boolean focusCard() {
        return card.requestFocus();
    }

    void show(Forecast f, boolean stale, String cityName) {
        Context context = getContext();
        WeatherCode code = WeatherCode.of(f.code);
        city.setText(cityName);
        temperature.setText(degrees(f.temperature));
        condition.setText(code.label);
        Forecast.Day today = f.days.get(0);
        range.setText(context.getString(R.string.weather_range, degrees(today.max), degrees(today.min)));
        bigGlyph.setImageDrawable(new WeatherGlyph(code, f.day));

        days.removeAllViews();
        int count = Math.min(7, f.days.size());
        for (int i = 0; i < count; i++) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            days.addView(dayCell(f.days.get(i), i == 0), params);
        }

        String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(f.fetchedAt));
        String text = context.getString(R.string.weather_updated, time);
        if (stale) text += "  •  " + context.getString(R.string.weather_stale);
        footer.setText(text + "  •  " + context.getString(R.string.weather_attribution));
    }

    private View dayCell(Forecast.Day day, boolean today) {
        Context context = getContext();
        LinearLayout cell = new LinearLayout(context);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView name = Ui.text(context, 14, today ? Palette.TEXT : Palette.TEXT_DIM, Ui.medium(context));
        name.setText(today ? context.getString(R.string.weather_today) : weekday(day.date));
        ImageView glyph = new ImageView(context);
        glyph.setImageDrawable(new WeatherGlyph(WeatherCode.of(day.code), true));
        TextView max = Ui.text(context, 19, Palette.TEXT, Ui.medium(context));
        max.setText(degrees(day.max));
        TextView min = Ui.text(context, 14, Palette.TEXT_FAINT, Ui.regular(context));
        min.setText(degrees(day.min));
        cell.addView(name);
        LinearLayout.LayoutParams glyphParams = new LinearLayout.LayoutParams(Ui.dp(context, 38), Ui.dp(context, 38));
        glyphParams.setMargins(0, Ui.dp(context, 8), 0, Ui.dp(context, 8));
        cell.addView(glyph, glyphParams);
        cell.addView(max);
        cell.addView(min);
        return cell;
    }

    static String degrees(double value) {
        return Math.round(value) + "°";
    }

    private static String weekday(String isoDate) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(isoDate);
            return new SimpleDateFormat("EEE", Locale.getDefault()).format(date);
        } catch (ParseException e) {
            return isoDate;
        }
    }
}
