package com.cleanyourtv.launcher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * The wallpaper gallery. Moving the focus over a card previews that wallpaper full screen behind the cards
 * (and the text switches to dark or light to suit it); OK applies it, and Back leaves the saved one in place.
 * The last card picks a picture from the device with the system file picker (no storage permission): the
 * picture is copied into the app, so it stays after a restart. The animated option lives in the settings.
 */
public final class WallpaperActivity extends Activity {
    private static final int PICK = 41;
    private static final int COLUMNS = 3;

    private Prefs prefs;
    private LinearLayout page;
    private LinearLayout grid;
    private TextView title;
    private TextView hint;
    private final List<TextView> labels = new ArrayList<>();
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new Prefs(this);
        Palette.apply(prefs);
        showSaved();

        int margin = Ui.dp(this, 48);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(margin, margin, margin, 0);

        title = Ui.text(this, 36, Palette.TEXT, Ui.light(this));
        title.setText(R.string.settings_wallpaper);
        page.addView(title);
        hint = Ui.text(this, 16, Palette.TEXT_DIM, Ui.regular(this));
        hint.setText(R.string.wallpaper_hint);
        hint.setPadding(0, Ui.dp(this, 6), 0, 0);
        page.addView(hint);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 32));
        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        Ui.allowOverflow(grid);
        scroll.addView(grid, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(page);
        build();
    }

    private void showSaved() {
        getWindow().setBackgroundDrawable(Wallpaper.load(this, prefs));
    }

    private void build() {
        grid.removeAllViews();
        labels.clear();
        int cardW = Ui.dp(this, 264);
        int cardH = Ui.dp(this, 149);
        int count = Wallpapers.ALL.length + 1;
        LinearLayout row = null;
        View first = null;
        for (int i = 0; i < count; i++) {
            if (i % COLUMNS == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                Ui.allowOverflow(row);
                grid.addView(row);
            }
            final boolean custom = i == Wallpapers.ALL.length;
            final WallpaperSource source = custom ? null : Wallpapers.ALL[i];
            String id = custom ? Wallpapers.CUSTOM : source.id();
            View card = card(source, id, cardW, cardH);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(cardW, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 8));
            row.addView(card, params);
            if (first == null || id.equals(prefs.wallpaperId())) first = card;
        }
        if (first != null) first.requestFocus();
    }

    /** One card: the picture, its name and a tick on the saved one. */
    private View card(final WallpaperSource source, final String id, int width, int height) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setFocusable(true);
        box.setClickable(true);
        box.setDefaultFocusHighlightEnabled(false);
        box.setClipChildren(false);

        FrameLayout picture = new FrameLayout(this);
        float radius = Ui.dp(this, 20);
        if (source != null) {
            picture.setBackground(source.load(this));
        } else {
            picture.setBackground(Ui.round(Palette.TILE, radius));
            TextView plus = Ui.text(this, 54, Palette.TEXT_DIM, Ui.light(this));
            plus.setText("+");
            plus.setGravity(Gravity.CENTER);
            picture.addView(plus, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        Ui.clipRounded(picture, radius);
        GradientDrawable ring = new GradientDrawable();
        ring.setCornerRadius(radius);
        ring.setStroke(Ui.dp(this, 3), Palette.RING);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_focused}, ring);
        states.addState(new int[] {}, new ColorDrawable(0x00000000));
        box.addView(picture, new LinearLayout.LayoutParams(width, height));
        picture.setForeground(states);
        picture.setDuplicateParentStateEnabled(true);

        boolean saved = id.equals(prefs.wallpaperId());
        TextView label = Ui.text(this, 17, saved ? Palette.TEXT : Palette.TEXT_DIM, Ui.medium(this));
        label.setText((saved ? "✓  " : "") + getString(source != null ? source.label() : R.string.wallpaper_custom));
        label.setPadding(Ui.dp(this, 6), Ui.dp(this, 8), 0, 0);
        label.setTag(saved);
        labels.add(label);
        box.addView(label);

        box.setOnFocusChangeListener((view, hasFocus) -> {
            float scale = hasFocus ? 1.05f : 1f;
            view.animate().scaleX(scale).scaleY(scale).setDuration(160).start();
            if (hasFocus) preview(source, id);
        });
        box.setOnClickListener(view -> {
            if (source == null) {
                pickPicture();
                return;
            }
            prefs.setWallpaperId(id);
            Palette.apply(prefs);
            Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
            recreate();
        });
        return box;
    }

    /** Shows a wallpaper behind the cards and switches the text colours to suit its brightness. */
    private void preview(WallpaperSource source, String id) {
        boolean light;
        Drawable background = null;
        if (source != null) {
            background = source.load(this);
            light = source.light();
        } else if (WallpaperStore.exists(this)) {
            background = WallpaperStore.load(this, prefs.effects().lightImages());
            light = prefs.customWallpaperLight();
        } else {
            background = Wallpaper.load(this, prefs);
            light = Wallpaper.isLight(prefs);
        }
        if (background != null) getWindow().setBackgroundDrawable(background);
        Palette.apply(light, prefs.accent().color);
        title.setTextColor(Palette.TEXT);
        hint.setTextColor(Palette.TEXT_DIM);
        for (TextView label : labels) {
            label.setTextColor(Boolean.TRUE.equals(label.getTag()) ? Palette.TEXT : Palette.TEXT_DIM);
        }
    }

    private void pickPicture() {
        Intent open = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*");
        try {
            startActivityForResult(open, PICK);
        } catch (ActivityNotFoundException first) {
            try {
                startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("image/*"), PICK);
            } catch (ActivityNotFoundException second) {
                // Many TV boxes ship without a file picker: say so, keep the gallery usable.
                Toast.makeText(this, R.string.wallpaper_no_picker, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        final Uri uri = data.getData();
        Toast.makeText(this, R.string.wallpaper_saving, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            boolean ok;
            boolean light = false;
            try {
                light = WallpaperStore.save(this, uri);
                ok = true;
            } catch (java.io.IOException | RuntimeException | OutOfMemoryError e) {
                ok = false;
            }
            final boolean saved = ok;
            final boolean pale = light;
            main.post(() -> {
                if (isDestroyed()) return;
                if (saved) {
                    prefs.setWallpaperId(Wallpapers.CUSTOM);
                    prefs.setCustomWallpaperLight(pale);
                    prefs.setAnimatedWallpaper(false);
                    Palette.apply(prefs);
                    Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                    recreate();
                } else {
                    Toast.makeText(this, R.string.wallpaper_pick_failed, Toast.LENGTH_LONG).show();
                }
            });
        }, "wallpaper-save").start();
    }
}
