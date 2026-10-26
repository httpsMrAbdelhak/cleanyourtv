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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Current weather and seven days, from Open-Meteo, on glass. The big card takes the focus (OK refreshes,
 * at most once a minute). Shows the time of the last update, says when the data is old, and never shows
 * a forecast it does not have: no city, no network or a bad answer each get their own plain message.
 */
final class WeatherPage extends Page {
    private final LinearLayout card;
    private final TextView city;
    private final TextView temperature;
    private final TextView condition;
    private final TextView range;
    private final ImageView bigGlyph;
    private final LinearLayout days;
    private final TextView footer;
    private final TextView message;
    private final TextView action;
    private final LinearLayout messageBox;
    private boolean shown;
    private boolean hasData;

    WeatherPage(Context context, Host host, Prefs prefs) {
        super(context, host, prefs);
        int w = getResources().getDisplayMetrics().widthPixels;
        int h = getResources().getDisplayMetrics().heightPixels;
        int side = Ui.dp(context, 72);

        card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setFocusable(true);
        card.setClickable(true);
        card.setDefaultFocusHighlightEnabled(false);
        card.setPadding(Ui.dp(context, 40), Ui.dp(context, 26), Ui.dp(context, 40), Ui.dp(context, 26));
        card.setBackground(Ui.focusGlass(context, Ui.dp(context, 36), true, prefs.effects()));
        card.setOnClickListener(view -> refresh(true));
        card.setOnKeyListener((view, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                host.focusTabs();
                return true;
            }
            return keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                    || keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
        });

