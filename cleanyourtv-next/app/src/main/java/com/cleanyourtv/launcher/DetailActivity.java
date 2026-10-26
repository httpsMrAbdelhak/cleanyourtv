package com.cleanyourtv.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * A film or series: backdrop, poster, title, year, rating and the synopsis. It says plainly that it cannot
 * start playback: TMDB describes titles, it gives no link to watch them, and Android offers a launcher no
 * way to open "this film" in another app. Back returns to the rails.
 */
public final class DetailActivity extends Activity {
    private static final String EXTRA = "item";

    static Intent intent(Context context, MediaItem item) {
        return new Intent(context, DetailActivity.class).putExtra(EXTRA, item);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        MediaItem item = (MediaItem) getIntent().getSerializableExtra(EXTRA);
        if (item == null) {
            finish();
            return;
        }
        Prefs prefs = new Prefs(this);
        boolean light = prefs.effects().lightImages();
        int w = getResources().getDisplayMetrics().widthPixels;
        int h = getResources().getDisplayMetrics().heightPixels;

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Palette.BACKGROUND);

        ImageView backdrop = new ImageView(this);
        backdrop.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(backdrop, new FrameLayout.LayoutParams(w, h * 80 / 100, Gravity.TOP | Gravity.END));
        if (!item.backdropPath.isEmpty()) {
            ImageLoader.get(this).load(Tmdb.backdropUrl(item.backdropPath), backdrop, 780, light, true);
        }
        View shade = new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {0xFF05060A, 0xE605060A, 0x6605060A}));
        root.addView(shade, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        View floor = new View(this);
        floor.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0x0005060A, 0xFF05060A}));
        root.addView(floor, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h * 45 / 100, Gravity.BOTTOM));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        int margin = Ui.dp(this, 72);
        content.setPadding(margin, Ui.dp(this, 48), margin, Ui.dp(this, 48));

        ImageView poster = new ImageView(this);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.setBackground(Ui.placeholder());
        Ui.clipRounded(poster, Ui.dp(this, 18));
        poster.setElevation(Ui.dp(this, 12));
        int posterW = Ui.dp(this, 220);
        content.addView(poster, new LinearLayout.LayoutParams(posterW, posterW * 3 / 2));
        ImageLoader.get(this).load(Tmdb.posterUrl(item.posterPath, light), poster, posterW, light, true);

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(Ui.dp(this, 48), 0, 0, 0);
        TextView title = Ui.text(this, 46, Palette.TEXT, Ui.medium(this));
        title.setText(item.title);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        StringBuilder meta = new StringBuilder(getString(item.series ? R.string.kind_series : R.string.kind_movie));
        if (!item.year.isEmpty()) meta.append("  •  ").append(item.year);
        if (item.rating > 0) meta.append("  •  ★ ").append(String.format(Locale.getDefault(), "%.1f / 10", item.rating));
        TextView metaView = Ui.text(this, 20, Palette.TEXT_DIM, Ui.regular(this));
        metaView.setText(meta);
        metaView.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 14));
        TextView overview = Ui.text(this, 20, Palette.TEXT, Ui.light(this));
        overview.setText(item.overview.isEmpty() ? getString(R.string.detail_no_overview) : item.overview);
        overview.setMaxLines(7);
        overview.setEllipsize(TextUtils.TruncateAt.END);
        overview.setLineSpacing(0, 1.15f);
        TextView note = Ui.text(this, 15, Palette.TEXT_FAINT, Ui.regular(this));
        note.setText(R.string.detail_no_playback);
        note.setPadding(0, Ui.dp(this, 22), 0, Ui.dp(this, 20));

        TextView back = Ui.text(this, 20, Palette.TEXT, Ui.medium(this));
        back.setText(R.string.action_back);
        back.setGravity(Gravity.CENTER);
        back.setPadding(Ui.dp(this, 34), Ui.dp(this, 14), Ui.dp(this, 34), Ui.dp(this, 14));
        back.setFocusable(true);
        back.setClickable(true);
        back.setDefaultFocusHighlightEnabled(false);
        back.setBackground(Ui.focusGlass(this, 0x33FFFFFF, 0x66FFFFFF, Ui.dp(this, 28)));
        back.setOnClickListener(v -> finish());

        text.addView(title);
        text.addView(metaView);
        text.addView(overview);
        text.addView(note);
        text.addView(back, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        back.requestFocus();
    }
}
