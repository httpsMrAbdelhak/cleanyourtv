package com.cleanyourtv.launcher;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * A dock icon: a rounded app icon that grows a little and gets a white ring when it has the focus. The
 * label and the ambient light are driven by the focus listener of the screen, not drawn here.
 */
final class DockTile extends FrameLayout {
    private static final float FOCUS_SCALE = 1.18f;
    private static final int FOCUS_MS = 180;

    final String packageName;
    final String label;
    private final ImageView icon;

    /** packageName is null for the "all apps" tile. */
    DockTile(Context context, String packageName, String label, int sizePx) {
        super(context);
        this.packageName = packageName;
        this.label = label;
        setFocusable(true);
        setClickable(true);
        setDefaultFocusHighlightEnabled(false);
        setClipChildren(false);

        float radius = sizePx * 0.24f;
        GradientDrawable ring = Ui.round(0x1FFFFFFF, radius);
        ring.setStroke(Ui.dp(context, 3), 0xF2FFFFFF);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_focused}, ring);
        states.addState(new int[] {}, Ui.round(0x00000000, radius));
        setBackground(states);

        int inset = Ui.dp(context, 5);
        icon = new ImageView(context);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Ui.clipRounded(icon, (sizePx - 2 * inset) * 0.22f);
        LayoutParams params = new LayoutParams(sizePx - 2 * inset, sizePx - 2 * inset, Gravity.CENTER);
        addView(icon, params);

        setOnFocusChangeListener((view, hasFocus) -> {
            float scale = hasFocus ? FOCUS_SCALE : 1f;
            view.animate().scaleX(scale).scaleY(scale).setDuration(FOCUS_MS).start();
            view.setElevation(hasFocus ? Ui.dp(context, 8) : 0);
        });
    }

    ImageView iconView() {
        return icon;
    }
}
