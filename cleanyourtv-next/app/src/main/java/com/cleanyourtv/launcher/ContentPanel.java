package com.cleanyourtv.launcher;

import android.content.Context;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The panel above the dock. What it shows depends on the dock tile that has the focus:
 * a streaming app shows that service's films and series (TMDB), YouTube shows the most popular videos, the
 * weather tile shows the weather, anything else shows what is trending. Each state without data (no key, no
 * city, offline, error) is a plain message with one button, never a made-up list. A late answer for a tile
 * the focus has already left is ignored (the token).
 */
final class ContentPanel extends FrameLayout implements CardRail.Listener, WeatherPanel.Listener {
    /** What the dock selection asks the panel to show. */
    static final class Selection {
        enum Kind { GENERAL, STREAMING, YOUTUBE, WEATHER }

        final Kind kind;
        final String packageName;
        final String label;

        Selection(Kind kind, String packageName, String label) {
            this.kind = kind;
            this.packageName = packageName;
            this.label = label;
        }
    }

    /** What the panel asks of the screen around it. */
    interface Host {
        void focusHeader();

        void focusDock();

        void openSettings();

        void openDetail(MediaItem item, String packageName, String appLabel);
    }

    static final int TAB_EXPLORE = 0;
    static final int TAB_MOVIES = 1;
    static final int TAB_SERIES = 2;

    private final Host host;
    private final Prefs prefs;
    private final CardRail rail;
    private final WeatherPanel weather;
    private final LinearLayout box;
    private final TextView boxText;
    private final TextView boxButton;
    private final TextView notice;

    private Selection selection = new Selection(Selection.Kind.GENERAL, null, "");
    private int tab = TAB_EXPLORE;
    private int token;
    private boolean shown;

