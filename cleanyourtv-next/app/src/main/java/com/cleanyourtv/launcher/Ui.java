package com.cleanyourtv.launcher;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.widget.TextView;

/** Small view helpers shared by the three screens. Everything is built in code: no layout XML to keep in sync. */
final class Ui {
    private Ui() { }

    static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable round(int color, float radiusPx) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusPx);
        return drawable;
    }

    /** Resting look, and a brighter one with an accent outline while the D-pad focus is on the view. */
    static StateListDrawable focusable(Context context, int normal, int focused, int accent, float radiusPx) {
        GradientDrawable lit = round(focused, radiusPx);
        lit.setStroke(dp(context, 3), accent);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_focused}, lit);
        states.addState(new int[] {android.R.attr.state_pressed}, lit);
        states.addState(new int[] {}, round(normal, radiusPx));
        return states;
    }

    static Typeface light() {
        return Typeface.create("sans-serif-light", Typeface.NORMAL);
    }

    static TextView text(Context context, float sp, int color, Typeface face) {
        TextView view = new TextView(context);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(color);
        if (face != null) view.setTypeface(face);
        return view;
    }
}
