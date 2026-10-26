package com.cleanyourtv.launcher;

import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URISyntaxException;
import java.util.List;

/**
 * The panel above the dock. It shows what the chosen streaming app (Nuvio, say) publishes for the Android TV
 * home screen: its "Continue watching" and its channels, with the tabs Explore, Movies and Series filtering
 * them. The panel keeps showing it whichever dock tile has the focus; only the weather tile swaps it for the
 * weather. Every state without data (no app, no permission, nothing published, error) is a plain message
 * with one button, never a made-up list. A late answer for a request already replaced is ignored (the token).
 */
final class ContentPanel extends FrameLayout implements CardRail.Listener, WeatherPanel.Listener {
    /** What the dock selection asks the panel to show. */
    static final class Selection {
        enum Kind { SOURCE, WEATHER }

        final Kind kind;

        Selection(Kind kind) {
            this.kind = kind;
        }
    }

    /** What the panel asks of the screen around it. */
    interface Host {
        void focusHeader();

        void focusDock();

        void openSettings();

        /** Asks Android for the READ_TV_LISTINGS permission. */
        void requestTvPermission();
    }

    private final Host host;
    private final Prefs prefs;
    private final TextView heading;
    private final CardRail rail;
    private final WeatherPanel weather;
    private final LinearLayout box;
    private final TextView boxText;
    private final TextView boxButton;

    private Selection selection = new Selection(Selection.Kind.SOURCE);
    private String sourcePackage;
    private String sourceLabel = "";
    private List<TvEntry> all = new java.util.ArrayList<>();
    private int tab = EntryFilter.EXPLORE;
    private int token;
    private boolean shown;
    /** False until the screen has looked at the installed apps: until then, "no app" would be a false alarm. */
    private boolean sourceKnown;

    ContentPanel(Context context, Host host, Prefs prefs) {
        super(context);
        this.host = host;
        this.prefs = prefs;
        setClipChildren(false);
        setClipToPadding(false);

        heading = Ui.text(context, 16, Palette.TEXT_DIM, Ui.medium(context));
        LayoutParams headingParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        headingParams.leftMargin = Ui.dp(context, 8);
        addView(heading, headingParams);

        rail = new CardRail(context, prefs, this);
        LayoutParams railParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP);
        railParams.topMargin = Ui.dp(context, 20);
        addView(rail, railParams);