    ContentPanel(Context context, Host host, Prefs prefs) {
        super(context);
        this.host = host;
        this.prefs = prefs;
        setClipChildren(false);
        setClipToPadding(false);

        rail = new CardRail(context, prefs, this);
        addView(rail, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        weather = new WeatherPanel(context, prefs, this);
        weather.setVisibility(GONE);
        addView(weather, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        notice = Ui.text(context, 12, Palette.TEXT_FAINT, Ui.regular(context));
        LayoutParams noticeParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
        noticeParams.leftMargin = Ui.dp(context, 8);
        addView(notice, noticeParams);

        box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setVisibility(GONE);
        int pad = Ui.dp(context, 28);
        box.setPadding(pad, pad, pad, pad);
        box.setBackground(new GlassDrawable(Ui.dp(context, 28), Ui.dp(context, 1.5f), true, false, prefs.effects()));
        boxText = Ui.text(context, 18, Palette.TEXT, Ui.regular(context));
        boxText.setGravity(Gravity.CENTER);
        boxText.setLineSpacing(0, 1.15f);
        boxButton = Ui.text(context, 17, Palette.TEXT, Ui.medium(context));
        boxButton.setGravity(Gravity.CENTER);
        boxButton.setPadding(Ui.dp(context, 26), Ui.dp(context, 11), Ui.dp(context, 26), Ui.dp(context, 11));
        boxButton.setFocusable(true);
        boxButton.setClickable(true);
        boxButton.setDefaultFocusHighlightEnabled(false);
        boxButton.setBackground(Ui.focusGlass(context, Palette.TILE, Palette.TILE_FOCUSED, Ui.dp(context, 24)));
        boxButton.setOnKeyListener((view, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) return onUp();
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) return onDown();
            return keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT;
        });
        box.addView(boxText, new LinearLayout.LayoutParams(Ui.dp(context, 480), ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = Ui.dp(context, 18);
        box.addView(boxButton, buttonParams);
        addView(box, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));
    }

    // --- control ----------------------------------------------------------------------------------------

    void onShown() {
        shown = true;
        refresh(false);
    }

    void onHidden() {
        shown = false;
        token++;
        rail.releaseImages();
    }

    /** The dock selection changed: show what fits the new tile. */
    void select(Selection next) {
        boolean same = next.kind == selection.kind
                && (next.packageName == null ? selection.packageName == null : next.packageName.equals(selection.packageName));
        selection = next;
        // The same tile again (the first focus after the panel already showed it): nothing to reload.
        if (shown && !(same && hasContent())) refresh(false);
    }

    void selectTab(int next) {
        if (next == tab) return;
        tab = next;
        if (shown) refresh(false);
    }

    /** Whether the header tabs mean something for the current selection. */
    boolean usesTabs() {
        return selection.kind == Selection.Kind.GENERAL || selection.kind == Selection.Kind.STREAMING;
    }

    void onLookChanged() {
        rail.refreshImages();
    }

    /** Puts the focus on the first card (or the message button). False when there is nothing to focus. */
    boolean enter() {
        if (box.getVisibility() == VISIBLE) return boxButton.requestFocus();
        if (weather.getVisibility() == VISIBLE) return weather.focusCard();
        return rail.focusFirst();
    }

    private void refresh(boolean force) {
        final int mine = ++token;
        notice.setText("");
        switch (selection.kind) {
            case WEATHER:
                loadWeather(mine, force);
                break;
            case YOUTUBE:
                loadYouTube(mine, force);
                break;
            case STREAMING:
                loadFilms(mine, force, selection.packageName);
                break;
            default:
                loadFilms(mine, force, null);
                break;
        }
    }

    // --- films and series -------------------------------------------------------------------------------

    private void loadFilms(final int mine, final boolean force, final String service) {
        if (!Tmdb.hasKey(prefs)) {
            message(R.string.cinema_no_key, R.string.action_open_settings, () -> host.openSettings());
            return;
        }
        showRail();
        rail.showLoading();
        final List<FeedSpec> feeds = new ArrayList<>();
        if (tab == TAB_MOVIES || tab == TAB_EXPLORE) feeds.add(service == null
                ? (tab == TAB_EXPLORE ? FeedSpec.trending() : FeedSpec.popular(false)) : FeedSpec.onService(service, false));
        if (service != null && tab == TAB_EXPLORE) feeds.add(FeedSpec.onService(service, true));
        if (tab == TAB_SERIES) feeds.add(service == null ? FeedSpec.popular(true) : FeedSpec.onService(service, true));

        final List<Tmdb.Result> results = new ArrayList<>();
        for (FeedSpec feed : feeds) {
            Tmdb.load(getContext(), feed, force, result -> {
                if (mine != token) return;
                results.add(result);
                if (results.size() == feeds.size()) showFilms(results);
            });
        }
    }

    private void showFilms(List<Tmdb.Result> results) {
        List<MediaItem> items = new ArrayList<>();
        Tmdb.Error error = Tmdb.Error.NONE;
        boolean stale = false;
        boolean providerMissing = false;
        long fetchedAt = 0;
        int longest = 0;
        for (Tmdb.Result result : results) longest = Math.max(longest, result.items.size());
        // Interleave films and series so that "Explore" mixes both.
        for (int i = 0; i < longest; i++) {
            for (Tmdb.Result result : results) {
                if (i < result.items.size()) items.add(result.items.get(i));
            }
        }
        for (Tmdb.Result result : results) {
            if (result.error != Tmdb.Error.NONE) error = result.error;
            stale |= result.stale;
            providerMissing |= result.providerMissing;
            fetchedAt = Math.max(fetchedAt, result.fetchedAt);
        }
        if (items.isEmpty()) {
            if (error == Tmdb.Error.BAD_KEY) {
                message(R.string.cinema_bad_key, R.string.action_open_settings, () -> host.openSettings());
            } else {
                message(error == Tmdb.Error.OFFLINE ? R.string.cinema_offline : R.string.cinema_failed,
                        R.string.action_retry, () -> refresh(true));
            }
            return;
        }
        showRail();
        List<ContentCard> cards = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            MediaItem item = items.get(i);
            String kind = getContext().getString(item.series ? R.string.kind_series : R.string.kind_movie);
            boolean wide = i == 0 && !item.backdropPath.isEmpty();
            cards.add(new ContentCard(wide ? ContentCard.Shape.WIDE : ContentCard.Shape.POSTER, item.title,
                    ContentCard.meta(kind, item),
                    wide ? Tmdb.backdropUrl(item.backdropPath) : Tmdb.posterUrl(item.posterPath, prefs.effects().lightImages()),
                    item, null));
        }
        rail.setCards(cards);
        String attribution = getContext().getString(R.string.tmdb_attribution);
        if (stale) {
            attribution = getContext().getString(R.string.tmdb_stale,
                    DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(fetchedAt))) + "  •  " + attribution;
        }
        if (providerMissing) {
            attribution = getContext().getString(R.string.tmdb_provider_missing, selection.label) + "  •  " + attribution;
        }
        notice.setText(attribution);
    }

    // --- YouTube ----------------------------------------------------------------------------------------

    private void loadYouTube(final int mine, boolean force) {
        if (!YouTube.hasKey(prefs)) {
            message(R.string.youtube_no_key, R.string.action_open_settings, () -> host.openSettings());
            return;
        }
        showRail();
        rail.showLoading();
        YouTube.load(getContext(), force, result -> {
            if (mine != token) return;
            if (result.videos.isEmpty()) {
                if (result.error == Tmdb.Error.BAD_KEY) {
                    message(R.string.youtube_bad_key, R.string.action_open_settings, () -> host.openSettings());
                } else {
                    message(result.error == Tmdb.Error.OFFLINE ? R.string.youtube_offline : R.string.youtube_failed,
                            R.string.action_retry, () -> refresh(true));
                }
                return;
            }
            showRail();
            List<ContentCard> cards = new ArrayList<>();
            for (YouTubeVideo video : result.videos) {
                cards.add(new ContentCard(ContentCard.Shape.VIDEO, video.title, video.channel, video.thumbnail, null, video.id));
            }
            rail.setCards(cards);
            notice.setText(result.stale ? getContext().getString(R.string.tmdb_stale,
                    DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(result.fetchedAt)))
                    : getContext().getString(R.string.youtube_attribution));
        });
    }

    // --- weather ----------------------------------------------------------------------------------------

    private void loadWeather(final int mine, boolean force) {
        if (!prefs.hasCity()) {
            message(R.string.weather_no_city, R.string.action_open_settings, () -> host.openSettings());
            return;
        }
        rail.setVisibility(GONE);
        box.setVisibility(GONE);
        weather.setVisibility(INVISIBLE);
        Weather.load(getContext(), force, result -> {
            if (mine != token) return;
            if (result.error != Weather.Error.NONE || result.forecast == null) {
                message(result.error == Weather.Error.OFFLINE ? R.string.weather_offline : R.string.weather_failed,
                        R.string.action_retry, () -> refresh(true));
                return;
            }
            box.setVisibility(GONE);
            rail.setVisibility(GONE);
            weather.setVisibility(VISIBLE);
            weather.show(result.forecast, result.stale, prefs.cityName());
        });
    }

    // --- states -----------------------------------------------------------------------------------------

    private void showRail() {
        box.setVisibility(GONE);
        weather.setVisibility(GONE);
        rail.setVisibility(VISIBLE);
    }

    private void message(int text, int action, final Runnable onAction) {
        rail.setVisibility(GONE);
        weather.setVisibility(GONE);
        boxText.setText(text);
        boxButton.setText(action);
        boxButton.setOnClickListener(view -> onAction.run());
        box.setVisibility(VISIBLE);
    }

    // --- listeners --------------------------------------------------------------------------------------

    @Override
    public void onOpen(ContentCard card) {
        if (card.videoId != null) {
            if (!YouTube.open(getContext(), card.videoId, selection.packageName)) {
                Toast.makeText(getContext(), R.string.toast_launch_failed, Toast.LENGTH_SHORT).show();
            }
        } else if (card.media != null) {
            host.openDetail(card.media,
                    selection.kind == Selection.Kind.STREAMING ? selection.packageName : null, selection.label);
        }
    }

    @Override
    public void onRefresh() {
        refresh(true);
    }

    @Override
    public boolean onUp() {
        host.focusHeader();
        return true;
    }

    @Override
    public boolean onDown() {
        host.focusDock();
        return true;
    }

    /** Hidden while the screen is not in front: nothing in the panel may keep working. */
    boolean hasContent() {
        return box.getVisibility() == VISIBLE || weather.getVisibility() == VISIBLE || rail.count() > 0;
    }

    View railView() {
        return rail;
    }
}
