package com.cleanyourtv.launcher;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A card of the shelf. Resting, it is a tall poster with the title underneath. Selected, it widens to landscape
 * art (the poster stays underneath until the wide picture has loaded), its title grows bolder and a line of
 * detail appears ("Resume · S38E01 · 20 min left · ends at 17:58"). The height never changes, so the row
 * does not jump; only this card's width animates. A started title has a progress bar, an episode has its
 * number in a badge.
 */
final class CardView extends LinearLayout {
    private static final int GROW_MS = 220;

    final TvEntry entry;
    final int narrowWidth;
    final int wideWidth;
    private final FrameLayout frame;
    private final ImageView poster;
    private final ImageView wide;
    private final TextView title;
    private final TextView meta;
    private final ValueAnimator grow = ValueAnimator.ofFloat(0f, 1f);
    private float expanded;

    CardView(Context context, TvEntry entry, int narrowWidth, int wideWidth, int height) {
        super(context);
        this.entry = entry;
        this.narrowWidth = narrowWidth;
        this.wideWidth = wideWidth;
        setOrientation(VERTICAL);
        setFocusable(true);
        setClickable(true);
        setDefaultFocusHighlightEnabled(false);
        setClipChildren(false);

        float radius = Ui.dp(context, 14);
        frame = new FrameLayout(context);
        frame.setBackground(Ui.round(Palette.PLACEHOLDER, radius));
        Ui.clipRounded(frame, radius);
        poster = new ImageView(context);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(poster, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        wide = new ImageView(context);
        wide.setScaleType(ImageView.ScaleType.CENTER_CROP);
        wide.setAlpha(0f);
        frame.addView(wide, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (!entry.badge.isEmpty()) {
            TextView badge = Ui.text(context, 11, 0xFFFFFFFF, Ui.semibold(context));
            badge.setText(entry.badge);
            int padX = Ui.dp(context, 7);
            int padY = Ui.dp(context, 3);
            badge.setPadding(padX, padY, padX, padY);
            badge.setBackground(Ui.round(0x99101420, Ui.dp(context, 7)));
            FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
            badgeParams.setMargins(Ui.dp(context, 8), Ui.dp(context, 8), 0, 0);
            frame.addView(badge, badgeParams);
        }
        if (entry.progress >= 0f) {
            int barH = Ui.dp(context, 3);
            FrameLayout track = new FrameLayout(context);
            track.setBackgroundColor(0x66FFFFFF);
            View bar = new View(context);
            bar.setBackgroundColor(0xFFFFFFFF);
            track.addView(bar, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, barH));
            bar.setPivotX(0f);
            bar.setScaleX(Math.max(0.03f, entry.progress));
            FrameLayout.LayoutParams trackParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, barH, Gravity.BOTTOM);
            int inset = Ui.dp(context, 10);
            trackParams.setMargins(inset, 0, inset, Ui.dp(context, 8));
            frame.addView(track, trackParams);
            Ui.clipRounded(track, barH / 2f);
        }
        GradientDrawable ring = new GradientDrawable();
        ring.setCornerRadius(radius);
        ring.setStroke(Ui.dp(context, 2), Palette.RING);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_focused}, ring);
        states.addState(new int[] {}, new ColorDrawable(0x00000000));
        frame.setForeground(states);
        frame.setDuplicateParentStateEnabled(true);
        addView(frame, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));

        title = Ui.text(context, 14, Palette.TEXT, Ui.medium(context));
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setText(entry.title);
        LayoutParams titleParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = Ui.dp(context, 8);
        addView(title, titleParams);

        meta = Ui.text(context, 13, Palette.TEXT_DIM, Ui.regular(context));
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        meta.setAlpha(0f);
        meta.setText(" ");
        addView(meta, new LayoutParams(wideWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

        grow.setDuration(GROW_MS);
        grow.setInterpolator(new DecelerateInterpolator(1.5f));
        grow.addUpdateListener(a -> applyWidth((float) a.getAnimatedValue()));
    }

    ImageView poster() {
        return poster;
    }

    ImageView wide() {
        return wide;
    }

    /** Widens (selected) or narrows the card. detail: the line shown under the title while it is wide. */
    void setSelectedLook(boolean selected, String detail) {
        grow.cancel();
        float from = expanded;
        float to = selected ? 1f : 0f;
        grow.setFloatValues(from, to);
        grow.start();
        title.setTypeface(selected ? Ui.semibold(getContext()) : Ui.medium(getContext()));
        title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, (selected ? 17 : 14) * Ui.unit(getContext()));
        if (selected) meta.setText(detail);
        meta.animate().alpha(selected ? 1f : 0f).setDuration(GROW_MS).start();
        wide.animate().alpha(selected && wide.getDrawable() != null ? 1f : 0f).setDuration(GROW_MS).start();
    }

    /** The wide picture has arrived: show it if the card is still wide. */
    void onWideLoaded() {
        if (expanded > 0.5f && isFocused()) wide.animate().alpha(1f).setDuration(GROW_MS).start();
    }

    private void applyWidth(float t) {
        expanded = t;
        ViewGroup.LayoutParams params = getLayoutParams();
        if (params == null) return;
        params.width = Math.round(narrowWidth + (wideWidth - narrowWidth) * t);
        setLayoutParams(params);
    }
}
