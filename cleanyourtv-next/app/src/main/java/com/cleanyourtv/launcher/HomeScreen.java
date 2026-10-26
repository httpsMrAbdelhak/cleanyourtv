package com.cleanyourtv.launcher;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
 * The whole home screen, in three zones like tvOS: a header (greeting, three tabs, clock and the settings
 * button), a content panel, and a smoked-glass dock of wide app banners at the bottom. The selected banner gets a
 * ring and lifts, and a light of its colour runs along the whole bottom of the screen, brightest under it.
 * The dock holds the focus at start. Up climbs to the panel, then the header; Down on the dock opens the list
 * of all apps, as wide banners; Back from the panel or the header returns to the dock.
 */
final class HomeScreen extends FrameLayout implements ContentPanel.Host {
    /** What the screen asks of the activity that hosts it. */
    interface Host {
        void openSettings();

        void openLibrary();

        void requestTvPermission();
    }

    private static final long SELECT_DELAY_MS = 220;

    private final Host host;
    private final Prefs prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AmbientGlow glow;
    private final TextView title;
    private final TextClock clock;
    private final TextClock date;
    private final TextView[] tabs = new TextView[3];
    private final View settingsButton;
    private final LinearLayout dock;
    private final ContentPanel panel;
    private final Map<String, String> labels = new HashMap<>();
    private final Runnable applySelection = new Runnable() {
        @Override public void run() {
            if (pendingTile != null) select(pendingTile);
        }
    };
    private final Runnable applyTab = new Runnable() {
        @Override public void run() {
            chooseTab(pendingTab);
        }
    };

    private BannerTile pendingTile;
    private int pendingTab;
    private int selectedTab = EntryFilter.EXPLORE;
    private int lastDockIndex;
    private String renderedDock = "";
    private boolean loadedApps;
    private Forecast forecast;

    HomeScreen(Context context, Host host, Prefs prefs) {
        super(context);
        this.host = host;
        this.prefs = prefs;
        setClipChildren(false);
        int side = Ui.dp(context, 56);

        glow = new AmbientGlow(context);
        glow.setStrength(Palette.light ? 0.6f : 0.85f);
        int height = getResources().getDisplayMetrics().heightPixels;
        addView(glow, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height * 50 / 100, Gravity.BOTTOM));

        // --- header ---
        title = Ui.text(context, 30, Palette.TEXT, Ui.light(context));
        LayoutParams titleParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        titleParams.setMargins(side, Ui.dp(context, 30), 0, 0);
        addView(title, titleParams);

