package com.cleanyourtv.launcher;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * A dock tile: a wide rounded banner (the app's own TV banner when it has one) that lifts and gets a ring
 * when it has the focus. The ambient light and the panel above the dock follow the focus; they are driven by
 * the screen, not drawn here.
 */
final class BannerTile extends FrameLayout {
    private static final float FOCUS_SCALE = 1.14f;
    private static final int FOCUS_MS = 180;

    /** The app behind the tile, or null for the weather tile. */
    final String packageName;
    final String label;
    private final ImageView picture;

    BannerTile(Context context, String packageName, String label, int widthPx, int heightPx) {
        super(context);
        this.packageName = packageName;
        this.label = label;
        setFocusable(true);
        setClickable(true);
        setDefaultFocusHighlightEnabled(false);
        setClipChildren(false);
        setLayoutParams(new LayoutParams(widthPx, heightPx));

        float radius = heightPx * 0.2f;
        GradientDrawable ring = Ui.round(0x00000000, radius + Ui.dp(context, 3));
        ring.setStroke(Ui.dp(context, 3), Palette.RING);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_focused}, ring);
        states.addState(new int[] {}, Ui.round(0x00000000, radius));
        setBackground(states);

        int inset = Ui.dp(context, 2);
        picture = new ImageView(context);
        picture.setScaleType(ImageView.ScaleType.CENTER_CROP);
        picture.setBackground(Ui.round(Palette.PLACEHOLDER, radius));
        Ui.clipRounded(picture, radius);
        addView(picture, new LayoutParams(widthPx - 2 * inset, heightPx - 2 * inset, Gravity.CENTER));

        setOnFocusChangeListener((view, hasFocus) -> {
            float scale = hasFocus ? FOCUS_SCALE : 1f;
            view.animate().scaleX(scale).scaleY(scale).setDuration(FOCUS_MS).start();
            view.setElevation(hasFocus ? Ui.dp(context, 10) : 0);
        });
    }

    ImageView picture() {
        return picture;
    }
}
