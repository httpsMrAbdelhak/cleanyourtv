package com.cleanyourtv.launcher;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** A picture with its title and a line of detail beneath, focusable as one item. The ring wraps the picture. */
final class CardView extends LinearLayout {
    final ContentCard card;
    private final ImageView picture;

    CardView(Context context, ContentCard card, int widthPx, int heightPx) {
        super(context);
        this.card = card;
        setOrientation(VERTICAL);
        setFocusable(true);
        setClickable(true);
        setDefaultFocusHighlightEnabled(false);
        setClipChildren(false);

        float radius = Ui.dp(context, 16);
        FrameLayout frame = new FrameLayout(context);
        picture = new ImageView(context);
        picture.setScaleType(ImageView.ScaleType.CENTER_CROP);
        picture.setBackground(Ui.placeholder());
        Ui.clipRounded(picture, radius);
        frame.addView(picture, new FrameLayout.LayoutParams(widthPx, heightPx));

        GradientDrawable ring = new GradientDrawable();
        ring.setCornerRadius(radius);
        ring.setStroke(Ui.dp(context, 3), Palette.RING);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_focused}, ring);
        states.addState(new int[] {}, new ColorDrawable(0x00000000));
        frame.setForeground(states);
        frame.setDuplicateParentStateEnabled(true);
        addView(frame, new LayoutParams(widthPx, heightPx));

        TextView title = Ui.text(context, 15, Palette.TEXT, Ui.medium(context));
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setText(card.title);
        LayoutParams titleParams = new LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = Ui.dp(context, 8);
        addView(title, titleParams);

        TextView detail = Ui.text(context, 12, Palette.TEXT_DIM, Ui.regular(context));
        detail.setSingleLine(true);
        detail.setEllipsize(TextUtils.TruncateAt.END);
        detail.setText(card.subtitle);
        addView(detail, new LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT));

        setOnFocusChangeListener((view, hasFocus) -> {
            float scale = hasFocus ? 1.05f : 1f;
            view.animate().scaleX(scale).scaleY(scale).setDuration(160).start();
        });
    }

    ImageView picture() {
        return picture;
    }
}