        LinearLayout tabRow = new LinearLayout(context);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setClipChildren(false);
        int[] tabNames = {R.string.tab_explore, R.string.tab_movies, R.string.tab_series};
        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            TextView tab = Ui.text(context, 16, Palette.TEXT_DIM, Ui.medium(context));
            tab.setText(tabNames[i]);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(Ui.dp(context, 14), Ui.dp(context, 6), Ui.dp(context, 14), Ui.dp(context, 6));
            tab.setFocusable(true);
            tab.setClickable(true);
            tab.setDefaultFocusHighlightEnabled(false);
            tab.setBackground(Ui.focusGlass(context, 0x00000000, Palette.TILE_FOCUSED, Ui.dp(context, 18)));
            tab.setOnFocusChangeListener((view, hasFocus) -> {
                if (!hasFocus) return;
                pendingTab = index;
                handler.removeCallbacks(applyTab);
                handler.postDelayed(applyTab, SELECT_DELAY_MS);
            });
            tab.setOnKeyListener((view, keyCode, event) -> headerKey(keyCode, event));
            tab.setOnClickListener(view -> chooseTab(index));
            tabs[i] = tab;
            tabRow.addView(tab);
        }
        LayoutParams tabsParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        tabsParams.setMargins(side - Ui.dp(context, 14), Ui.dp(context, 78), 0, 0);
        addView(tabRow, tabsParams);

        LinearLayout right = new LinearLayout(context);
        right.setOrientation(LinearLayout.HORIZONTAL);
        right.setGravity(Gravity.CENTER_VERTICAL);
        right.setClipChildren(false);
        LinearLayout when = new LinearLayout(context);
        when.setOrientation(LinearLayout.VERTICAL);
        when.setGravity(Gravity.END);
        clock = new TextClock(context);
        clock.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 24 * Ui.unit(context));
        clock.setTextColor(Palette.TEXT);
        clock.setTypeface(Ui.regular(context));
        clock.setIncludeFontPadding(false);
        clock.setGravity(Gravity.END);
        date = new TextClock(context);
        date.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 12 * Ui.unit(context));
        date.setTextColor(Palette.TEXT_DIM);
        date.setTypeface(Ui.regular(context));
        date.setIncludeFontPadding(false);
        date.setGravity(Gravity.END);
        when.addView(clock);
        when.addView(date);
        LinearLayout.LayoutParams whenParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        whenParams.rightMargin = Ui.dp(context, 18);
        right.addView(when, whenParams);
        settingsButton = roundButton(context, Glyphs.settings(Palette.TEXT), () -> host.openSettings());
        right.addView(settingsButton, new LinearLayout.LayoutParams(Ui.dp(context, 42), Ui.dp(context, 42)));
        LayoutParams rightParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        rightParams.setMargins(0, Ui.dp(context, 32), side, 0);
        addView(right, rightParams);

        // --- panel ---
        panel = new ContentPanel(context, this, prefs);
        LayoutParams panelParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                Ui.dp(context, 284), Gravity.TOP | Gravity.START);
        panelParams.setMargins(side - Ui.dp(context, 6), Ui.dp(context, 120), 0, 0);
        addView(panel, panelParams);

        // --- dock: smoked glass, wide banners ---
        dock = new LinearLayout(context);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setClipChildren(false);
        dock.setClipToPadding(false);
        int pad = Ui.dp(context, 14);
        dock.setPadding(pad, pad, pad, pad);
        dock.setBackground(new GlassDrawable(Ui.dp(context, 30), Ui.dp(context, 1.5f), true, false, true, prefs.effects()));
        LayoutParams dockParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dockParams.bottomMargin = Ui.dp(context, 26);
        addView(dock, dockParams);

        updateTabStyles();
    }

    private View roundButton(Context context, android.graphics.drawable.Drawable glyph, final Runnable action) {
        ImageView button = new ImageView(context);
        int pad = Ui.dp(context, 11);
        button.setPadding(pad, pad, pad, pad);
        button.setImageDrawable(glyph);
        button.setFocusable(true);
        button.setClickable(true);
        button.setDefaultFocusHighlightEnabled(false);
        button.setBackground(Ui.focusGlass(context, Ui.dp(context, 21), true, prefs.effects()));
        button.setOnClickListener(view -> action.run());
        button.setOnKeyListener((view, keyCode, event) -> headerKey(keyCode, event));
        button.setOnFocusChangeListener((view, hasFocus) -> {
            float scale = hasFocus ? 1.1f : 1f;
            view.animate().scaleX(scale).scaleY(scale).setDuration(140).start();
        });
        return button;
    }

    // --- lifecycle --------------------------------------------------------------------------------------

    void onShown() {
        applyClock();
        updateGreeting();
        panel.onShown();
        AppRepository.loadAsync(getContext(), true, apps -> {
            if (!isAttachedToWindow()) return;
            onAppsLoaded(apps);
        });
        if (prefs.hasCity()) {
            Weather.load(getContext(), false, result -> {
                if (!isAttachedToWindow() || result.forecast == null) return;
                forecast = result.forecast;
                renderedDock = "";
                rebuildDock(-1);
            });
        }
    }

    void onHidden() {
        handler.removeCallbacks(applySelection);
        handler.removeCallbacks(applyTab);
        panel.onHidden();
    }

    void tick() {
        updateGreeting();
    }

    void onLookChanged() {
        panel.onLookChanged();
    }

    // Hooks for the layout test: the three zones, and a list of apps without a PackageManager.
    View titleView() {
        return title;
    }

    View panelView() {
        return panel;
    }

    View dockView() {
        return dock;
    }

    void showApps(List<AppEntry> apps) {
        onAppsLoaded(apps);
    }

    /** The permission question was answered: the panel looks again. */
    void onPermissionResult() {
        panel.onPermissionResult();
    }

    /** The Home key again, or a return from an app: back to the dock. */
    void goHome() {
        focusDock();
    }

    /** True when the key was used here. Called by the activity for Menu. */
    boolean menuKey(View focused) {
        return focused instanceof BannerTile && focused.isLongClickable() && focused.performLongClick();
    }

    boolean hasFocusInside() {
        return findFocus() != null;
    }

    /** Back: from the panel or the header the focus goes down to the dock; on the dock nothing happens. */
    void back() {
        if (NavLogic.onBack(zoneOf(findFocus())) == NavLogic.BackAction.FOCUS_DOCK) focusDock();
    }

    // --- header -----------------------------------------------------------------------------------------

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
        title.setText(Greeting.compose(getContext().getString(text), prefs.name()));
    }

    private void chooseTab(int index) {
        selectedTab = index;
        panel.selectTab(index);
        updateTabStyles();
    }

    private void updateTabStyles() {
        boolean usable = panel == null || panel.usesTabs();
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setTextColor(i == selectedTab ? Palette.TEXT : Palette.TEXT_DIM);
            tabs[i].setTypeface(i == selectedTab ? Ui.medium(getContext()) : Ui.regular(getContext()));
            tabs[i].setFocusable(usable);
            tabs[i].setAlpha(usable ? 1f : 0.35f);
        }
    }

    private boolean headerKey(int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) return true;
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (!panel.enter()) focusDock();
            return true;
        }
        return false;
    }

    // --- dock -------------------------------------------------------------------------------------------

    private void onAppsLoaded(List<AppEntry> apps) {
        labels.clear();
        for (AppEntry app : apps) labels.put(app.packageName, app.label);
        if (!prefs.dockConfigured()) prefs.saveDock(DockDefaults.pick(new HashSet<>(labels.keySet())));
        String source = SourceApp.resolve(prefs.sourcePackage(), apps);
        panel.setSource(source, source == null ? "" : labels.get(source));
        loadedApps = true;
        rebuildDock(-1);
        if (findFocus() == null) focusDock();
    }

    private void rebuildDock(int focusOverride) {
        if (!loadedApps) return;
        List<String> shown = prefs.dock().visible(labels.keySet());
        String key = shown + "|" + prefs.accent().id + "|" + prefs.effects().id + "|" + Palette.light
                + "|" + (forecast == null ? "" : forecast.fetchedAt + ":" + forecast.code) + "|" + prefs.cityName();
        if (key.equals(renderedDock) && focusOverride < 0) return;
        renderedDock = key;

        View focused = dock.getFocusedChild();
        int focusIndex = focusOverride >= 0 ? focusOverride : (focused == null ? -1 : dock.indexOfChild(focused));
        dock.removeAllViews();

        Context context = getContext();
        int count = shown.size() + 1;
        int gap = Ui.dp(context, 8);
        int available = getResources().getDisplayMetrics().widthPixels - Ui.dp(context, 2 * 40 + 2 * 14);
        int width = Math.min(Ui.dp(context, 128), available / count - 2 * gap);
        int height = width * 9 / 16;
        PackageManager pm = context.getPackageManager();

        for (final String packageName : shown) {
            BannerTile tile = new BannerTile(context, packageName, labels.get(packageName), width, height, false);
            IconCache.get().loadBanner(pm, packageName, labels.get(packageName), width, height, tile.picture());
            tile.setOnClickListener(view -> open(packageName));
            tile.setOnLongClickListener(view -> {
                showDockMenu(packageName);
                return true;
            });
            addTile(tile, gap);
        }

        BannerTile weatherTile = new BannerTile(context, null, context.getString(R.string.tab_weather), width, height, false);
        weatherTile.picture().setImageBitmap(WeatherTileArt.render(context, width, height, forecast,
                context.getString(R.string.tab_weather)));
        weatherTile.setOnClickListener(view -> {
            if (!prefs.hasCity()) host.openSettings();
        });
        addTile(weatherTile, gap);

        if (focusIndex >= 0) dock.getChildAt(Math.min(focusIndex, dock.getChildCount() - 1)).requestFocus();
    }

    private void addTile(final BannerTile tile, int gap) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(tile.getLayoutParams().width, tile.getLayoutParams().height);
        params.setMargins(gap, 0, gap, 0);
        tile.setOnKeyListener((view, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            int at = dock.indexOfChild(view);
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (!panel.enter()) focusHeader();
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    // Down from the dock opens the list of all apps, as wide banners.
                    host.openLibrary();
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
                lastDockIndex = dock.indexOfChild(view);
                lightUnder(tile);
                pendingTile = tile;
                handler.removeCallbacks(applySelection);
                handler.postDelayed(applySelection, SELECT_DELAY_MS);
            } else if (!isInDock(findFocus())) {
                glow.fadeOut();
            }
        });
        dock.addView(tile, params);
    }

    /** The light along the bottom takes the colour of the selected app and is brightest under it. */
    private void lightUnder(final BannerTile tile) {
        if (tile.getWidth() == 0) {
            // Not laid out yet (first focus): try again once it is.
            tile.post(() -> {
                if (tile.hasFocus()) lightUnder(tile);
            });
            return;
        }
        int color = tile.packageName == null ? 0 : IconCache.get().colorOf(tile.packageName, 0);
        if (color == 0) color = prefs.accent().color;
        int[] where = new int[2];
        tile.getLocationInWindow(where);
        glow.aim(where[0] + tile.getWidth() / 2f, color);
    }

    /** Tells the panel what the dock selection is, once the focus has rested on it for a moment. */
    private void select(BannerTile tile) {
        panel.select(new ContentPanel.Selection(tile.packageName == null
                ? ContentPanel.Selection.Kind.WEATHER : ContentPanel.Selection.Kind.SOURCE));
        updateTabStyles();
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

    // --- focus zones ------------------------------------------------------------------------------------

    private boolean isInDock(View view) {
        return view != null && view.getParent() == dock;
    }

    private NavLogic.Zone zoneOf(View view) {
        if (isInDock(view)) return NavLogic.Zone.DOCK;
        for (TextView tab : tabs) {
            if (tab == view) return NavLogic.Zone.HEADER;
        }
        if (view == settingsButton) return NavLogic.Zone.HEADER;
        return NavLogic.Zone.CONTENT;
    }

    @Override
    public void focusHeader() {
        if (panel.usesTabs()) tabs[selectedTab].requestFocus();
        else settingsButton.requestFocus();
    }

    @Override
    public void focusDock() {
        int count = dock.getChildCount();
        if (count == 0) return;
        dock.getChildAt(Math.max(0, Math.min(lastDockIndex, count - 1))).requestFocus();
    }

    @Override
    public void openSettings() {
        host.openSettings();
    }

    @Override
    public void requestTvPermission() {
        host.requestTvPermission();
    }
}
