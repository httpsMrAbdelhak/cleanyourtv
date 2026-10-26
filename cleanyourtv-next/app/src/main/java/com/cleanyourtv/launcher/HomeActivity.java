package com.cleanyourtv.launcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

/**
 * The home screen host: the wallpaper, the ambient light, three pages (Home, Cinema, Weather) shown one at
 * a time with a cross-fade, and the tab bar above them. Focus moves like on tvOS: Left and Right along a
 * row, Up to the tab bar, Down into the page. Back climbs to the tab bar, then to Home, then does nothing.
 */
public final class HomeActivity extends Activity implements Page.Host, TabBar.Listener, WallpaperView.SlowListener {
    private static final int GREETING_REFRESH_MS = 60_000;

    private Prefs prefs;
    private FrameLayout root;
    private WallpaperView wallpaper;
    private AmbientGlow glow;
    private TabBar tabs;
    private final Page[] pages = new Page[NavLogic.TAB_COUNT];
    private int current = -1;
    private String lookSignature = "";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable minuteTick = new Runnable() {
        @Override public void run() {
            ((HomePage) pages[NavLogic.HOME]).tick();
            handler.postDelayed(this, GREETING_REFRESH_MS);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new Prefs(this);
        buildUi();
        setContentView(root);
        root.getViewTreeObserver().addOnGlobalFocusChangeListener((oldFocus, newFocus) -> onFocusMoved(newFocus));
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyLook();
        handler.post(minuteTick);
        if (current < 0) showPage(NavLogic.HOME);
        else pages[current].onShown();
        wallpaper.startAnimation();
        // Back from an app, the focus may be gone: give it to the page on screen.
        root.post(() -> {
            if (getCurrentFocus() == null) {
                if (!pages[current].enter()) tabs.focusSelected();
            }
        });
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(minuteTick);
        wallpaper.stopAnimation();
        if (current >= 0) pages[current].onHidden();
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Home pressed while already here: back to the Home tab, on the first dock icon.
        showPage(NavLogic.HOME);
        if (!pages[NavLogic.HOME].enter()) tabs.focusSelected();
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration config) {
        super.onConfigurationChanged(config);
        applyLook();
        for (Page page : pages) page.onLookChanged();
    }

    // --- layout -----------------------------------------------------------------------------------------

    private void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Palette.BACKGROUND);
        root.setClipChildren(false);

        wallpaper = new WallpaperView(this);
        wallpaper.setSlowListener(this);
        root.addView(wallpaper, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        glow = new AmbientGlow(this);
        int height = getResources().getDisplayMetrics().heightPixels;
        root.addView(glow, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height * 55 / 100, Gravity.BOTTOM));

        pages[NavLogic.HOME] = new HomePage(this, this, prefs);
        pages[NavLogic.CINEMA] = new CinemaPage(this, this, prefs);
        pages[NavLogic.WEATHER] = new WeatherPage(this, this, prefs);
        for (Page page : pages) {
            page.setVisibility(View.GONE);
            root.addView(page, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        tabs = new TabBar(this, prefs, this);
        FrameLayout.LayoutParams tabParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        tabParams.topMargin = Ui.dp(this, 30);
        root.addView(tabs, tabParams);
    }

    /** Wallpaper, effects level and animation, from the saved settings. Rebuilds the frosted copy only if they changed. */
    private void applyLook() {
        String signature = prefs.wallpaperId() + "|" + prefs.effects().id + "|"
                + (Wallpapers.CUSTOM.equals(prefs.wallpaperId()) ? WallpaperStore.file(this).lastModified() : 0)
                + "|" + prefs.accent().id;
        wallpaper.setWallpaper(Wallpaper.load(this, prefs));
        wallpaper.setAnimated(Wallpaper.animated(prefs));
        boolean lostBackdrop = prefs.effects().frostedGlass() && Backdrop.bitmap() == null;
        if (!signature.equals(lookSignature) || lostBackdrop) {
            boolean effectsChanged = !lookSignature.isEmpty();
            lookSignature = signature;
            Backdrop.setScreen(getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
            Backdrop.rebuild(this, prefs, root);
            if (effectsChanged) {
                for (Page page : pages) page.onLookChanged();
            }
        }
    }

    // --- pages and focus --------------------------------------------------------------------------------

    private void showPage(int index) {
        if (index == current) return;
        int previous = current;
        current = index;
        tabs.select(index);
        for (int i = 0; i < pages.length; i++) {
            final Page page = pages[i];
            if (i == index) {
                page.animate().cancel();
                page.setVisibility(View.VISIBLE);
                page.setAlpha(previous < 0 ? 1f : 0f);
                if (previous >= 0) page.animate().alpha(1f).setDuration(240).start();
                page.onShown();
            } else if (page.getVisibility() == View.VISIBLE) {
                page.animate().cancel();
                page.animate().alpha(0f).setDuration(140).withEndAction(() -> page.setVisibility(View.GONE)).start();
                page.onHidden();
            }
        }
    }

    private void onFocusMoved(View focus) {
        if (focus instanceof DockTile) {
            DockTile tile = (DockTile) focus;
            int[] where = new int[2];
            tile.getLocationInWindow(where);
            int color = tile.packageName == null ? 0 : IconCache.get().colorOf(tile.packageName, 0);
            if (color == 0) color = prefs.accent().color;
            glow.aim(where[0] + tile.getWidth() / 2f, color);
        } else if (focus != null) {
            glow.fadeOut();
        }
        int tab = tabs.indexOf(focus);
        if (tab >= 0) showPage(tab);
    }

    @Override
    public void onEnterContent() {
        if (!pages[current].enter()) tabs.focusSelected();
    }

    @Override
    public void onSettings() {
        openSettings();
    }

    @Override
    public void focusTabs() {
        tabs.focusSelected();
    }

    @Override
    public void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    public void openLibrary() {
        startActivity(new Intent(this, LibraryActivity.class));
    }

    @Override
    public void openDetail(MediaItem item) {
        startActivity(DetailActivity.intent(this, item));
    }

    @Override
    public void onTooSlow() {
        // The TV could not keep the animation going: go back to the still picture for good.
        prefs.setAnimatedWallpaper(false);
        Toast.makeText(this, R.string.toast_animation_off, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        boolean inTabs = tabs.contains(getCurrentFocus());
        switch (NavLogic.onBack(current, inTabs)) {
            case FOCUS_TABS:
                tabs.focusSelected();
                break;
            case SELECT_HOME:
                showPage(NavLogic.HOME);
                tabs.getChildAt(NavLogic.HOME).requestFocus();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && current == NavLogic.HOME) {
            if (((HomePage) pages[NavLogic.HOME]).menuKey(getCurrentFocus())) return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}
