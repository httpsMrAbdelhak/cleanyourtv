package com.cleanyourtv.launcher;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** An icon over a label on flat glass, for the app library. Focus lights the glass and grows it slightly. */
final class TileView extends LinearLayout {
    private static final float FOCUS_SCALE = 1.08f;
    private static final int FOCUS_MS = 140;

    private final ImageView icon;
    private final TextView label;

    TileView(Context context, int widthPx, int heightPx, int iconPx) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setFocusable(true);
        setClickable(true);
        setDefaultFocusHighlightEnabled(false);
        int pad = Ui.dp(context, 8);
        setPadding(pad, pad, pad, pad);
        setBackground(Ui.focusGlass(context, Palette.TILE, Palette.TILE_FOCUSED, Ui.dp(context, 22)));
        setLayoutParams(new LayoutParams(widthPx, heightPx));

        icon = new ImageView(context);
        icon.setLayoutParams(new LayoutParams(iconPx, iconPx));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Ui.clipRounded(icon, iconPx * 0.22f);
        addView(icon);

        label = Ui.text(context, 14, Palette.TEXT, Ui.regular(context));
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER);
        LayoutParams labelParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = Ui.dp(context, 8);
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
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).setClipChildren(false);
            ((ViewGroup) parent).setClipToPadding(false);
        }
    }
}
