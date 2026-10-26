package com.cleanyourtv.launcher;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** An icon over a label, the building block of the dock and the app library. Focus grows it slightly. */
final class TileView extends LinearLayout {
    private static final float FOCUS_SCALE = 1.08f;
    private static final int FOCUS_MS = 140;

    private final ImageView icon;
    private final TextView label;

    TileView(Context context, int widthPx, int heightPx, int iconPx, int accent) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setFocusable(true);
        setClickable(true);
        setDefaultFocusHighlightEnabled(false);
        int pad = Ui.dp(context, 8);
        setPadding(pad, pad, pad, pad);
        setBackground(Ui.focusable(context, Palette.TILE, Palette.TILE_FOCUSED, accent, Ui.dp(context, 20)));
        setLayoutParams(new LayoutParams(widthPx, heightPx));

        icon = new ImageView(context);
        icon.setLayoutParams(new LayoutParams(iconPx, iconPx));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        addView(icon);

        label = Ui.text(context, 13, Palette.TEXT, null);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER);
        LayoutParams labelParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = Ui.dp(context, 6);
        label.setLayoutParams(labelParams);
        addView(label);

        setOnFocusChangeListener((view, hasFocus) -> {
            float scale = hasFocus ? FOCUS_SCALE : 1f;
            view.animate().scaleX(scale).scaleY(scale).setDuration(FOCUS_MS).start();
        });
    }

    ImageView iconView() {
        return icon;
    }

    void setLabel(CharSequence text) {
        label.setText(text);
    }

    /** Gives the focus animation room: the parents must not clip a tile that grows past its cell. */
    static void allowOverflow(View parent) {
        if (parent instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) parent).setClipChildren(false);
            ((android.view.ViewGroup) parent).setClipToPadding(false);
        }
    }
}
