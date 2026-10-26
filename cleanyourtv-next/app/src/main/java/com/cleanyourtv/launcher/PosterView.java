package com.cleanyourtv.launcher;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.widget.ImageView;

/** A poster: a rounded picture that grows slightly and gets a white ring on focus. Empty until loaded. */
final class PosterView extends ImageView {
    private static final float FOCUS_SCALE = 1.1f;

    final MediaItem item;

    PosterView(Context context, MediaItem item, int widthPx, int heightPx) {
        super(context);
        this.item = item;
        setScaleType(ScaleType.CENTER_CROP);
        setFocusable(true);
        setClickable(true);
        setDefaultFocusHighlightEnabled(false);
        setBackground(Ui.placeholder());
        float radius = Ui.dp(context, 14);
        Ui.clipRounded(this, radius);
        GradientDrawable ring = new GradientDrawable();
        ring.setCornerRadius(radius);
        ring.setStroke(Ui.dp(context, 3), 0xF2FFFFFF);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_focused}, ring);
        states.addState(new int[] {}, new android.graphics.drawable.ColorDrawable(0x00000000));
        setForeground(states);
        setLayoutParams(new android.widget.LinearLayout.LayoutParams(widthPx, heightPx));
        setOnFocusChangeListener((view, hasFocus) -> {
            float scale = hasFocus ? FOCUS_SCALE : 1f;
            view.animate().scaleX(scale).scaleY(scale).setDuration(160).start();
            view.setElevation(hasFocus ? Ui.dp(context, 10) : 0);
        });
    }
}
