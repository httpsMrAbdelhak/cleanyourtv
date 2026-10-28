package com.cleanyourtv.launcher;

import android.content.Context;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** A plain message with one button, on glass: what the screen shows instead of a list it does not have. */
final class MessageBox extends LinearLayout {
    interface Keys {
        boolean onUp();

        boolean onDown();
    }

    private final TextView text;
    private final TextView button;

    MessageBox(Context context, Prefs prefs, final Keys keys) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = Ui.dp(context, 24);
        setPadding(pad, pad, pad, pad);
        setBackground(new GlassDrawable(Ui.dp(context, 26), Ui.dp(context, 1.5f), true, false, prefs.effects()));
        text = Ui.text(context, 17, Palette.TEXT, Ui.regular(context));
        text.setGravity(Gravity.CENTER);
        text.setLineSpacing(0, 1.15f);
        button = Ui.text(context, 17, Palette.TEXT, Ui.semibold(context));
        button.setGravity(Gravity.CENTER);
        button.setPadding(Ui.dp(context, 26), Ui.dp(context, 10), Ui.dp(context, 26), Ui.dp(context, 10));
        button.setFocusable(true);
        button.setClickable(true);
        button.setDefaultFocusHighlightEnabled(false);
        button.setBackground(Ui.focusGlass(context, Palette.TILE, Palette.TILE_FOCUSED, Ui.dp(context, 22)));
        button.setOnKeyListener((view, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) return keys.onUp();
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) return keys.onDown();
            return keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT;
        });
        addView(text, new LayoutParams(Ui.dp(context, 520), ViewGroup.LayoutParams.WRAP_CONTENT));
        LayoutParams buttonParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = Ui.dp(context, 14);
        addView(button, buttonParams);
    }

    void show(String message, String action, final Runnable onAction) {
        text.setText(message);
        button.setVisibility(action == null ? GONE : VISIBLE);
        if (action != null) {
            button.setText(action);
            button.setOnClickListener(view -> onAction.run());
        }
        setVisibility(VISIBLE);
    }

    boolean focusButton() {
        return button.getVisibility() == VISIBLE && button.requestFocus();
    }
}
