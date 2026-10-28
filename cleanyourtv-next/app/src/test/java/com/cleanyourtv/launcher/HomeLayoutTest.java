package com.cleanyourtv.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Runs the real home screen on a 1920x1080 display (960 by 540 dp at 2x, like the TV) and checks where the
 * three zones land: header on top, dock at the bottom, panel between. A layout bug that only a screenshot
 * would show (the dock at the top, the panel under it) fails here instead.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "w960dp-h540dp-xhdpi")
public class HomeLayoutTest {
    private static final int W = 1920;
    private static final int H = 1080;

    private static int topIn(View view, View root) {
        int top = 0;
        View at = view;
        while (at != root) {
            top += at.getTop();
            at = (View) at.getParent();
        }
        return top;
    }

    private static HomeScreen find(View view) {
        if (view instanceof HomeScreen) return (HomeScreen) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                HomeScreen found = find(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    @Test public void theDockSitsAtTheBottomAndTheHeaderAtTheTop() {
        Activity activity = Robolectric.buildActivity(HomeActivity.class).setup().get();
        View decor = activity.getWindow().getDecorView();
        decor.measure(View.MeasureSpec.makeMeasureSpec(W, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(H, View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, W, H);

        HomeScreen screen = find(decor);
        assertNotNull("the home screen is in the window", screen);
        screen.showApps(Arrays.asList(new AppEntry("YouTube", "com.google.android.youtube.tv"),
                new AppEntry("Netflix", "com.netflix.ninja"), new AppEntry("Nuvio", "dev.nuvio.android")));
        decor.measure(View.MeasureSpec.makeMeasureSpec(W, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(H, View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, W, H);

        View dock = screen.dockView();
        View title = screen.titleView();
        View panel = screen.panelView();
        int dockTop = topIn(dock, decor);
        int dockBottom = dockTop + dock.getHeight();
        int titleTop = topIn(title, decor);
        int panelTop = topIn(panel, decor);
        String where = "title top " + titleTop + ", panel top " + panelTop + ", dock " + dockTop + ".." + dockBottom
                + " of " + H + ", dock children " + ((ViewGroup) dock).getChildCount();

        assertTrue("the dock has its label and banners (" + where + ")", ((ViewGroup) dock).getChildCount() >= 3);
        assertTrue("the dock is in the lower fifth (" + where + ")", dockTop > H * 75 / 100);
        assertTrue("the dock ends inside the screen (" + where + ")", dockBottom <= H);
        assertTrue("the greeting is in the top fifth, under the top bar (" + where + ")", titleTop < H / 5 && titleTop > H / 20);
        assertTrue("the panel is below the title (" + where + ")", panelTop > titleTop);
        assertTrue("the panel starts above the dock (" + where + ")", panelTop < dockTop);
        assertEquals("the dock is centred", dock.getLeft(), W - dock.getRight(), 2);
    }
}
