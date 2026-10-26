package com.cleanyourtv.launcher;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Every installed app as a wide banner, the same look as the dock. It opens when Down is pressed on the dock.
 * Rows of focusable banners inside a ScrollView: the framework's own D-pad focus search walks them in all four
 * directions and scrolls to follow. OK opens an app; a long OK (or Menu) adds it to or removes it from the dock.
 * The focused banner lifts and lights a pane of glass in its own colour behind it.
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
        page.setPadding(margin, Ui.dp(this, 36), margin, 0);
        page.setBackgroundColor((Palette.BACKGROUND & 0xFFFFFF) | 0x66000000);

        title = Ui.text(this, 32, Palette.TEXT, Ui.light(this));
        title.setText(R.string.library_title);
        page.addView(title);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 40));
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

        int gap = Ui.dp(this, 10);
        int columns = 5;
        int available = getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 96);
        int width = available / columns - 2 * gap;
        int height = width * 9 / 16;
        PackageManager pm = getPackageManager();

        LinearLayout row = null;
        View first = null;
        for (int i = 0; i < apps.size(); i++) {
            if (i % columns == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.START);
                Ui.allowOverflow(row);
                int padY = Ui.dp(this, 6);
                row.setPadding(0, padY, 0, padY);
                content.addView(row);
            }
            final AppEntry app = apps.get(i);
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
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
            params.setMargins(gap, gap, gap, gap);
            row.addView(tile, params);
            if (first == null) first = tile;
        }
        if (getCurrentFocus() == null && first != null) first.requestFocus();
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
