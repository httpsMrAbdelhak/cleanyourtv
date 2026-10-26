package com.cleanyourtv.launcher;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The first tab: a quiet clock and date on the wallpaper, and the dock of favourite apps in a glass pill at
 * the bottom. The name of the selected app fades in above the dock; the light under it is driven by the
 * screen (see HomeActivity). Left and Right move along the dock, OK opens an app, a long OK edits the dock,
 * Up goes to the tab bar, and Down does nothing: no app grid ever opens by surprise.
 */
final class HomePage extends Page {
    private final TextView greeting;
    private final TextClock clock;
    private final TextClock date;
    private final TextView dockLabel;
    private final LinearLayout dock;
    private final Map<String, String> labels = new HashMap<>();
    private String renderedDock = "";
    private boolean loadedApps;

    HomePage(Context context, Host host, Prefs prefs) {
        super(context, host, prefs);
        int h = getResources().getDisplayMetrics().heightPixels;
        int side = Ui.dp(context, 72);

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.VERTICAL);
        greeting = Ui.text(context, 22, Palette.TEXT_DIM, Ui.medium(context));
        clock = new TextClock(context);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 76);
        clock.setTextColor(Palette.TEXT);
        clock.setTypeface(Ui.light(context));
        clock.setIncludeFontPadding(false);
        date = new TextClock(context);
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        date.setTextColor(Palette.TEXT_DIM);
        date.setTypeface(Ui.regular(context));
        date.setIncludeFontPadding(false);
        date.setPadding(0, Ui.dp(context, 4), 0, 0);
        header.addView(greeting);
        header.addView(clock);
        header.addView(date);
        FrameLayout.LayoutParams headerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        headerParams.setMargins(side, h * 21 / 100, 0, 0);
        addView(header, headerParams);

        dock = new LinearLayout(context);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setClipChildren(false);
        dock.setClipToPadding(false);
        int pad = Ui.dp(context, 18);
        dock.setPadding(pad, pad, pad, pad);
        dock.setBackground(new GlassDrawable(Ui.dp(context, 40), Ui.dp(context, 1.5f), true, false, prefs.effects()));
        FrameLayout.LayoutParams dockParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dockParams.bottomMargin = Ui.dp(context, 52);
        addView(dock, dockParams);

        dockLabel = Ui.text(context, 22, Palette.TEXT, Ui.medium(context));
        dockLabel.setAlpha(0f);
        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        labelParams.bottomMargin = Ui.dp(context, 52 + 124 + 16);
        addView(dockLabel, labelParams);
    }

    // --- Page --------------------------------------------------------------------------------------------

    @Override
    void onShown() {
        applyClock();
        updateGreeting();
        AppRepository.loadAsync(getContext(), true, apps -> {
            if (!isAttachedToWindow()) return;
            onAppsLoaded(apps);
        });
    }

    @Override
    void onHidden() { }

    @Override
    boolean enter() {
        int count = dock.getChildCount();
        if (count == 0) return false;
        View focused = dock.getFocusedChild();
        if (focused != null) return true;
        return dock.getChildAt(0).requestFocus();
    }

    @Override
    void onLookChanged() {
        renderedDock = "";
        dock.setBackground(new GlassDrawable(Ui.dp(getContext(), 40), Ui.dp(getContext(), 1.5f), true, false, prefs.effects()));
        rebuildDock(-1);
    }

    /** Called by the screen each minute and on resume. */
    void tick() {
        updateGreeting();
    }

    private void applyClock() {
        // TextClock reads its format once: set it again after a language or 12/24 hour change.
        String pattern = prefs.clockMode().pattern;
        clock.setFormat12Hour(pattern);
        clock.setFormat24Hour(pattern);
        String dateFormat = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM");
        date.setFormat12Hour(dateFormat);
        date.setFormat24Hour(dateFormat);
    }

    private void updateGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int text;
        switch (Greeting.forHour(hour)) {
            case MORNING: text = R.string.greeting_morning; break;
            case AFTERNOON: text = R.string.greeting_afternoon; break;
            default: text = R.string.greeting_evening; break;
        }
        greeting.setText(Greeting.compose(getContext().getString(text), prefs.name()));
    }

    // --- dock --------------------------------------------------------------------------------------------

    private void onAppsLoaded(List<AppEntry> apps) {
        labels.clear();
        for (AppEntry app : apps) labels.put(app.packageName, app.label);
        if (!prefs.dockConfigured()) prefs.saveDock(DockDefaults.pick(new HashSet<>(labels.keySet())));
        loadedApps = true;
        rebuildDock(-1);
    }

    private void rebuildDock(int focusOverride) {
        if (!loadedApps) return;
        List<String> shown = prefs.dock().visible(labels.keySet());
        String key = shown + "|" + prefs.accent().id + "|" + prefs.effects().id;
        if (key.equals(renderedDock) && focusOverride < 0) return;
        renderedDock = key;

        View focused = dock.getFocusedChild();
        int focusIndex = focusOverride >= 0 ? focusOverride : (focused == null ? -1 : dock.indexOfChild(focused));
        dock.removeAllViews();

        Context context = getContext();
        int count = shown.size() + 1;
        int available = getResources().getDisplayMetrics().widthPixels - Ui.dp(context, 2 * 72 + 2 * 18);
        int gap = Ui.dp(context, 9);
        int size = Math.min(Ui.dp(context, 88), available / count - 2 * gap);
        int iconPx = size - Ui.dp(context, 10);
        PackageManager pm = context.getPackageManager();

        for (final String packageName : shown) {
            DockTile tile = new DockTile(context, packageName, labels.get(packageName), size);
            IconCache.get().load(pm, packageName, iconPx, tile.iconView());
            tile.setOnClickListener(view -> open(packageName));
            tile.setOnLongClickListener(view -> {
                showDockMenu(packageName);
                return true;
            });
            addTile(tile, gap, size);
        }

        DockTile all = new DockTile(context, null, context.getString(R.string.dock_all_apps), size);
        all.iconView().setImageDrawable(Glyphs.apps(Palette.TEXT));
        all.iconView().setBackground(Ui.round(0x33FFFFFF, size * 0.2f));
        int glyphPad = size / 5;
        all.iconView().setPadding(glyphPad, glyphPad, glyphPad, glyphPad);
        all.setOnClickListener(view -> host.openLibrary());
        addTile(all, gap, size);

        if (focusIndex >= 0) dock.getChildAt(Math.min(focusIndex, dock.getChildCount() - 1)).requestFocus();
    }

    private void addTile(final DockTile tile, int gap, int size) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(gap, 0, gap, 0);
        tile.setOnKeyListener((view, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            int at = dock.indexOfChild(view);
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    host.focusTabs();
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    return at == 0;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    return at == dock.getChildCount() - 1;
                default:
                    return false;
            }
        });
        final View.OnFocusChangeListener grow = tile.getOnFocusChangeListener();
        tile.setOnFocusChangeListener((view, hasFocus) -> {
            grow.onFocusChange(view, hasFocus);
            if (hasFocus) {
                dockLabel.setText(tile.label);
                dockLabel.animate().alpha(1f).setDuration(200).start();
            } else {
                dockLabel.animate().alpha(0f).setDuration(160).start();
            }
        });
        dock.addView(tile, params);
    }

    private void open(String packageName) {
        if (!AppRepository.launch(getContext(), packageName)) {
            Toast.makeText(getContext(), R.string.toast_launch_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void showDockMenu(final String packageName) {
        final DockModel model = prefs.dock();
        final Set<String> installed = labels.keySet();
        final int at = model.visible(installed).indexOf(packageName);
        final int last = model.visible(installed).size() - 1;
        ChoiceDialog dialog = new ChoiceDialog((android.app.Activity) getContext(), labels.get(packageName));
        if (at > 0) {
            dialog.option(getContext().getString(R.string.menu_move_left), () -> {
                if (model.moveLeft(packageName, installed)) saveAndFocus(model, at - 1);
            });
        }
        if (at >= 0 && at < last) {
            dialog.option(getContext().getString(R.string.menu_move_right), () -> {
                if (model.moveRight(packageName, installed)) saveAndFocus(model, at + 1);
            });
        }
        dialog.option(getContext().getString(R.string.menu_remove_dock), () -> {
            model.remove(packageName);
            saveAndFocus(model, at);
            Toast.makeText(getContext(), R.string.toast_removed, Toast.LENGTH_SHORT).show();
        });
        dialog.show();
    }

    private void saveAndFocus(DockModel model, int index) {
        prefs.saveDock(model);
        rebuildDock(Math.max(0, index));
    }

    /** The long-press of the Menu key, forwarded by the screen. */
    boolean menuKey(View focused) {
        return focused instanceof DockTile && focused.isLongClickable() && focused.performLongClick();
    }
}
