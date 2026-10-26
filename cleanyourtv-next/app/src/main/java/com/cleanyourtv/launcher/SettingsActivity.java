package com.cleanyourtv.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
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

import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * The settings, in sections: look, dock, weather, cinema, and the state of each integration. A row steps to
 * its next value with OK or Right (Left goes back); rows that need typing or a choice open a dialog. Every
 * change is saved at once. A setting that cannot work says so on its row instead of pretending.
 */
public final class SettingsActivity extends Activity {
    private interface Value { String get(); }

    private interface Step { void apply(int direction); }

    private Prefs prefs;
    private boolean builtLight;
    private LinearLayout rows;

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

        TextView title = Ui.text(this, 36, Palette.TEXT, Ui.light(this));
        title.setText(R.string.settings_title);
        page.addView(title);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 40));
        rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        Ui.allowOverflow(rows);
        scroll.addView(rows, new FrameLayout.LayoutParams(
                Ui.dp(this, 820), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));
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
        refresh();
    }

    /** Rebuilds the rows: the wallpaper screen or a dialog may have changed what they show. */
    private void refresh() {
        View focused = getCurrentFocus();
        int index = focused == null ? 0 : rows.indexOfChild(focused);
        rows.removeAllViews();
        buildRows();
        View target = rows.getChildAt(Math.max(0, Math.min(index, rows.getChildCount() - 1)));
        if (target != null && target.isFocusable()) target.requestFocus();
        else firstFocusable();
    }

    private void firstFocusable() {
        for (int i = 0; i < rows.getChildCount(); i++) {
            if (rows.getChildAt(i).isFocusable()) {
                rows.getChildAt(i).requestFocus();
                return;
            }
        }
    }

    private void buildRows() {
        section(R.string.section_look);

        View wallpaperRow = addRow(R.string.settings_wallpaper, () -> wallpaperName(), null);
        wallpaperRow.setOnClickListener(v -> startActivity(new Intent(this, WallpaperActivity.class)));

        addRow(R.string.settings_effects, () -> getString(prefs.effects() == Effects.QUALITY
                ? R.string.effects_quality : R.string.effects_performance),
                direction -> {
                    prefs.setEffects(prefs.effects() == Effects.QUALITY ? Effects.PERFORMANCE : Effects.QUALITY);
                    if (!prefs.effects().allowsAnimatedWallpaper()) prefs.setAnimatedWallpaper(false);
                });
        addRow(R.string.settings_effects_hint, null, null).setFocusable(false);

        addRow(R.string.settings_animated, () -> animatedValue(),
                direction -> {
                    if (!prefs.effects().allowsAnimatedWallpaper()) {
                        Toast.makeText(this, R.string.animated_needs_quality, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (Wallpapers.CUSTOM.equals(prefs.wallpaperId())) {
                        Toast.makeText(this, R.string.animated_needs_scene, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    prefs.setAnimatedWallpaper(!prefs.animatedWallpaper());
                });

        addRow(R.string.settings_accent, () -> getString(prefs.accent().label),
                direction -> {
                    int next = Cycle.wrap(Accents.indexOf(prefs.accent().id), Accents.ALL.length, direction);
                    prefs.setAccent(Accents.ALL[next].id);
                });

        addRow(R.string.settings_clock, () -> getString(clockLabel(prefs.clockMode())),
                direction -> {
                    ClockMode[] modes = ClockMode.values();
                    prefs.setClockMode(modes[Cycle.wrap(prefs.clockMode().ordinal(), modes.length, direction)]);
                });

        View nameRow = addRow(R.string.settings_name,
                () -> prefs.name().isEmpty() ? getString(R.string.settings_none) : prefs.name(), null);
        nameRow.setOnClickListener(v -> editText(R.string.settings_name, prefs.name(), false, text -> prefs.setName(text)));

        section(R.string.section_dock);
        addRow(R.string.settings_nontv, () -> getString(prefs.includeNonTvApps() ? R.string.value_on : R.string.value_off),
                direction -> prefs.setIncludeNonTvApps(!prefs.includeNonTvApps()));
        View resetRow = addRow(R.string.settings_reset_dock, () -> "", null);
        resetRow.setOnClickListener(v -> new ChoiceDialog(this, getString(R.string.settings_reset_dock))
                .option(getString(R.string.settings_reset_confirm), this::resetDock)
                .option(getString(R.string.dialog_cancel), () -> { })
                .show());
        addRow(R.string.settings_dock_hint, null, null).setFocusable(false);

        section(R.string.section_weather);
        View cityRow = addRow(R.string.settings_city,
                () -> prefs.hasCity() ? prefs.cityName() : getString(R.string.settings_none), null);
        cityRow.setOnClickListener(v -> editText(R.string.settings_city_search, "", false, this::searchCity));

        section(R.string.section_content);
        View sourceRow = addRow(R.string.settings_source, () -> sourceName(), null);
        sourceRow.setOnClickListener(v -> chooseSource());
        View permissionRow = addRow(R.string.settings_tv_permission, () -> getString(TvChannels.permissionGranted(this)
                ? R.string.permission_granted : R.string.permission_missing), null);
        permissionRow.setOnClickListener(v -> {
            if (TvChannels.permissionGranted(this)) {
                Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show();
            } else {
                requestPermissions(new String[] {TvChannels.PERMISSION}, 9);
            }
        });
        addRow(R.string.settings_source_hint, null, null).setFocusable(false);

        section(R.string.section_status);
        addRow(R.string.status_content, () -> statusText(prefs.status("channels")), null);
        addRow(R.string.status_weather, () -> statusText(prefs.status("weather")), null);
        addRow(R.string.status_resume_hint, null, null).setFocusable(false);

        section(R.string.section_about);
        addRow(R.string.settings_version, () -> versionName(), null);
        addRow(R.string.about_credits, null, null).setFocusable(false);
    }

    // --- values -----------------------------------------------------------------------------------------

    private String wallpaperName() {
        if (Wallpapers.CUSTOM.equals(prefs.wallpaperId())) return getString(R.string.wallpaper_custom);
        return getString(Wallpapers.byId(prefs.wallpaperId()).label());
    }

    private String animatedValue() {
        if (!prefs.effects().allowsAnimatedWallpaper()) return getString(R.string.value_unavailable);
        return getString(prefs.animatedWallpaper() ? R.string.value_on : R.string.value_off);
    }

    private String sourceName() {
        String saved = prefs.sourcePackage();
        if (saved.isEmpty()) return getString(R.string.source_auto);
        return saved;
    }

    private String statusText(String code) {
        Status.Parsed status = Status.parse(code);
        String when = status.time > 0
                ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(status.time)) : "";
        switch (status.kind) {
            case OK: return getString(R.string.status_ok, when);
            case STALE: return getString(R.string.status_stale, when);
            case NO_SOURCE: return getString(R.string.status_no_source);
            case NO_PERMISSION: return getString(R.string.status_no_permission);
            case NOTHING: return getString(R.string.status_nothing);
            case NO_CITY: return getString(R.string.status_no_city);
            case OFFLINE: return getString(R.string.status_offline);
            case FAILED: return getString(R.string.status_failed);
            default: return getString(R.string.status_never);
        }
    }

    private static int clockLabel(ClockMode mode) {
        switch (mode) {
            case H24: return R.string.clock_24;
            case H12: return R.string.clock_12;
            default: return R.string.clock_system;
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

    // --- rows -------------------------------------------------------------------------------------------

    private void section(int title) {
        TextView view = Ui.text(this, 16, Palette.TEXT_FAINT, Ui.medium(this));
        view.setText(getString(title).toUpperCase(java.util.Locale.getDefault()));
        view.setLetterSpacing(0.12f);
        view.setPadding(Ui.dp(this, 6), Ui.dp(this, 26), 0, Ui.dp(this, 4));
        rows.addView(view);
    }

    /**
     * One row. value and step may be null: a row with neither is a note (and the caller turns its focus off).
     * Returns the row view so the caller can set an OK action.
     */
    private View addRow(int label, final Value value, final Step step) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setFocusable(true);
        row.setClickable(true);
        row.setDefaultFocusHighlightEnabled(false);
        int padX = Ui.dp(this, 22);
        int padY = Ui.dp(this, 15);
        row.setPadding(padX, padY, padX, padY);
        row.setBackground(Ui.focusGlass(this, Palette.TILE, Palette.TILE_FOCUSED, Ui.dp(this, 18)));

        boolean note = value == null && step == null;
        TextView name = Ui.text(this, note ? 15 : 20, note ? Palette.TEXT_DIM : Palette.TEXT, Ui.regular(this));
        name.setText(label);
        row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (note) row.setBackground(null);

        if (value != null) {
            final TextView shown = Ui.text(this, 20, prefs.accent().color, Ui.medium(this));
            shown.setText(value.get());
            shown.setGravity(Gravity.END);
            row.addView(shown, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            if (step != null) {
                row.setOnClickListener(view -> {
                    step.apply(1);
                    shown.setText(value.get());
                    if (label == R.string.settings_accent || label == R.string.settings_effects) recreate();
                });
                row.setOnKeyListener((view, keyCode, event) -> {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                    if (keyCode != KeyEvent.KEYCODE_DPAD_LEFT && keyCode != KeyEvent.KEYCODE_DPAD_RIGHT) return false;
                    step.apply(keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1);
                    shown.setText(value.get());
                    if (label == R.string.settings_accent || label == R.string.settings_effects) recreate();
                    return true;
                });
            }
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Ui.dp(this, note ? 2 : 8);
        rows.addView(row, params);
        return row;
    }

    // --- actions ----------------------------------------------------------------------------------------

    private interface Text { void accept(String text); }

    private void editText(int title, String initial, boolean sensitive, final Text onSave) {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(initial);
        input.setSelection(input.getText().length());
        input.setTextColor(Palette.TEXT);
        input.setHintTextColor(Palette.TEXT_DIM);
        input.setTextSize(20);
        input.setTypeface(Ui.regular(this));
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        if (sensitive) input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        final ChoiceDialog dialog = new ChoiceDialog(this, getString(title));
        final Runnable save = () -> {
            onSave.accept(input.getText().toString().trim());
            refresh();
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

    private void searchCity(final String name) {
        if (name.isEmpty()) return;
        Toast.makeText(this, R.string.city_searching, Toast.LENGTH_SHORT).show();
        Weather.search(name, (places, failed) -> {
            if (isDestroyed()) return;
            if (failed) {
                Toast.makeText(this, R.string.city_search_failed, Toast.LENGTH_LONG).show();
                return;
            }
            if (places.isEmpty()) {
                Toast.makeText(this, R.string.city_none, Toast.LENGTH_LONG).show();
                return;
            }
            ChoiceDialog dialog = new ChoiceDialog(this, getString(R.string.settings_city));
            for (final GeoParser.Place place : places) {
                dialog.option(place.label(), () -> {
                    prefs.setCity(place.name, place.latitude, place.longitude);
                    refresh();
                });
            }
            dialog.option(getString(R.string.dialog_cancel), () -> { });
            dialog.show();
        });
    }

    /** Lists the installed apps to pick the one whose home-screen content the panel shows. */
    private void chooseSource() {
        AppRepository.loadAsync(this, true, apps -> {
            if (isDestroyed()) return;
            ChoiceDialog dialog = new ChoiceDialog(this, getString(R.string.settings_source));
            dialog.option(getString(R.string.source_auto), () -> {
                prefs.setSourcePackage("");
                refresh();
            });
            for (final AppEntry app : apps) {
                dialog.option(app.label, () -> {
                    prefs.setSourcePackage(app.packageName);
                    refresh();
                });
            }
            dialog.show();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        refresh();
    }

    private void resetDock() {
        AppRepository.loadAsync(this, true, apps -> {
            Set<String> installed = new HashSet<>();
            for (AppEntry app : apps) installed.add(app.packageName);
            prefs.saveDock(DockDefaults.pick(installed));
            Toast.makeText(this, R.string.toast_dock_reset, Toast.LENGTH_SHORT).show();
        });
    }
}
