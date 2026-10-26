package com.cleanyourtv.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

/**
 * The home screen: greeting, clock, date, and a dock of favourite apps. Everything is driven by the
 * D-pad: Left and Right move along the dock, OK opens, and a long OK (or Menu) edits the dock.
 * Back does nothing here, as on any home screen.
 */
public final class HomeActivity extends Activity {
    private static final int GREETING_REFRESH_MS = 60_000;

    private Prefs prefs;
    private FrameLayout root;
    private View wallpaper;
    private TextView greeting;
    private TextClock clock;
    private TextClock date;
    private LinearLayout dock;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable greetingTick = new Runnable() {
        @Override public void run() {
            updateGreeting();
            handler.postDelayed(this, GREETING_REFRESH_MS);
        }
    };

    private final Map<String, String> labels = new HashMap<>();
    private String renderedDock = "";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new Prefs(this);
        buildUi();
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyLook();
        handler.post(greetingTick);
        AppRepository.loadAsync(this, true, apps -> {
            if (isDestroyed() || isFinishing()) return;
            onAppsLoaded(apps);
        });
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(greetingTick);
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Home pressed while already here: put the focus back on the first tile.
        focusDock(0);
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration config) {
        super.onConfigurationChanged(config);
        applyLook();
        renderedDock = "";
        rebuildDock();
    }

    /** A home screen has nowhere to go back to. */
    @Override
    public void onBackPressed() {
        // Intentionally empty.
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            View focused = getCurrentFocus();
            if (focused != null && focused.isLongClickable()) return focused.performLongClick();
        }
        return super.onKeyUp(keyCode, event);
    }

    // --- layout -----------------------------------------------------------------------------------------

    private void buildUi() {
        int margin = Ui.dp(this, 48);

        root = new FrameLayout(this);
        root.setClipChildren(false);

        wallpaper = new View(this);
        root.addView(wallpaper, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // A soft shade under the dock keeps the labels readable on any wallpaper.
        View shade = new View(this);
        shade.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0x00000000, 0xCC000000}));
        root.addView(shade, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 280), Gravity.BOTTOM));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        greeting = Ui.text(this, 24, Palette.TEXT_DIM, Ui.light());
        clock = new TextClock(this);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 96);
        clock.setTextColor(Palette.TEXT);
        clock.setTypeface(Ui.light());
        clock.setIncludeFontPadding(false);
        date = new TextClock(this);
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        date.setTextColor(Palette.TEXT_DIM);
        date.setTypeface(Ui.light());
        header.addView(greeting);
        header.addView(clock);
        header.addView(date);
        FrameLayout.LayoutParams headerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        headerParams.setMargins(margin, margin, margin, 0);
        root.addView(header, headerParams);

        dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        int panelPad = Ui.dp(this, 12);
        dock.setPadding(panelPad, panelPad, panelPad, panelPad);
        dock.setBackground(Ui.round(Palette.PANEL, Ui.dp(this, 30)));
        TileView.allowOverflow(dock);
        FrameLayout.LayoutParams dockParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dockParams.bottomMargin = Ui.dp(this, 88);
        root.addView(dock, dockParams);

        TextView hint = Ui.text(this, 14, Palette.TEXT_DIM, null);
        hint.setText(R.string.dock_hint);
        hint.setAlpha(0.7f);
        root.addView(hint, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));
        ((FrameLayout.LayoutParams) hint.getLayoutParams()).bottomMargin = Ui.dp(this, 40);
    }

    private void applyLook() {
        wallpaper.setBackground(prefs.wallpaper().load(this));
        // TextClock reads its format once: set it again after a language or 12/24 hour change.
        String pattern = prefs.clockMode().pattern;
        clock.setFormat12Hour(pattern);
        clock.setFormat24Hour(pattern);
        String dateFormat = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM");
        date.setFormat12Hour(dateFormat);
        date.setFormat24Hour(dateFormat);
        updateGreeting();
    }

    private void updateGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int text;
        switch (Greeting.forHour(hour)) {
            case MORNING: text = R.string.greeting_morning; break;
            case AFTERNOON: text = R.string.greeting_afternoon; break;
            default: text = R.string.greeting_evening; break;
        }
        greeting.setText(Greeting.compose(getString(text), prefs.name()));
    }

    // --- dock -------------------------------------------------------------------------------------------

    private void onAppsLoaded(List<AppEntry> apps) {
        labels.clear();
        for (AppEntry app : apps) labels.put(app.packageName, app.label);
        if (!prefs.dockConfigured()) prefs.saveDock(DockDefaults.pick(new HashSet<>(labels.keySet())));
        rebuildDock();
        if (getCurrentFocus() == null) focusDock(0);
    }

    private void rebuildDock() {
        rebuildDock(-1);
    }

    /** focusOverride: the tile to focus afterwards, or -1 to keep the focus where it was. */
    private void rebuildDock(int focusOverride) {
        List<String> shown = prefs.dock().visible(labels.keySet());
        int accent = prefs.accent().color;
        String key = shown + "|" + accent + "|" + getResources().getConfiguration().getLocales().get(0);
        if (key.equals(renderedDock)) return;
        renderedDock = key;

        int focusIndex = focusOverride >= 0 ? focusOverride : indexOfFocusedTile();
        dock.removeAllViews();

        int count = shown.size() + 2;
        int available = getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 2 * 48 + 2 * 12);
        int gap = Ui.dp(this, 6);
        int size = Math.min(Ui.dp(this, 120), available / count - 2 * gap);
        int iconPx = size * 52 / 100;
        PackageManager pm = getPackageManager();

        for (final String packageName : shown) {
            TileView tile = new TileView(this, size, size, iconPx, accent);
            tile.setLabel(labels.get(packageName));
            IconCache.get().load(pm, packageName, iconPx, tile.iconView());
            tile.setOnClickListener(view -> open(packageName));
            tile.setOnLongClickListener(view -> {
                showDockMenu(packageName);
                return true;
            });
            addTile(tile, gap);
        }

        TileView all = new TileView(this, size, size, iconPx, accent);
        all.setLabel(getString(R.string.dock_all_apps));
        all.iconView().setImageDrawable(Glyphs.apps(Palette.TEXT));
        all.setOnClickListener(view -> startActivity(new Intent(this, LibraryActivity.class)));
        addTile(all, gap);

        TileView settings = new TileView(this, size, size, iconPx, accent);
        settings.setLabel(getString(R.string.dock_settings));
        settings.iconView().setImageDrawable(Glyphs.settings(Palette.TEXT));
        settings.setOnClickListener(view -> startActivity(new Intent(this, SettingsActivity.class)));
        addTile(settings, gap);

        focusDock(focusIndex < 0 ? 0 : focusIndex);
    }

    private void addTile(TileView tile, int gap) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(tile.getLayoutParams());
        params.setMargins(gap, gap, gap, gap);
        dock.addView(tile, params);
    }

    private int indexOfFocusedTile() {
        View focused = getCurrentFocus();
        return focused == null ? -1 : dock.indexOfChild(focused);
    }

    private void focusDock(int index) {
        int count = dock.getChildCount();
        if (count == 0) return;
        dock.getChildAt(Math.max(0, Math.min(index, count - 1))).requestFocus();
    }

    private void open(String packageName) {
        if (!AppRepository.launch(this, packageName)) {
            Toast.makeText(this, R.string.toast_launch_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void showDockMenu(final String packageName) {
        final DockModel model = prefs.dock();
        final java.util.Set<String> installed = labels.keySet();
        final int at = model.visible(installed).indexOf(packageName);
        final int last = model.visible(installed).size() - 1;
        ChoiceDialog dialog = new ChoiceDialog(this, labels.get(packageName));
        if (at > 0) {
            dialog.option(getString(R.string.menu_move_left), () -> {
                if (model.moveLeft(packageName, installed)) saveAndFocus(model, at - 1);
            });
        }
        if (at >= 0 && at < last) {
            dialog.option(getString(R.string.menu_move_right), () -> {
                if (model.moveRight(packageName, installed)) saveAndFocus(model, at + 1);
            });
        }
        dialog.option(getString(R.string.menu_remove_dock), () -> {
            model.remove(packageName);
            saveAndFocus(model, at);
            Toast.makeText(this, R.string.toast_removed, Toast.LENGTH_SHORT).show();
        });
        dialog.show();
    }

    private void saveAndFocus(DockModel model, int index) {
        prefs.saveDock(model);
        rebuildDock(Math.max(0, index));
    }
}
