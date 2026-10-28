package com.cleanyourtv.launcher;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The app drawer: every installed app as a wide banner, the same look as the dock. It rises when Down is pressed
 * on the dock, and Up on its first row sinks it back to the dock (Back does too). It also opens as Games (apps
 * that declare themselves games) and as Search (a field on top filters the banners as you type). OK opens an app;
 * a long OK (or Menu) adds it to or removes it from the dock. The focused banner lights a pane of its colour.
 */
public final class LibraryActivity extends Activity {
    static final String EXTRA_MODE = "mode";
    static final int MODE_ALL = 0;
    static final int MODE_GAMES = 1;
    static final int MODE_SEARCH = 2;
    private static final int COLUMNS = 5;

    private Prefs prefs;
    private boolean builtLight;
    private int mode;
    private LinearLayout content;
    private TextView title;
    private EditText search;
    private List<AppEntry> apps = new ArrayList<>();
    private final List<View> firstRow = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable filter = new Runnable() {
        @Override public void run() {
            populate();
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new Prefs(this);
        Palette.apply(prefs);
        builtLight = Palette.light;
        mode = getIntent().getIntExtra(EXTRA_MODE, MODE_ALL);
        getWindow().setBackgroundDrawable(Wallpaper.load(this, prefs));

        int margin = Ui.dp(this, 48);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(margin, Ui.dp(this, 30), margin, 0);
        page.setBackgroundColor((Palette.BACKGROUND & 0xFFFFFF) | 0x66000000);

        title = Ui.text(this, 30, Palette.TEXT, Ui.semibold(this));
        title.setText(mode == MODE_GAMES ? R.string.nav_games : mode == MODE_SEARCH ? R.string.search_title : R.string.library_title);
        page.addView(title);

        if (mode == MODE_SEARCH) {
            search = new EditText(this);
            search.setSingleLine(true);
            search.setHint(R.string.search_hint);
            search.setTextColor(Palette.TEXT);
            search.setHintTextColor(Palette.TEXT_DIM);
            search.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 20 * Ui.unit(this));
            search.setTypeface(Ui.regular(this));
            search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
            int pad = Ui.dp(this, 14);
            search.setPadding(pad * 2, pad, pad * 2, pad);
            search.setBackground(Ui.focusGlass(this, Palette.TILE, Palette.TILE_FOCUSED, Ui.dp(this, 24)));
            search.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

                @Override public void afterTextChanged(Editable s) {
                    handler.removeCallbacks(filter);
                    handler.postDelayed(filter, 150);
                }
            });
            search.setOnKeyListener((view, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    close();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && !firstRow.isEmpty()) return firstRow.get(0).requestFocus();
                return false;
            });
            LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(Ui.dp(this, 520), ViewGroup.LayoutParams.WRAP_CONTENT);
            searchParams.topMargin = Ui.dp(this, 12);
            page.addView(search, searchParams);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 40));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        Ui.allowOverflow(content);
        scroll.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(page);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Palette.apply(prefs) || builtLight != Palette.light) {
            recreate();
            return;
        }
        AppRepository.loadAsync(this, prefs.includeNonTvApps(), loaded -> {
            if (isDestroyed() || isFinishing()) return;
            apps = loaded;
            populate();
            if (search != null && getCurrentFocus() == null) search.requestFocus();
        });
    }

    @Override
    public void onBackPressed() {
        close();
    }

    /** Sinks the drawer back into the dock. */
    private void close() {
        finish();
        overridePendingTransition(R.anim.stay, R.anim.drawer_out);
    }

    /** The apps this drawer shows: all, the games, or those whose name contains the search text. */
    static List<AppEntry> select(List<AppEntry> apps, int mode, String query) {
        List<AppEntry> out = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        for (AppEntry app : apps) {
            if (mode == MODE_GAMES && !app.game) continue;
            if (mode == MODE_SEARCH && !q.isEmpty() && !app.label.toLowerCase(Locale.getDefault()).contains(q)) continue;
            out.add(app);
        }
        return out;
    }

    private void populate() {
        content.removeAllViews();
        firstRow.clear();
        List<AppEntry> shown = select(apps, mode, search == null ? "" : search.getText().toString());
        if (mode != MODE_SEARCH) {
            int base = mode == MODE_GAMES ? R.string.nav_games : R.string.library_title;
            title.setText(getString(base) + "  (" + shown.size() + ")");
        }
        if (shown.isEmpty()) {
            TextView empty = Ui.text(this, 20, Palette.TEXT_DIM, Ui.regular(this));
            empty.setText(mode == MODE_GAMES ? R.string.games_empty : mode == MODE_SEARCH ? R.string.search_none : R.string.library_empty);
            content.addView(empty);
            return;
        }

        int gap = Ui.dp(this, 10);
        int available = getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 96);
        int width = available / COLUMNS - 2 * gap;
        int height = width * 9 / 16;
        PackageManager pm = getPackageManager();

        LinearLayout row = null;
        View first = null;
        for (int i = 0; i < shown.size(); i++) {
            if (i % COLUMNS == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.START);
                Ui.allowOverflow(row);
                int padY = Ui.dp(this, 6);
                row.setPadding(0, padY, 0, padY);
                content.addView(row);
            }
            final AppEntry app = shown.get(i);
            BannerTile tile = new BannerTile(this, app.packageName, app.label, width, height, true);
            IconCache.get().loadBanner(pm, app.packageName, app.label, width, height, tile.picture());
            tile.setOnClickListener(view -> {
                if (!AppRepository.launch(this, app.packageName)) {
                    Toast.makeText(this, R.string.toast_launch_failed, Toast.LENGTH_SHORT).show();
                }
            });
            tile.setOnLongClickListener(view -> {
                showMenu(app);
                return true;
            });
            if (i < COLUMNS) {
                firstRow.add(tile);
                // Up on the first row: back to the dock (or to the search field), no need for Back.
                tile.setOnKeyListener((view, keyCode, event) -> {
                    if (event.getAction() != KeyEvent.ACTION_DOWN || keyCode != KeyEvent.KEYCODE_DPAD_UP) return false;
                    if (search != null) return search.requestFocus();
                    close();
                    return true;
                });
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
            params.setMargins(gap, gap, gap, gap);
            row.addView(tile, params);
            if (first == null) first = tile;
        }
        if (search == null && getCurrentFocus() == null && first != null) first.requestFocus();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            View focused = getCurrentFocus();
            if (focused != null && focused.isLongClickable()) return focused.performLongClick();
        }
        return super.onKeyUp(keyCode, event);
    }

    private void showMenu(final AppEntry app) {
        final DockModel dock = prefs.dock();
        ChoiceDialog dialog = new ChoiceDialog(this, app.label);
        if (dock.contains(app.packageName)) {
            dialog.option(getString(R.string.menu_remove_dock), () -> {
                dock.remove(app.packageName);
                prefs.saveDock(dock);
                Toast.makeText(this, R.string.toast_removed, Toast.LENGTH_SHORT).show();
            });
        } else {
            dialog.option(getString(R.string.menu_add_dock), () -> {
                if (dock.add(app.packageName)) {
                    prefs.saveDock(dock);
                    Toast.makeText(this, R.string.toast_added, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.toast_dock_full, DockModel.MAX), Toast.LENGTH_SHORT).show();
                }
            });
        }
        dialog.show();
    }
}
