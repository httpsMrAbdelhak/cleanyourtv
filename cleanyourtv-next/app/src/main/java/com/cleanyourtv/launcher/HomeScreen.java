package com.cleanyourtv.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The home screen. From top to bottom: the top bar (sections, search, buttons, weather and time), a bold
 * greeting, the chips Explore, Movies, Series, a shelf of posters from the streaming app (the selected one
 * widens), and a liquid-glass dock of app banners with the ambient light along the bottom of the screen.
 * Library shows every row of the app one under the other; My list shows its watchlist. Down on the dock opens
 * the app drawer. Back from anywhere goes down to the dock; on the dock it does nothing.
 */
final class HomeScreen extends FrameLayout implements TopBar.Listener, CardRail.Listener, WeatherPanel.Listener,
        LibraryShelf.Host, MessageBox.Keys {
    /** What the screen asks of the activity that hosts it. */
    interface Host {
        void openSettings();

        /** mode: LibraryActivity.MODE_ALL, MODE_GAMES or MODE_SEARCH. */
        void openDrawer(int mode);

        void requestTvPermission();

        /** The theme or the effects level changed: rebuild the screen. */
        void rebuild();
    }

    private static final long SETTLE_MS = 220;

    private final Host host;
    private final Prefs prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AmbientGlow glow;
    private final TopBar top;
    private final TextView greeting;
    private final LinearLayout chipRow;
    private final TextView[] chips = new TextView[3];
    private final FrameLayout homeArea;
    private final CardRail rail;
    private final MessageBox message;
    private final WeatherPanel weather;
    private final LibraryShelf shelf;
    private final LinearLayout dock;
    private final List<BannerTile> tiles = new ArrayList<>();
    private final Map<String, String> labels = new HashMap<>();

    private final Runnable applyChip = new Runnable() {
        @Override public void run() {
            chooseChip(pendingChip);
        }
    };
    private final Runnable applySection = new Runnable() {
        @Override public void run() {
            showPage(pendingSection);
        }
    };

    private int pendingChip;
    private int chip = EntryFilter.EXPLORE;
    private int pendingSection;
    private int page = TopBar.HOME;
    private boolean weatherShown;
    private int lastDockIndex;
    private String renderedDock = "";
    private boolean loadedApps;
    private boolean sourceKnown;
    private String sourcePackage;
    private String sourceLabel = "";
    private TvChannels.Result content;
    private int token;

    HomeScreen(Context context, Host host, Prefs prefs) {
        super(context);
        this.host = host;
        this.prefs = prefs;
        setClipChildren(false);
        int side = Ui.dp(context, 56);
        int height = getResources().getDisplayMetrics().heightPixels;

        glow = new AmbientGlow(context);
        glow.setStrength(Palette.light ? 0.6f : 0.85f);
        addView(glow, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height * 50 / 100, Gravity.BOTTOM));

        top = new TopBar(context, prefs, this);
        LayoutParams topParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP);
        topParams.setMargins(Ui.dp(context, 48), Ui.dp(context, 24), Ui.dp(context, 48), 0);
        addView(top, topParams);

        greeting = Ui.text(context, 30, Palette.TEXT, Ui.semibold(context));
        LayoutParams greetingParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        greetingParams.setMargins(side, Ui.dp(context, 74), 0, 0);
        addView(greeting, greetingParams);

        chipRow = new LinearLayout(context);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        chipRow.setClipChildren(false);
        int[] chipNames = {R.string.tab_explore, R.string.tab_movies, R.string.tab_series};
        for (int i = 0; i < chips.length; i++) {
            final int index = i;
            TextView view = Ui.text(context, 15, Palette.TEXT, Ui.medium(context));
            view.setGravity(Gravity.CENTER);
            view.setPadding(Ui.dp(context, 14), Ui.dp(context, 5), Ui.dp(context, 14), Ui.dp(context, 5));
            view.setFocusable(true);
            view.setClickable(true);
            view.setDefaultFocusHighlightEnabled(false);
            view.setTag(context.getString(chipNames[i]));
            view.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) return;
                pendingChip = index;
                handler.removeCallbacks(applyChip);
                handler.postDelayed(applyChip, SETTLE_MS);
            });
            view.setOnClickListener(v -> chooseChip(index));
            view.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_UP:
                        top.focusSelected();
                        return true;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        if (!enterHomeArea()) focusDock();
                        return true;
                    case KeyEvent.KEYCODE_DPAD_LEFT: return index == 0;
                    case KeyEvent.KEYCODE_DPAD_RIGHT: return index == chips.length - 1;
                    default: return false;
                }
            });
            chips[i] = view;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.rightMargin = Ui.dp(context, 8);
            chipRow.addView(view, params);
        }
        LayoutParams chipParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        chipParams.setMargins(side - Ui.dp(context, 4), Ui.dp(context, 116), 0, 0);
        addView(chipRow, chipParams);

        homeArea = new FrameLayout(context);
        homeArea.setClipChildren(false);
        rail = new CardRail(context, prefs, this);
        homeArea.addView(rail, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        weather = new WeatherPanel(context, prefs, this);
        weather.setVisibility(GONE);
        LayoutParams weatherParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        weatherParams.rightMargin = side;
        homeArea.addView(weather, weatherParams);
        message = new MessageBox(context, prefs, this);
        message.setVisibility(GONE);
        homeArea.addView(message, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP));
        LayoutParams areaParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(context, 236), Gravity.TOP);
        areaParams.setMargins(side - Ui.dp(context, 4), Ui.dp(context, 150), 0, 0);
        addView(homeArea, areaParams);

        shelf = new LibraryShelf(context, prefs, this);
        shelf.setVisibility(GONE);
        LayoutParams shelfParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                height - Ui.dp(context, 118) - Ui.dp(context, 150), Gravity.TOP);
        shelfParams.setMargins(side - Ui.dp(context, 4), Ui.dp(context, 118), 0, 0);
        addView(shelf, shelfParams);

        // The dock: liquid glass, "Favourite apps" and the banners.
        dock = new LinearLayout(context);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER_VERTICAL);
        dock.setClipChildren(false);
        dock.setClipToPadding(false);
        int pad = Ui.dp(context, 12);
        dock.setPadding(Ui.dp(context, 22), pad, pad, pad);
        dock.setBackground(new GlassDrawable(Ui.dp(context, 26), Ui.dp(context, 1.6f), true, false, true, prefs.effects()));
        LayoutParams dockParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        dockParams.setMargins(Ui.dp(context, 48), 0, Ui.dp(context, 48), Ui.dp(context, 34));
        addView(dock, dockParams);

        ImageView chevron = new ImageView(context);
        chevron.setImageDrawable(Glyphs.chevronDown(Palette.TEXT_DIM));
        LayoutParams chevronParams = new LayoutParams(Ui.dp(context, 18), Ui.dp(context, 18), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        chevronParams.bottomMargin = Ui.dp(context, 10);
        addView(chevron, chevronParams);

        updateChips();
        showPage(TopBar.HOME);
    }

    // --- lifecycle --------------------------------------------------------------------------------------

    void onShown() {
        updateGreeting();
        AppRepository.loadAsync(getContext(), true, apps -> {
            if (!isAttachedToWindow()) return;
            onAppsLoaded(apps);
        });
        if (prefs.hasCity()) {
            Weather.load(getContext(), false, result -> {
                if (!isAttachedToWindow()) return;
                top.setWeather(result.forecast);
            });
        }
        loadContent(false);
    }

    void onHidden() {
        handler.removeCallbacks(applyChip);
        handler.removeCallbacks(applySection);
        token++;
        rail.releaseImages();
        shelf.releaseImages();
    }

    void tick() {
        updateGreeting();
    }

    void onLookChanged() {
        rail.refreshImages();
    }

    void onPermissionResult() {
        loadContent(true);
    }

    /** The Home key again, or a return from an app: back to the dock. */
    void goHome() {
        focusDock();
    }

    boolean menuKey(View focused) {
        return focused instanceof BannerTile && focused.isLongClickable() && focused.performLongClick();
    }

    boolean hasFocusInside() {
        return findFocus() != null;
    }

    /** Back: from anywhere but the dock, to the dock. */
    void back() {
        if (NavLogic.onBack(zoneOf(findFocus())) == NavLogic.BackAction.FOCUS_DOCK) focusDock();
    }

    // Hooks for the layout test: the zones, and a list of apps without a PackageManager.
    View titleView() {
        return greeting;
    }

    View panelView() {
        return homeArea;
    }

    View dockView() {
        return dock;
    }

    void showApps(List<AppEntry> apps) {
        onAppsLoaded(apps);
    }

    // --- pages ------------------------------------------------------------------------------------------

    private void showPage(int section) {
        page = section;
        top.select(section);
        weatherShown = false;
        boolean home = section == TopBar.HOME;
        chipRow.setVisibility(home ? VISIBLE : GONE);
        homeArea.setVisibility(section == TopBar.LIBRARY ? GONE : VISIBLE);
        shelf.setVisibility(section == TopBar.LIBRARY ? VISIBLE : GONE);
        updateGreeting();
        render();
    }

    private void updateGreeting() {
        if (page == TopBar.LIBRARY) {
            greeting.setText(R.string.nav_library);
            return;
        }
        if (page == TopBar.MY_LIST) {
            greeting.setText(R.string.nav_my_list);
            return;
        }
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int text;
        switch (Greeting.forHour(hour)) {
            case MORNING: text = R.string.greeting_morning; break;
            case AFTERNOON: text = R.string.greeting_afternoon; break;
            default: text = R.string.greeting_evening; break;
        }
        greeting.setText(Greeting.compose(getContext().getString(text), prefs.name()).replace(", ", " "));
    }

    private void chooseChip(int index) {
        chip = index;
        updateChips();
        if (page == TopBar.HOME) {
            weatherShown = false;
            render();
        }
    }

    /** The selected chip is a white pill with a tick; the others are outlined. */
    private void updateChips() {
        Context context = getContext();
        for (int i = 0; i < chips.length; i++) {
            boolean on = i == chip;
            float radius = Ui.dp(context, 15);
            android.graphics.drawable.GradientDrawable rest = Ui.round(on ? 0xF2FFFFFF : 0x00000000, radius);
            rest.setStroke(Ui.dp(context, 1.5f), on ? 0xF2FFFFFF : (Palette.light ? 0x66000000 : 0x8CFFFFFF));
            android.graphics.drawable.GradientDrawable lit = Ui.round(on ? 0xFFFFFFFF : Palette.TILE_FOCUSED, radius);
            lit.setStroke(Ui.dp(context, 2.5f), Palette.RING);
            android.graphics.drawable.StateListDrawable states = new android.graphics.drawable.StateListDrawable();
            states.addState(new int[] {android.R.attr.state_focused}, lit);
            states.addState(new int[] {}, rest);
            chips[i].setBackground(states);
            chips[i].setText((on ? "✓  " : "") + chips[i].getTag());
            chips[i].setTextColor(on ? 0xFF1A1D26 : Palette.TEXT);
            chips[i].setTypeface(on ? Ui.semibold(context) : Ui.medium(context));
        }
    }

    // --- content ----------------------------------------------------------------------------------------

    private void loadContent(boolean force) {
        if (!sourceKnown) {
            rail.showLoading();
            return;
        }
        if (content != null && !force && content.status == TvChannels.Status.OK) {
            render();
        } else if (content == null) {
            rail.showLoading();
        }
        final int mine = ++token;
        Context context = getContext();
        TvChannels.Labels names = new TvChannels.Labels(context.getString(R.string.section_continue),
                context.getString(R.string.section_next), context.getString(R.string.section_new),
                context.getString(R.string.nav_my_list));
        TvChannels.load(context, sourcePackage, names, result -> {
            if (mine != token) return;
            content = result;
            render();
        });
    }

    /** Shows the current page from the last answer, or the message that explains why there is nothing. */
    private void render() {
        if (weatherShown) return;
        weather.setVisibility(GONE);
        if (content == null) return;
        Context context = getContext();
        switch (content.status) {
            case NO_SOURCE:
                showMessage(context.getString(R.string.source_none), context.getString(R.string.action_open_settings),
                        () -> host.openSettings());
                return;
            case NO_PERMISSION:
                showMessage(context.getString(R.string.source_no_permission, sourceLabel), context.getString(R.string.action_allow),
                        () -> host.requestTvPermission());
                return;
            case NOTHING:
                showMessage(context.getString(R.string.source_nothing, sourceLabel), context.getString(R.string.action_retry),
                        () -> loadContent(true));
                return;
            case FAILED:
                showMessage(context.getString(R.string.source_failed, sourceLabel), context.getString(R.string.action_retry),
                        () -> loadContent(true));
                return;
            default:
                break;
        }
        if (page == TopBar.LIBRARY) {
            message.setVisibility(GONE);
            shelf.setVisibility(VISIBLE);
            shelf.setSections(content.sections);
            return;
        }
        List<TvEntry> entries = page == TopBar.MY_LIST ? EntryFilter.oneCardPerTitle(content.watchlist)
                : EntryFilter.apply(content.home, chip);
        if (entries.isEmpty()) {
            int text = page == TopBar.MY_LIST ? R.string.my_list_empty : R.string.source_tab_empty;
            showMessage(context.getString(text, sourceLabel), context.getString(R.string.action_retry), () -> loadContent(true));
            return;
        }
        message.setVisibility(GONE);
        rail.setVisibility(VISIBLE);
        rail.setEntries(entries);
    }

    private void showMessage(String text, String action, Runnable onAction) {
        if (page == TopBar.LIBRARY) {
            shelf.setVisibility(GONE);
            homeArea.setVisibility(VISIBLE);
        }
        rail.setVisibility(GONE);
        weather.setVisibility(GONE);
        message.show(text, action, onAction);
    }

    private boolean enterHomeArea() {
        if (page == TopBar.LIBRARY && shelf.getVisibility() == VISIBLE) return shelf.enter();
        if (message.getVisibility() == VISIBLE) return message.focusButton();
        if (weather.getVisibility() == VISIBLE) return weather.focusCard();
        return rail.getVisibility() == VISIBLE && rail.focusFirst();
    }

    /** OK on a card: the app's own intent for it, only if it stays inside that app; else the app itself. */
    @Override
    public void onOpen(TvEntry entry) {
        Context context = getContext();
        Intent intent = null;
        if (!entry.intentUri.isEmpty()) {
            try {
                intent = Intent.parseUri(entry.intentUri, Intent.URI_INTENT_SCHEME);
            } catch (URISyntaxException e) {
                intent = null;
            }
        }
        if (intent != null) {
            String component = intent.getComponent() == null ? null : intent.getComponent().getPackageName();
            if (IntentGuard.allowed(sourcePackage, component, intent.getPackage()) && intent.getSelector() == null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(intent);
                    return;
                } catch (RuntimeException e) {
                    // fall through: open the app itself
                }
            }
        }
        if (sourcePackage == null || !AppRepository.launch(context, sourcePackage)) {
            Toast.makeText(context, R.string.toast_launch_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /** "Resume · S38E01 · 20 min left · ends at 17:58", or "Series · S01E02 · 2021". */
    @Override
    public String detail(TvEntry entry) {
        Context context = getContext();
        if (entry.resume) {
            long minutes = CardText.minutesLeft(entry.remainingMs);
            String left = minutes > 0 ? context.getString(R.string.meta_left, minutes) : "";
            String ends = minutes > 0 ? context.getString(R.string.meta_ends, android.text.format.DateFormat.getTimeFormat(context)
                    .format(new java.util.Date(System.currentTimeMillis() + entry.remainingMs))) : "";
            return CardText.join(context.getString(R.string.meta_resume), entry.badge, left, ends);
        }
        String kind = entry.kind == TvEntry.Kind.MOVIE ? context.getString(R.string.kind_movie)
                : entry.kind == TvEntry.Kind.SERIES ? context.getString(R.string.kind_series) : "";
        return CardText.join(kind, entry.badge, entry.year);
    }

    @Override
    public boolean onUp() {
        if (page == TopBar.HOME) chips[chip].requestFocus();
        else top.focusSelected();
        return true;
    }

    @Override
    public boolean onDown() {
        focusDock();
        return true;
    }

    @Override
    public void onEntered(CardRail which) {
        // The home rail is always in view: nothing to scroll.
    }

    @Override
    public void onRefresh() {
        Weather.load(getContext(), true, result -> {
            if (!isAttachedToWindow() || result.forecast == null) return;
            top.setWeather(result.forecast);
            weather.show(result.forecast, result.stale, prefs.cityName());
        });
    }

    @Override
    public void focusTopBar() {
        top.focusSelected();
    }

    // --- top bar ----------------------------------------------------------------------------------------

    @Override
    public void onSectionFocused(int section) {
        pendingSection = section;
        handler.removeCallbacks(applySection);
        handler.postDelayed(applySection, SETTLE_MS);
    }

    @Override
    public void onSectionChosen(int section) {
        if (section == TopBar.APPS) host.openDrawer(LibraryActivity.MODE_ALL);
        else if (section == TopBar.GAMES) host.openDrawer(LibraryActivity.MODE_GAMES);
        else {
            handler.removeCallbacks(applySection);
            showPage(section);
            if (!enterHomeArea()) focusDock();
        }
    }

    @Override
    public void onSearch() {
        host.openDrawer(LibraryActivity.MODE_SEARCH);
    }

    /** The moon: switches between the dark and the light look (a dark or a light wallpaper). */
    @Override
    public void onTheme() {
        prefs.setWallpaperId(Palette.light ? Wallpapers.DEFAULT_DARK : Wallpapers.DEFAULT_LIGHT);
        host.rebuild();
    }

    /** The bolt: switches between the Quality and the Performance effects. */
    @Override
    public void onEffects() {
        Effects next = prefs.effects() == Effects.QUALITY ? Effects.PERFORMANCE : Effects.QUALITY;
        prefs.setEffects(next);
        if (!next.allowsAnimatedWallpaper()) prefs.setAnimatedWallpaper(false);
        Toast.makeText(getContext(), next == Effects.QUALITY ? R.string.effects_quality : R.string.effects_performance,
                Toast.LENGTH_SHORT).show();
        host.rebuild();
    }

    @Override
    public void onSettings() {
        host.openSettings();
    }

    /** The weather pill: the weather card in place of the shelf, until another chip or section is chosen. */
    @Override
    public void onWeather() {
        if (!prefs.hasCity()) {
            host.openSettings();
            return;
        }
        if (page != TopBar.HOME) showPage(TopBar.HOME);
        weatherShown = true;
        rail.setVisibility(GONE);
        message.setVisibility(GONE);
        weather.setVisibility(INVISIBLE);
        Weather.load(getContext(), false, result -> {
            if (!isAttachedToWindow() || !weatherShown) return;
            if (result.forecast == null) {
                weatherShown = false;
                showMessage(getContext().getString(result.error == Weather.Error.OFFLINE ? R.string.weather_offline
                        : R.string.weather_failed), getContext().getString(R.string.action_retry), this::onWeather);
                return;
            }
            weather.setVisibility(VISIBLE);
            weather.show(result.forecast, result.stale, prefs.cityName());
            top.setWeather(result.forecast);
            weather.focusCard();
        });
    }

    @Override
    public void onLeaveDown() {
        if (page == TopBar.HOME && chipRow.getVisibility() == VISIBLE) chips[chip].requestFocus();
        else if (!enterHomeArea()) focusDock();
    }

    // --- dock -------------------------------------------------------------------------------------------

    private void onAppsLoaded(List<AppEntry> apps) {
        labels.clear();
        for (AppEntry app : apps) labels.put(app.packageName, app.label);
        if (!prefs.dockConfigured()) prefs.saveDock(DockDefaults.pick(new HashSet<>(labels.keySet())));
        String source = SourceApp.resolve(prefs.sourcePackage(), apps);
        boolean changed = !sourceKnown || (source == null ? sourcePackage != null : !source.equals(sourcePackage));
        sourceKnown = true;
        sourcePackage = source;
        sourceLabel = source == null ? "" : labels.get(source);
        loadedApps = true;
        rebuildDock(-1);
        if (changed) {
            content = null;
            loadContent(true);
        }
        if (findFocus() == null) focusDock();
    }

    private void rebuildDock(int focusOverride) {
        if (!loadedApps) return;
        List<String> shown = prefs.dock().visible(labels.keySet());
        String key = shown + "|" + prefs.accent().id + "|" + prefs.effects().id + "|" + Palette.light;
        if (key.equals(renderedDock) && focusOverride < 0) return;
        renderedDock = key;

        View focused = dock.getFocusedChild();
        int focusIndex = focusOverride >= 0 ? focusOverride : (focused == null ? -1 : tiles.indexOf(focused));
        dock.removeAllViews();
        tiles.clear();

        Context context = getContext();
        TextView label = Ui.text(context, 15, Palette.TEXT_DIM, Ui.medium(context));
        label.setText(R.string.dock_label);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.rightMargin = Ui.dp(context, 14);
        dock.addView(label, labelParams);

        int count = Math.max(1, shown.size());
        int gap = Ui.dp(context, 7);
        int available = getResources().getDisplayMetrics().widthPixels - Ui.dp(context, 2 * 48 + 22 + 12 + 130);
        int width = Math.min(Ui.dp(context, 118), available / count - 2 * gap);
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
        if (focusIndex >= 0 && !tiles.isEmpty()) tiles.get(Math.min(focusIndex, tiles.size() - 1)).requestFocus();
    }

    private void addTile(final BannerTile tile, int gap) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(tile.getLayoutParams().width, tile.getLayoutParams().height);
        params.setMargins(gap, 0, gap, 0);
        tile.setOnKeyListener((view, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            int at = tiles.indexOf(view);
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (!enterHomeArea()) {
                        if (page == TopBar.HOME) chips[chip].requestFocus();
                        else top.focusSelected();
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    // Down from the dock opens the drawer of all apps, as wide banners.
                    host.openDrawer(LibraryActivity.MODE_ALL);
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    return at == 0;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    return at == tiles.size() - 1;
                default:
                    return false;
            }
        });
        final View.OnFocusChangeListener lift = tile.getOnFocusChangeListener();
        tile.setOnFocusChangeListener((view, hasFocus) -> {
            lift.onFocusChange(view, hasFocus);
            if (hasFocus) {
                lastDockIndex = tiles.indexOf(view);
                lightUnder(tile);
            } else if (!(findFocus() instanceof BannerTile)) {
                glow.fadeOut();
            }
        });
        tiles.add(tile);
        dock.addView(tile, params);
    }

    /** The light along the bottom takes the colour of the selected app and is brightest under it. */
    private void lightUnder(final BannerTile tile) {
        if (tile.getWidth() == 0) {
            // Not laid out yet: wait for its layout once, rather than re-posting until it has a width.
            tile.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override public void onLayoutChange(View view, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                    view.removeOnLayoutChangeListener(this);
                    if (view.getWidth() > 0 && view.hasFocus()) lightUnder(tile);
                }
            });
            return;
        }
        int color = tile.packageName == null ? 0 : IconCache.get().colorOf(tile.packageName, 0);
        if (color == 0) color = prefs.accent().color;
        int[] where = new int[2];
        tile.getLocationInWindow(where);
        glow.aim(where[0] + tile.getWidth() / 2f, color);
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

    private NavLogic.Zone zoneOf(View view) {
        if (view instanceof BannerTile && tiles.contains(view)) return NavLogic.Zone.DOCK;
        if (top.contains(view)) return NavLogic.Zone.TOP;
        for (TextView c : chips) {
            if (c == view) return NavLogic.Zone.CHIPS;
        }
        return NavLogic.Zone.CONTENT;
    }

    @Override
    public void focusDock() {
        if (tiles.isEmpty()) return;
        tiles.get(Math.max(0, Math.min(lastDockIndex, tiles.size() - 1))).requestFocus();
    }
}
