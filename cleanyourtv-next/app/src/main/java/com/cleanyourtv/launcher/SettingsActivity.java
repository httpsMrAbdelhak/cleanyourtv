package com.cleanyourtv.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
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

/**
 * The settings: a column of rows. OK or Right steps a row to its next value, Left to the previous one.
 * Every change is saved at once and the wallpaper behind the screen follows, so there is no Save button.
 * Nothing here touches the system: the "home screen" row only opens Android's own settings page.
 */
public final class SettingsActivity extends Activity {
    private interface Value { String get(); }

    private interface Step { void apply(int direction); }

    private Prefs prefs;
    private LinearLayout rows;
    private int accent;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new Prefs(this);
        accent = prefs.accent().color;
        getWindow().setBackgroundDrawable(prefs.wallpaper().load(this));

        int margin = Ui.dp(this, 48);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(margin, margin, margin, 0);

        TextView title = Ui.text(this, 34, Palette.TEXT, Ui.light());
        title.setText(R.string.settings_title);
        page.addView(title);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 32));
        rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        TileView.allowOverflow(rows);
        scroll.addView(rows, new FrameLayout.LayoutParams(
                Ui.dp(this, 760), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(page);

        buildRows();
    }

    private void buildRows() {
        addRow(R.string.settings_accent,
                () -> getString(prefs.accent().label),
                direction -> {
                    int next = Cycle.wrap(Accents.indexOf(prefs.accent().id), Accents.ALL.length, direction);
                    prefs.setAccent(Accents.ALL[next].id);
                    // The focus outlines were built with the old colour: rebuild the screen with the new one.
                    recreate();
                });

        addRow(R.string.settings_wallpaper,
                () -> getString(prefs.wallpaper().label()),
                direction -> {
                    int next = Cycle.wrap(Wallpapers.indexOf(prefs.wallpaper().id()), Wallpapers.ALL.length, direction);
                    prefs.setWallpaper(Wallpapers.ALL[next].id());
                    getWindow().setBackgroundDrawable(prefs.wallpaper().load(this));
                });

        addRow(R.string.settings_clock,
                () -> getString(clockLabel(prefs.clockMode())),
                direction -> {
                    ClockMode[] modes = ClockMode.values();
                    prefs.setClockMode(modes[Cycle.wrap(prefs.clockMode().ordinal(), modes.length, direction)]);
                });

        addRow(R.string.settings_nontv,
                () -> getString(prefs.includeNonTvApps() ? R.string.value_on : R.string.value_off),
                direction -> prefs.setIncludeNonTvApps(!prefs.includeNonTvApps()));

        TextView name = addRow(R.string.settings_name,
                () -> prefs.name().isEmpty() ? getString(R.string.settings_name_none) : prefs.name(),
                null);
        ((View) name.getParent()).setOnClickListener(view -> editName(name));

        TextView reset = addRow(R.string.settings_reset_dock, () -> "", null);
        ((View) reset.getParent()).setOnClickListener(view ->
                new ChoiceDialog(this, getString(R.string.settings_reset_dock))
                        .option(getString(R.string.settings_reset_confirm), () -> {
                            AppRepository.loadAsync(this, true, apps -> {
                                java.util.Set<String> installed = new java.util.HashSet<>();
                                for (AppEntry app : apps) installed.add(app.packageName);
                                prefs.saveDock(DockDefaults.pick(installed));
                            });
                        })
                        .option(getString(R.string.dialog_cancel), () -> { })
                        .show());

        TextView home = addRow(R.string.settings_home, () -> getString(R.string.settings_home_open), null);
        ((View) home.getParent()).setOnClickListener(view -> openHomeSettings());

        TextView version = Ui.text(this, 14, Palette.TEXT_DIM, null);
        version.setText(getString(R.string.settings_version, versionName()));
        version.setPadding(Ui.dp(this, 18), Ui.dp(this, 20), 0, 0);
        rows.addView(version);

        rows.getChildAt(0).requestFocus();
    }

    /** One row; returns its value view. step is null for rows that act on OK only. */
    private TextView addRow(int label, final Value value, final Step step) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setFocusable(true);
        row.setClickable(true);
        row.setDefaultFocusHighlightEnabled(false);
        int padX = Ui.dp(this, 20);
        int padY = Ui.dp(this, 16);
        row.setPadding(padX, padY, padX, padY);
        row.setBackground(Ui.focusable(this, Palette.TILE, Palette.TILE_FOCUSED, accent, Ui.dp(this, 16)));

        TextView name = Ui.text(this, 20, Palette.TEXT, null);
        name.setText(label);
        row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView shown = Ui.text(this, 20, accent, null);
        shown.setText(value.get());
        row.addView(shown);

        if (step != null) {
            row.setOnClickListener(view -> {
                step.apply(1);
                shown.setText(value.get());
            });
            row.setOnKeyListener((view, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (keyCode != KeyEvent.KEYCODE_DPAD_LEFT && keyCode != KeyEvent.KEYCODE_DPAD_RIGHT) return false;
                step.apply(keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1);
                shown.setText(value.get());
                return true;
            });
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Ui.dp(this, 8);
        rows.addView(row, params);
        return shown;
    }

    private static int clockLabel(ClockMode mode) {
        switch (mode) {
            case H24: return R.string.clock_24;
            case H12: return R.string.clock_12;
            default: return R.string.clock_system;
        }
    }

    private void editName(final TextView shown) {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(prefs.name());
        input.setSelection(input.getText().length());
        input.setHint(R.string.dialog_name_hint);
        input.setTextColor(Palette.TEXT);
        input.setHintTextColor(Palette.TEXT_DIM);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        final ChoiceDialog dialog = new ChoiceDialog(this, getString(R.string.settings_name));
        final Runnable save = () -> {
            prefs.setName(input.getText().toString());
            shown.setText(prefs.name().isEmpty() ? getString(R.string.settings_name_none) : prefs.name());
        };
        input.setOnEditorActionListener((view, actionId, event) -> {
            save.run();
            dialog.dismiss();
            return true;
        });
        dialog.header(input)
                .option(getString(R.string.dialog_save), save)
                .option(getString(R.string.dialog_cancel), () -> { })
                .show();
    }

    /** Opens Android's own "home screen" settings page, where the default launcher can be changed back. */
    private void openHomeSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch (RuntimeException e) {
            Toast.makeText(this, R.string.settings_home_failed, Toast.LENGTH_LONG).show();
        }
    }

    private String versionName() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }
}
