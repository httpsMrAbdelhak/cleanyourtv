package com.cleanyourtv.launcher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Discovery page: a big backdrop with the title of the poster in focus, and rails of trending films and
 * series from TMDB below. Everything shown comes from TMDB or from its cache, and is labelled as old when
 * the network failed. With no key, or offline with no cache, the page says so instead of showing anything made up.
 */
final class CinemaPage extends Page implements RailBlock.Listener {
    private static final Tmdb.Feed[] FEEDS = {Tmdb.Feed.TRENDING, Tmdb.Feed.MOVIES, Tmdb.Feed.SERIES, Tmdb.Feed.NEW};
    private static final int[] TITLES = {R.string.rail_trending, R.string.rail_movies, R.string.rail_series, R.string.rail_new};
    private static final long HERO_DELAY_MS = 150;

    private final Handler handler = new Handler(Looper.getMainLooper());
    /** Two stacked backdrops: the new picture loads into the hidden one, then they cross-fade and swap roles. */
    private ImageView heroFront;
    private ImageView heroBack;
    private final TextView heroTitle;
    private final TextView heroMeta;
    private final TextView heroOverview;
    private final TextView notice;
    private final LinearLayout stateBox;
    private final TextView stateText;
    private final TextView stateButton;
    private final ScrollView vertical;
    private final LinearLayout column;
    private final List<RailBlock> rails = new ArrayList<>();
    private final boolean[] loaded = new boolean[FEEDS.length];
    private final long[] loadedAt = new long[FEEDS.length];
    private MediaItem pending;
    private String heroPath = "";
    private boolean shown;

    private final Runnable applyHero = new Runnable() {
        @Override public void run() {
            if (pending != null) showHero(pending);
        }
    };

