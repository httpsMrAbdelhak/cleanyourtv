package com.cleanyourtv.launcher;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.TextView;

/** View helpers shared by every screen. Everything is built in code: no layout XML to keep in sync. */
final class Ui {
    private Ui() { }

    /**
     * One design unit: the screen is laid out as 960 units wide whatever its density, so the interface looks
     * the same on a TV set to 320 dpi and on one set to 213 dpi. Sizes below are in these units.
     */
    static float unit(Context context) {
        return Math.max(0.5f, context.getResources().getDisplayMetrics().widthPixels / 960f);
    }

    static int dp(Context context, float value) {
        return Math.round(value * unit(context));
    }

    static GradientDrawable round(int color, float radiusPx) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusPx);
        return drawable;
    }

    /** Fonts are loaded once: a variable font rebuilt per view is slow on a TV CPU. */
    private static Typeface light;
    private static Typeface regular;
    private static Typeface medium;

    static synchronized Typeface light(Context context) {
        if (light == null) light = context.getResources().getFont(R.font.outfit_light);
        return light;
    }

    static synchronized Typeface regular(Context context) {
        if (regular == null) regular = context.getResources().getFont(R.font.outfit_regular);
        return regular;
    }

    static synchronized Typeface medium(Context context) {
        if (medium == null) medium = context.getResources().getFont(R.font.outfit_medium);
        return medium;
    }

    static TextView text(Context context, float sp, int color, Typeface face) {
        TextView view = new TextView(context);
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp * unit(context));
        view.setTextColor(color);
        if (face != null) view.setTypeface(face);
        view.setIncludeFontPadding(false);
        return view;
    }

    /**
     * A flat glass look that brightens with focus, for rows and tiles inside scrolling lists. The focused
     * state carries a clear white ring, so the selection stays visible whatever the effects level.
     */
    static StateListDrawable focusGlass(Context context, int normal, int focused, float radiusPx) {
        GradientDrawable lit = round(focused, radiusPx);
        lit.setStroke(dp(context, 2), Palette.RING);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_focused}, lit);
        states.addState(new int[] {android.R.attr.state_pressed}, lit);
        states.addState(new int[] {}, round(normal, radiusPx));
        return states;
    }

    /** Glass that swaps to its bright version on focus. */
    static StateListDrawable focusGlass(Context context, float radiusPx, boolean sample, Effects effects) {
        float stroke = dp(context, 1.5f);
        StateListDrawable states = new StateListDrawable();
        GradientDrawable ring = round(0x00000000, radiusPx);
        ring.setStroke(dp(context, 2), Palette.RING);
        states.addState(new int[] {android.R.attr.state_focused},
                new android.graphics.drawable.LayerDrawable(new Drawable[] {
                        new GlassDrawable(radiusPx, stroke, sample, true, effects), ring}));
        states.addState(new int[] {}, new GlassDrawable(radiusPx, stroke, sample, false, effects));
        return states;
    }

    /** Clips a view's content to a rounded rectangle, cheaply (the GPU does it). */
    static void clipRounded(View view, final float radiusPx) {
        view.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline outline) {
                outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radiusPx);
            }
        });
        view.setClipToOutline(true);
    }

    static Drawable placeholder() {
        return new ColorDrawable(Palette.PLACEHOLDER);
    }
}