        LinearLayout left = new LinearLayout(context);
        left.setOrientation(LinearLayout.VERTICAL);
        city = Ui.text(context, 24, Palette.TEXT_DIM, Ui.medium(context));
        temperature = Ui.text(context, 92, Palette.TEXT, Ui.light(context));
        condition = Ui.text(context, 26, Palette.TEXT, Ui.regular(context));
        range = Ui.text(context, 20, Palette.TEXT_DIM, Ui.regular(context));
        range.setPadding(0, Ui.dp(context, 6), 0, 0);
        left.addView(city);
        left.addView(temperature);
        left.addView(condition);
        left.addView(range);
        card.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bigGlyph = new ImageView(context);
        card.addView(bigGlyph, new LinearLayout.LayoutParams(Ui.dp(context, 150), Ui.dp(context, 150)));

        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(w - 2 * side, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        cardParams.setMargins(side, h * 19 / 100, side, 0);
        addView(card, cardParams);

        days = new LinearLayout(context);
        days.setOrientation(LinearLayout.HORIZONTAL);
        days.setClipChildren(false);
        FrameLayout.LayoutParams daysParams = new FrameLayout.LayoutParams(w - 2 * side, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        daysParams.setMargins(side, h * 19 / 100 + Ui.dp(context, 218), side, 0);
        addView(days, daysParams);

        footer = Ui.text(context, 13, Palette.TEXT_FAINT, Ui.regular(context));
        FrameLayout.LayoutParams footerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
        footerParams.setMargins(side, 0, 0, Ui.dp(context, 22));
        addView(footer, footerParams);

        messageBox = new LinearLayout(context);
        messageBox.setOrientation(LinearLayout.VERTICAL);
        messageBox.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = Ui.dp(context, 34);
        messageBox.setPadding(pad, pad, pad, pad);
        messageBox.setBackground(new GlassDrawable(Ui.dp(context, 32), Ui.dp(context, 1.5f), true, false, prefs.effects()));
        message = Ui.text(context, 22, Palette.TEXT, Ui.regular(context));
        message.setGravity(Gravity.CENTER);
        message.setLineSpacing(0, 1.15f);
        action = Ui.text(context, 20, Palette.TEXT, Ui.medium(context));
        action.setGravity(Gravity.CENTER);
        action.setPadding(Ui.dp(context, 30), Ui.dp(context, 13), Ui.dp(context, 30), Ui.dp(context, 13));
        action.setFocusable(true);
        action.setClickable(true);
        action.setDefaultFocusHighlightEnabled(false);
        action.setBackground(Ui.focusGlass(context, 0x33FFFFFF, 0x66FFFFFF, Ui.dp(context, 26)));
        action.setOnKeyListener((view, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                host.focusTabs();
                return true;
            }
            return false;
        });
        messageBox.addView(message, new LinearLayout.LayoutParams(Ui.dp(context, 520), ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionParams.topMargin = Ui.dp(context, 22);
        messageBox.addView(action, actionParams);
        addView(messageBox, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        showMessage(R.string.weather_loading, 0, null);
    }

    @Override
    void onShown() {
        shown = true;
        refresh(false);
    }

    @Override
    void onHidden() {
        shown = false;
    }

    @Override
    boolean enter() {
        return messageBox.getVisibility() == VISIBLE ? action.requestFocus() : card.requestFocus();
    }

    private void refresh(boolean force) {
        if (!prefs.hasCity()) {
            showMessage(R.string.weather_no_city, R.string.action_open_settings, () -> host.openSettings());
            return;
        }
        Weather.load(getContext(), force, result -> {
            if (!shown) return;
            show(result);
        });
    }

    private void show(Weather.Result result) {
        if (result.error != Weather.Error.NONE || result.forecast == null) {
            if (hasData) return;
            int text = result.error == Weather.Error.OFFLINE ? R.string.weather_offline : R.string.weather_failed;
            showMessage(text, R.string.action_retry, () -> refresh(true));
            return;
        }
        hasData = true;
        messageBox.setVisibility(GONE);
        card.setVisibility(VISIBLE);
        days.setVisibility(VISIBLE);
        Forecast f = result.forecast;
        WeatherCode code = WeatherCode.of(f.code);
        city.setText(prefs.cityName());
        temperature.setText(degrees(f.temperature));
        condition.setText(code.label);
        Forecast.Day today = f.days.get(0);
        range.setText(getContext().getString(R.string.weather_range, degrees(today.max), degrees(today.min)));
        bigGlyph.setImageDrawable(new WeatherGlyph(code, f.day));

        days.removeAllViews();
        int count = Math.min(7, f.days.size());
        for (int i = 0; i < count; i++) {
            Forecast.Day day = f.days.get(i);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            params.setMargins(Ui.dp(getContext(), i == 0 ? 0 : 7), 0, Ui.dp(getContext(), i == count - 1 ? 0 : 7), 0);
            days.addView(dayCell(day, i == 0), params);
        }

        String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(f.fetchedAt));
        String text = getContext().getString(R.string.weather_updated, time);
        if (result.stale) text += "  •  " + getContext().getString(R.string.weather_stale);
        footer.setText(text + "  •  " + getContext().getString(R.string.weather_attribution));
    }

    private View dayCell(Forecast.Day day, boolean today) {
        Context context = getContext();
        LinearLayout cell = new LinearLayout(context);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setPadding(0, Ui.dp(context, 16), 0, Ui.dp(context, 16));
        cell.setBackground(new GlassDrawable(Ui.dp(context, 26), Ui.dp(context, 1.5f), true, today, prefs.effects()));
        TextView name = Ui.text(context, 18, today ? Palette.TEXT : Palette.TEXT_DIM, Ui.medium(context));
        name.setText(today ? context.getString(R.string.weather_today) : weekday(day.date));
        ImageView glyph = new ImageView(context);
        glyph.setImageDrawable(new WeatherGlyph(WeatherCode.of(day.code), true));
        TextView max = Ui.text(context, 24, Palette.TEXT, Ui.medium(context));
        max.setText(degrees(day.max));
        TextView min = Ui.text(context, 18, Palette.TEXT_FAINT, Ui.regular(context));
        min.setText(degrees(day.min));
        cell.addView(name);
        LinearLayout.LayoutParams glyphParams = new LinearLayout.LayoutParams(Ui.dp(context, 54), Ui.dp(context, 54));
        glyphParams.setMargins(0, Ui.dp(context, 10), 0, Ui.dp(context, 10));
        cell.addView(glyph, glyphParams);
        cell.addView(max);
        cell.addView(min);
        return cell;
    }

    private void showMessage(int text, int actionText, final Runnable onAction) {
        card.setVisibility(INVISIBLE);
        days.setVisibility(INVISIBLE);
        footer.setText("");
        hasData = false;
        message.setText(text);
        action.setVisibility(actionText == 0 ? GONE : VISIBLE);
        if (actionText != 0) {
            action.setText(actionText);
            action.setOnClickListener(view -> onAction.run());
        }
        messageBox.setVisibility(VISIBLE);
    }

    private static String degrees(double value) {
        return Math.round(value) + "°";
    }

    private static String weekday(String isoDate) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(isoDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return new SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.getTime());
        } catch (ParseException e) {
            return isoDate;
        }
    }
}