    CinemaPage(Context context, Host host, Prefs prefs) {
        super(context, host, prefs);
        int w = getResources().getDisplayMetrics().widthPixels;
        int h = getResources().getDisplayMetrics().heightPixels;

        heroBack = backdropView(context);
        heroFront = backdropView(context);
        FrameLayout.LayoutParams heroBox = new FrameLayout.LayoutParams(w * 72 / 100, h * 66 / 100, Gravity.TOP | Gravity.END);
        addView(heroBack, heroBox);
        addView(heroFront, heroBox);
        // Fades the picture into the page on its left and bottom edges, so it has no hard border.
        View fadeLeft = new View(context);
        fadeLeft.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT, new int[] {0xFF05060A, 0x9905060A, 0x0005060A}));
        addView(fadeLeft, new FrameLayout.LayoutParams(w * 60 / 100, h * 70 / 100, Gravity.TOP | Gravity.START));
        View fadeBottom = new View(context);
        fadeBottom.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, new int[] {0x0005060A, 0xCC05060A, 0xFF05060A}));
        addView(fadeBottom, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h * 36 / 100, Gravity.TOP));
        ((FrameLayout.LayoutParams) fadeBottom.getLayoutParams()).topMargin = h * 40 / 100;

        LinearLayout text = new LinearLayout(context);
        text.setOrientation(LinearLayout.VERTICAL);
        heroTitle = Ui.text(context, 40, Palette.TEXT, Ui.medium(context));
        heroTitle.setMaxLines(2);
        heroTitle.setEllipsize(TextUtils.TruncateAt.END);
        heroMeta = Ui.text(context, 18, Palette.TEXT_DIM, Ui.regular(context));
        heroMeta.setPadding(0, Ui.dp(context, 8), 0, Ui.dp(context, 8));
        heroOverview = Ui.text(context, 17, Palette.TEXT_DIM, Ui.light(context));
        heroOverview.setMaxLines(3);
        heroOverview.setEllipsize(TextUtils.TruncateAt.END);
        heroOverview.setLineSpacing(0, 1.12f);
        text.addView(heroTitle);
        text.addView(heroMeta);
        text.addView(heroOverview);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(w * 46 / 100, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        textParams.setMargins(Ui.dp(context, 72), h * 17 / 100, 0, 0);
        addView(text, textParams);

        vertical = new RailsScroll(context);
        vertical.setVerticalScrollBarEnabled(false);
        vertical.setClipChildren(false);
        vertical.setClipToPadding(false);
        vertical.setVerticalFadingEdgeEnabled(true);
        vertical.setFadingEdgeLength(Ui.dp(context, 24));
        column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setClipChildren(false);
        column.setPadding(Ui.dp(context, 68), 0, 0, Ui.dp(context, 70));
        for (int i = 0; i < FEEDS.length; i++) {
            RailBlock rail = new RailBlock(context, prefs, TITLES[i], this);
            rails.add(rail);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = Ui.dp(context, 4);
            column.addView(rail, params);
        }
        vertical.addView(column);
        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, h * 48 / 100, Gravity.BOTTOM);
        addView(vertical, scrollParams);

        notice = Ui.text(context, 13, Palette.TEXT_FAINT, Ui.regular(context));
        notice.setText(R.string.tmdb_attribution);
        FrameLayout.LayoutParams noticeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
        noticeParams.setMargins(Ui.dp(context, 72), 0, 0, Ui.dp(context, 22));
        addView(notice, noticeParams);

        stateBox = new LinearLayout(context);
        stateBox.setOrientation(LinearLayout.VERTICAL);
        stateBox.setGravity(Gravity.CENTER_HORIZONTAL);
        stateBox.setVisibility(GONE);
        int pad = Ui.dp(context, 34);
        stateBox.setPadding(pad, pad, pad, pad);
        stateBox.setBackground(new GlassDrawable(Ui.dp(context, 32), Ui.dp(context, 1.5f), true, false, prefs.effects()));
        stateText = Ui.text(context, 22, Palette.TEXT, Ui.regular(context));
        stateText.setGravity(Gravity.CENTER);
        stateText.setLineSpacing(0, 1.15f);
        stateButton = Ui.text(context, 20, Palette.TEXT, Ui.medium(context));
        stateButton.setGravity(Gravity.CENTER);
        stateButton.setPadding(Ui.dp(context, 30), Ui.dp(context, 13), Ui.dp(context, 30), Ui.dp(context, 13));
        stateButton.setFocusable(true);
        stateButton.setClickable(true);
        stateButton.setDefaultFocusHighlightEnabled(false);
        stateButton.setBackground(Ui.focusGlass(context, 0x33FFFFFF, 0x66FFFFFF, Ui.dp(context, 26)));
        stateButton.setOnKeyListener((view, keyCode, event) -> event.getAction() == android.view.KeyEvent.ACTION_DOWN
                && keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP && consumeUp());
        stateBox.addView(stateText, new LinearLayout.LayoutParams(Ui.dp(context, 520), ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = Ui.dp(context, 22);
        stateBox.addView(stateButton, buttonParams);
        addView(stateBox, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
    }

    private ImageView backdropView(Context context) {
        ImageView view = new ImageView(context);
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        view.setAlpha(0f);
        return view;
    }

    /** Vertical strip of rails: the framework's instant scroll is off; focus changes glide instead. */
    private final class RailsScroll extends ScrollView {
        RailsScroll(Context context) {
            super(context);
        }

        @Override
        protected int computeScrollDeltaToGetChildRectOnScreen(android.graphics.Rect rect) {
            return 0;
        }
    }

    // --- Page --------------------------------------------------------------------------------------------

    @Override
    void onShown() {
        shown = true;
        long now = System.currentTimeMillis();
        boolean any = false;
        for (int i = 0; i < FEEDS.length; i++) {
            if (!loaded[i] || !CachePolicy.isFresh(now, loadedAt[i], CachePolicy.TMDB_TTL_MS)) {
                loadFeed(i, false);
                any = true;
            }
        }
        if (!any && stateBox.getVisibility() != VISIBLE) refreshRails();
    }

    @Override
    void onHidden() {
        shown = false;
        handler.removeCallbacks(applyHero);
        for (RailBlock rail : rails) rail.releaseImages();
        ImageLoader.get(getContext()).release(heroFront);
        ImageLoader.get(getContext()).release(heroBack);
        heroPath = "";
        // The rails keep their poster views, so a return to the page reloads the pictures near the focus.
        for (int i = 0; i < loaded.length; i++) loaded[i] = false;
    }

    @Override
    boolean enter() {
        if (stateBox.getVisibility() == VISIBLE) return stateButton.requestFocus();
        for (RailBlock rail : rails) {
            if (rail.count() > 0) return rail.focusFirst();
        }
        return false;
    }

    @Override
    void onLookChanged() {
        for (RailBlock rail : rails) rail.refreshImages();
    }

    private void loadFeed(final int index, boolean force) {
        if (!Tmdb.hasKey(prefs)) {
            showState(R.string.cinema_no_key, R.string.action_open_settings, () -> host.openSettings());
            return;
        }
        if (!loaded[index] && rails.get(index).count() == 0) rails.get(index).showLoading();
        Tmdb.load(getContext(), FEEDS[index], force, result -> {
            if (!shown) return;
            onFeed(index, result);
        });
    }

    private void onFeed(int index, Tmdb.Result result) {
        if (result.error != Tmdb.Error.NONE || result.items.isEmpty()) {
            if (rails.get(index).count() == 0 && allRailsEmpty()) {
                int message = result.error == Tmdb.Error.BAD_KEY ? R.string.cinema_bad_key
                        : result.error == Tmdb.Error.OFFLINE ? R.string.cinema_offline : R.string.cinema_failed;
                int action = result.error == Tmdb.Error.BAD_KEY ? R.string.action_open_settings : R.string.action_retry;
                if (result.error == Tmdb.Error.BAD_KEY) showState(message, action, () -> host.openSettings());
                else showState(message, action, this::retry);
            } else {
                rails.get(index).setItems(new ArrayList<MediaItem>());
                rails.get(index).setVisibility(GONE);
            }
            return;
        }
        stateBox.setVisibility(GONE);
        vertical.setVisibility(VISIBLE);
        loaded[index] = true;
        loadedAt[index] = System.currentTimeMillis();
        rails.get(index).setVisibility(VISIBLE);
        rails.get(index).setItems(result.items);
        if (result.stale) {
            notice.setText(getContext().getString(R.string.tmdb_stale,
                    DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(result.fetchedAt))));
        } else {
            notice.setText(R.string.tmdb_attribution);
        }
        if (heroPath.isEmpty() && pending == null && !result.items.isEmpty()) showHero(result.items.get(0));
    }

    private boolean allRailsEmpty() {
        for (RailBlock rail : rails) {
            if (rail.count() > 0) return false;
        }
        return true;
    }

    private void refreshRails() {
        for (RailBlock rail : rails) rail.refreshImages();
    }

    private void retry() {
        stateBox.setVisibility(GONE);
        vertical.setVisibility(VISIBLE);
        for (int i = 0; i < FEEDS.length; i++) loadFeed(i, true);
    }

    private void showState(int message, int action, final Runnable onAction) {
        vertical.setVisibility(INVISIBLE);
        stateText.setText(message);
        stateButton.setText(action);
        stateButton.setOnClickListener(view -> onAction.run());
        stateBox.setVisibility(VISIBLE);
    }

    // --- hero --------------------------------------------------------------------------------------------

    @Override
    public void onFocused(MediaItem item, RailBlock rail) {
        pending = item;
        handler.removeCallbacks(applyHero);
        handler.postDelayed(applyHero, HERO_DELAY_MS);
        int at = rails.indexOf(rail);
        int top = column.getChildAt(Math.max(0, at)).getTop();
        vertical.smoothScrollTo(0, top);
    }

    private void showHero(MediaItem item) {
        heroTitle.setText(item.title);
        StringBuilder meta = new StringBuilder(getContext().getString(item.series ? R.string.kind_series : R.string.kind_movie));
        if (!item.year.isEmpty()) meta.append("  •  ").append(item.year);
        if (item.rating > 0) meta.append("  •  ★ ").append(String.format(Locale.getDefault(), "%.1f", item.rating));
        heroMeta.setText(meta);
        heroOverview.setText(item.overview);
        String path = item.backdropPath.isEmpty() ? "" : item.backdropPath;
        if (path.isEmpty() || path.equals(heroPath)) return;
        heroPath = path;
        // Load into the hidden layer, then swap: the old picture stays until the new one is ready.
        final ImageView next = heroBack;
        ImageLoader.get(getContext()).load(Tmdb.backdropUrl(path), next, 780, prefs.effects().lightImages(), false);
        next.postDelayed(new Runnable() {
            @Override public void run() {
                crossfade(next);
            }
        }, 260);
    }

    private void crossfade(ImageView next) {
        if (next.getDrawable() == null || !shown) return;
        next.animate().alpha(1f).setDuration(380).start();
        heroFront.animate().alpha(0f).setDuration(380).start();
        ImageView old = heroFront;
        heroFront = next;
        heroBack = old;
    }

    private boolean consumeUp() {
        host.focusTabs();
        return true;
    }

    @Override
    public void onOpen(MediaItem item) {
        host.openDetail(item);
    }

    @Override
    public boolean onUp(RailBlock rail) {
        if (rails.indexOf(rail) == firstRailWithItems()) {
            host.focusTabs();
            return true;
        }
        return false;
    }

    private int firstRailWithItems() {
        for (int i = 0; i < rails.size(); i++) {
            if (rails.get(i).count() > 0) return i;
        }
        return 0;
    }
}
