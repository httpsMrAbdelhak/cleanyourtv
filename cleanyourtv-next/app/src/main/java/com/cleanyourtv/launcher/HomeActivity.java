package com.cleanyourtv.launcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

/**
 * The home screen host: the wallpaper under one HomeScreen. The theme follows the wallpaper (a pale one
 * gives dark text on white glass); when the wallpaper changes the theme may flip, and the screen is rebuilt.
 */
public final class HomeActivity extends Activity implements HomeScreen.Host, WallpaperView.SlowListener {
    private static final int GREETING_REFRESH_MS = 60_000;

    private Prefs prefs;
    private FrameLayout root;
    private WallpaperView wallpaper;
    private HomeScreen screen;
    private boolean builtLight;
    private String lookSignature = "";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable minuteTick = new Runnable() {
        @Override public void run() {
            screen.tick();
            handler.postDelayed(this, GREETING_REFRESH_MS);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new Prefs(this);
        Palette.apply(prefs);
        builtLight = Palette.light;
        buildUi();
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Palette.apply(prefs) || builtLight != Palette.light) {
            // The wallpaper was changed to one of the other brightness: every colour is rebuilt.
            recreate();
            return;
        }
        applyLook();
        handler.post(minuteTick);
        screen.onShown();
        wallpaper.startAnimation();
        // Back from an app, the focus may be gone: give it to the dock.
        root.post(() -> {
            if (!screen.hasFocusInside()) screen.goHome();
        });
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(minuteTick);
        wallpaper.stopAnimation();
        if (screen != null) screen.onHidden();
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Home pressed while already here: back to the dock.
        screen.goHome();
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration config) {
        super.onConfigurationChanged(config);
        applyLook();
        screen.onLookChanged();
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

        screen = new HomeScreen(this, this, prefs);
        root.addView(screen, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
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
            boolean changed = !lookSignature.isEmpty();
            lookSignature = signature;
            Backdrop.setScreen(getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
            Backdrop.rebuild(this, prefs, root);
            if (changed) screen.onLookChanged();
        }
    }

    // --- host -------------------------------------------------------------------------------------------

    @Override
    public void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    public void openLibrary() {
        startActivity(new Intent(this, LibraryActivity.class));
    }

    @Override
    public void openDetail(MediaItem item, String packageName, String appLabel) {
        startActivity(DetailActivity.intent(this, item, packageName, appLabel));
    }

    @Override
    public void onTooSlow() {
        // The TV could not keep the animation going: go back to the still picture for good.
        prefs.setAnimatedWallpaper(false);
        Toast.makeText(this, R.string.toast_animation_off, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        screen.back();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && screen.menuKey(getCurrentFocus())) return true;
        return super.onKeyUp(keyCode, event);
    }
}
