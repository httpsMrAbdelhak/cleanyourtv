package com.cleanyourtv.launcher;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.widget.HorizontalScrollView;

/**
 * A horizontal strip that follows the focus with a smooth glide instead of the framework's instant jump:
 * the focused item is brought to the left third of the strip, so the next ones are always in view.
 */
class RailScroll extends HorizontalScrollView {
    RailScroll(Context context) {
        super(context);
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setClipChildren(false);
        setClipToPadding(false);
    }

    /** Turns off the instant jump; reveal() does the scrolling. */
    @Override
    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        return 0;
    }

    void reveal(View item) {
        int target = item.getLeft() - getWidth() / 5;
        View content = getChildAt(0);
        int max = content == null ? 0 : Math.max(0, content.getWidth() - getWidth());
        smoothScrollTo(Math.max(0, Math.min(target, max)), 0);
    }
}
