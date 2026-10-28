package com.cleanyourtv.launcher;

import android.content.Context;
import android.graphics.Rect;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Library page: every row the streaming app published, one under the other (continue watching, my list,
 * then its channels). Up and Down move between rows and the page glides to keep the selected row near the top;
 * Up on the first row goes to the top bar, Down on the last one to the dock.
 */
final class LibraryShelf extends ScrollView {
    interface Host {
        void onOpen(TvEntry entry);

        String detail(TvEntry entry);

        void focusTopBar();

        void focusDock();
    }

    private final LinearLayout column;
    private final List<CardRail> rails = new ArrayList<>();
    private final List<TextView> titles = new ArrayList<>();
    private final Prefs prefs;
    private final Host host;

    LibraryShelf(Context context, Prefs prefs, Host host) {
        super(context);
        this.prefs = prefs;
        this.host = host;
        setVerticalScrollBarEnabled(false);
        setClipChildren(false);
        setClipToPadding(false);
        setVerticalFadingEdgeEnabled(true);
        setFadingEdgeLength(Ui.dp(context, 18));
        column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setClipChildren(false);
        column.setPadding(0, 0, 0, Ui.dp(context, 120));
        addView(column);
    }

    /** The framework's instant scroll is off: the page glides to the selected row instead. */
    @Override
    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        return 0;
    }

    void setSections(Map<String, List<TvEntry>> sections) {
        releaseImages();
        column.removeAllViews();
        rails.clear();
        titles.clear();
        Context context = getContext();
        for (Map.Entry<String, List<TvEntry>> section : sections.entrySet()) {
            final int index = rails.size();
            TextView title = Ui.text(context, 18, Palette.TEXT, Ui.semibold(context));
            title.setText(section.getKey());
            title.setPadding(0, Ui.dp(context, index == 0 ? 0 : 10), 0, 0);
            column.addView(title);
            CardRail rail = new CardRail(context, prefs, new CardRail.Listener() {
                @Override public void onOpen(TvEntry entry) {
                    host.onOpen(entry);
                }

                @Override public String detail(TvEntry entry) {
                    return host.detail(entry);
                }

                @Override public boolean onUp() {
                    if (index == 0) host.focusTopBar();
                    else rails.get(index - 1).focusFirst();
                    return true;
                }

                @Override public boolean onDown() {
                    if (index == rails.size() - 1) host.focusDock();
                    else rails.get(index + 1).focusFirst();
                    return true;
                }

                @Override public void onEntered(CardRail which) {
                    smoothScrollTo(0, Math.max(0, titles.get(index).getTop()));
                }
            });
            rail.setEntries(section.getValue());
            column.addView(rail, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            rails.add(rail);
            titles.add(title);
        }
        scrollTo(0, 0);
    }

    boolean enter() {
        return !rails.isEmpty() && rails.get(0).focusFirst();
    }

    void releaseImages() {
        for (CardRail rail : rails) rail.releaseImages();
    }
}
