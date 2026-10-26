package com.cleanyourtv.launcher;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Every installed app, in a grid of tiles. The grid is plain rows of focusable views inside a ScrollView:
 * the framework's own D-pad focus search walks it in all four directions and scrolls to follow, with no
 * custom key handling to get wrong. OK opens an app; a long OK (or Menu) adds it to or removes it from the dock.
 */
public final class LibraryActivity extends Activity {
    private Prefs prefs;
    private boolean builtLight;
    private LinearLayout content;
    private TextView title;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new Prefs(this);
        Palette.apply(prefs);
        builtLight = Palette.light;
        getWindow().setBackgroundDrawable(Wallpaper.load(this, prefs));

        int margin = Ui.dp(this, 48);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(margin, margin, margin, 0);
        page.setBackgroundColor((Palette.BACKGROUND & 0xFFFFFF) | 0x80000000);

        title = Ui.text(this, 36, Palette.TEXT, Ui.light(this));
        title.setText(R.string.library_title);
        page.addView(title);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 32));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        TileView.allowOverflow(content);
        scroll.addView(content, new ViewGroup.LayoutParams(
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
        AppRepository.loadAsync(this, prefs.includeNonTvApps(), apps -> {
            if (isDestroyed() || isFinishing()) return;
            populate(apps);
        });
    }

    private void populate(List<AppEntry> apps) {
        content.removeAllViews();
        title.setText(getString(R.string.library_title) + "  (" + apps.size() + ")");
        if (apps.isEmpty()) {
            TextView empty = Ui.text(this, 20, Palette.TEXT_DIM, Ui.regular(this));
            empty.setText(R.string.library_empty);
            content.addView(empty);
            return;
        }

        int cellWidth = Ui.dp(this, 148);
        int tileHeight = Ui.dp(this, 120);
        int tileWidth = Ui.dp(this, 132);
        int columns = Math.max(3, (getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 96)) / cellWidth);
        int iconPx = Ui.dp(this, 60);
        int gap = Ui.dp(this, 8);
        PackageManager pm = getPackageManager();

        LinearLayout row = null;
        View first = null;
        for (int i = 0; i < apps.size(); i++) {
            if (i % columns == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.START);
                TileView.allowOverflow(row);
                content.addView(row);
            }
            final AppEntry app = apps.get(i);
            TileView tile = new TileView(this, tileWidth, tileHeight, iconPx);
            tile.setLabel(app.label);
            IconCache.get().load(pm, app.packageName, iconPx, tile.iconView());
            tile.setOnClickListener(view -> {
                if (!AppRepository.launch(this, app.packageName)) {
                    Toast.makeText(this, R.string.toast_launch_failed, Toast.LENGTH_SHORT).show();
                }
            });
            tile.setOnLongClickListener(view -> {
                showMenu(app);
                return true;
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(tile.getLayoutParams());
            params.setMargins(gap, gap, gap, gap);
            row.addView(tile, params);
            if (first == null) first = tile;
        }
        if (getCurrentFocus() == null && first != null) first.requestFocus();
    }

    @Override
    public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_MENU) {
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