        weather = new WeatherPanel(context, prefs, this);
        weather.setVisibility(GONE);
        addView(weather, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setVisibility(GONE);
        int pad = Ui.dp(context, 26);
        box.setPadding(pad, pad, pad, pad);
        box.setBackground(new GlassDrawable(Ui.dp(context, 28), Ui.dp(context, 1.5f), true, false, prefs.effects()));
        boxText = Ui.text(context, 17, Palette.TEXT, Ui.regular(context));
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
        box.addView(boxText, new LinearLayout.LayoutParams(Ui.dp(context, 520), ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = Ui.dp(context, 16);
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

    /** The app whose home-screen content is shown (null when none is chosen yet). */
    void setSource(String packageName, String label) {
        boolean changed = !sourceKnown || (packageName == null ? sourcePackage != null : !packageName.equals(sourcePackage));
        sourceKnown = true;
        sourcePackage = packageName;
        sourceLabel = label == null ? "" : label;
        if (changed && shown && selection.kind == Selection.Kind.SOURCE) refresh(false);
    }

    /** The dock selection changed. Only the weather tile (and coming back from it) changes the panel. */
    void select(Selection next) {
        if (next.kind == selection.kind) return;
        selection = next;
        if (shown) refresh(false);
    }

    void selectTab(int next) {
        if (next == tab) return;
        tab = next;
        if (shown && selection.kind == Selection.Kind.SOURCE) showEntries();
    }

    /** Whether the header tabs mean something now (not while the weather is shown). */
    boolean usesTabs() {
        return selection.kind == Selection.Kind.SOURCE;
    }

    void onLookChanged() {
        rail.refreshImages();
    }

    /** The system answered the permission question: look again. */
    void onPermissionResult() {
        if (shown) refresh(true);
    }

    /** Puts the focus on the first card (or the message button). False when there is nothing to focus. */
    boolean enter() {
        if (box.getVisibility() == VISIBLE) return boxButton.requestFocus();
        if (weather.getVisibility() == VISIBLE) return weather.focusCard();
        return rail.focusFirst();
    }

    private void refresh(boolean force) {
        final int mine = ++token;
        if (selection.kind == Selection.Kind.WEATHER) loadWeather(mine, force);
        else loadEntries(mine);
    }

    // --- the app's own content --------------------------------------------------------------------------

    private void loadEntries(final int mine) {
        heading.setText("");
        if (!sourceKnown) {
            showRail();
            rail.showLoading();
            return;
        }
        if (sourcePackage == null) {
            message(R.string.source_none, R.string.action_open_settings, () -> host.openSettings());
            return;
        }
        showRail();
        rail.showLoading();
        TvChannels.load(getContext(), sourcePackage, getContext().getString(R.string.section_continue), result -> {
            if (mine != token) return;
            onChannels(result);
        });
    }

    private void onChannels(TvChannels.Result result) {
        switch (result.status) {
            case NO_PERMISSION:
                message(getContext().getString(R.string.source_no_permission, sourceLabel), R.string.action_allow,
                        () -> host.requestTvPermission());
                return;
            case NOTHING:
                message(getContext().getString(R.string.source_nothing, sourceLabel), R.string.action_retry, () -> refresh(true));
                return;
            case FAILED:
                message(getContext().getString(R.string.source_failed, sourceLabel), R.string.action_retry, () -> refresh(true));
                return;
            case NO_SOURCE:
                message(R.string.source_none, R.string.action_open_settings, () -> host.openSettings());
                return;
            default:
                all = result.entries;
                showEntries();
        }
    }

    private void showEntries() {
        List<TvEntry> shownEntries = EntryFilter.apply(all, tab);
        if (shownEntries.isEmpty()) {
            message(getContext().getString(R.string.source_tab_empty, sourceLabel), R.string.action_retry, () -> refresh(true));
            return;
        }
        showRail();
        rail.setEntries(shownEntries);
        heading.setText(shownEntries.get(0).section);
    }

    // --- weather ----------------------------------------------------------------------------------------

    private void loadWeather(final int mine, boolean force) {
        heading.setText("");
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

    private void message(int text, int action, Runnable onAction) {
        message(getContext().getString(text), action, onAction);
    }

    private void message(String text, int action, final Runnable onAction) {
        rail.setVisibility(GONE);
        weather.setVisibility(GONE);
        heading.setText("");
        boxText.setText(text);
        boxButton.setText(action);
        boxButton.setOnClickListener(view -> onAction.run());
        box.setVisibility(VISIBLE);
    }

    // --- listeners --------------------------------------------------------------------------------------

    @Override
    public void onOpen(TvEntry entry) {
        Context context = getContext();
        Intent intent = null;
        if (!entry.intentUri.isEmpty()) {
            try {
                intent = Intent.parseUri(entry.intentUri, Intent.URI_INTENT_SCHEME);
            } catch (URISyntaxException e) {
                intent = null;
            }
        }
        if (intent != null) {
            String component = intent.getComponent() == null ? null : intent.getComponent().getPackageName();
            // The app chose this intent, so it must stay inside that app; a selector could smuggle another target.
            if (IntentGuard.allowed(sourcePackage, component, intent.getPackage()) && intent.getSelector() == null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(intent);
                    return;
                } catch (RuntimeException e) {
                    // fall through: open the app itself
                }
            }
        }
        if (sourcePackage == null || !AppRepository.launch(context, sourcePackage)) {
            Toast.makeText(context, R.string.toast_launch_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFocused(TvEntry entry) {
        heading.setText(entry.section);
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
}
