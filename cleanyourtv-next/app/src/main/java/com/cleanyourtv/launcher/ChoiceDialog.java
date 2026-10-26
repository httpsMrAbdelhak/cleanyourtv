package com.cleanyourtv.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** A dark glass list of choices, driven by the D-pad. Back closes it; OK picks the focused line. */
final class ChoiceDialog {
    private final Activity activity;
    private final Dialog dialog;
    private final LinearLayout box;

    ChoiceDialog(Activity activity, CharSequence title) {
        this.activity = activity;
        dialog = new Dialog(activity, R.style.Theme_CleanYourTV_Dialog);
        box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(activity, 26);
        box.setPadding(pad, pad, pad, pad);
        GradientDrawable panel = Ui.round(Palette.DIALOG, Ui.dp(activity, 30));
        panel.setStroke(Ui.dp(activity, 1), 0x40808080);
        box.setBackground(panel);
        TextView heading = Ui.text(activity, 24, Palette.TEXT, Ui.medium(activity));
        heading.setText(title);
        heading.setPadding(Ui.dp(activity, 4), 0, 0, Ui.dp(activity, 14));
        box.addView(heading);
    }

    /** A view above the choices, for instance a text field. */
    ChoiceDialog header(View view) {
        box.addView(view, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return this;
    }

    ChoiceDialog option(CharSequence label, final Runnable action) {
        TextView line = Ui.text(activity, 19, Palette.TEXT, Ui.regular(activity));
        line.setText(label);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setFocusable(true);
        line.setClickable(true);
        line.setDefaultFocusHighlightEnabled(false);
        int padX = Ui.dp(activity, 20);
        int padY = Ui.dp(activity, 14);
        line.setPadding(padX, padY, padX, padY);
        line.setBackground(Ui.focusGlass(activity, 0x00000000, Palette.TILE_FOCUSED, Ui.dp(activity, 16)));
        line.setOnClickListener(view -> {
            dialog.dismiss();
            action.run();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Ui.dp(activity, 4);
        box.addView(line, params);
        return this;
    }

    void dismiss() {
        dialog.dismiss();
    }

    void show() {
        dialog.setContentView(box, new ViewGroup.LayoutParams(Ui.dp(activity, 560), ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.show();
        for (int i = 0; i < box.getChildCount(); i++) {
            if (box.getChildAt(i).isFocusable()) {
                box.getChildAt(i).requestFocus();
                break;
            }
        }
    }
}
