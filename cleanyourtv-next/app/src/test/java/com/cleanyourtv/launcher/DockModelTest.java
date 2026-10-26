package com.cleanyourtv.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class DockModelTest {
    private static Set<String> set(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    @Test public void addKeepsOrderAndRejectsDuplicates() {
        DockModel dock = new DockModel(Collections.<String>emptyList());
        assertTrue(dock.add("com.a"));
        assertTrue(dock.add("com.b"));
        assertFalse(dock.add("com.a"));
        assertEquals(Arrays.asList("com.a", "com.b"), dock.items());
    }

    @Test public void dockStopsAtMax() {
        DockModel dock = new DockModel(Collections.<String>emptyList());
        for (int i = 0; i < DockModel.MAX; i++) assertTrue(dock.add("app.n" + i));
        assertTrue(dock.isFull());
        assertFalse(dock.add("app.extra"));
        assertEquals(DockModel.MAX, dock.items().size());
    }

    @Test public void rejectsNamesThatAreNotPackages() {
        DockModel dock = new DockModel(Collections.<String>emptyList());
        assertFalse(dock.add(null));
        assertFalse(dock.add(""));
        assertFalse(dock.add("has space"));
        assertFalse(dock.add("line\nbreak"));
        assertFalse(dock.add("../../etc"));
        assertTrue(dock.items().isEmpty());
    }

    @Test public void serializeAndParseRoundTrip() {
        DockModel dock = new DockModel(Arrays.asList("com.a", "org.b.c", "tv.d"));
        assertEquals(dock.items(), DockModel.parse(dock.serialize()).items());
    }

    @Test public void parseToleratesJunk() {
        assertTrue(DockModel.parse(null).items().isEmpty());
        assertTrue(DockModel.parse("").items().isEmpty());
        assertEquals(Arrays.asList("com.a", "com.b"), DockModel.parse("com.a\n\n bad name \ncom.b\ncom.a").items());
    }

    @Test public void removeWorks() {
        DockModel dock = new DockModel(Arrays.asList("com.a", "com.b"));
        assertTrue(dock.remove("com.a"));
        assertFalse(dock.remove("com.a"));
        assertEquals(Collections.singletonList("com.b"), dock.items());
    }

    @Test public void moveSwapsWithTheVisibleNeighbour() {
        DockModel dock = new DockModel(Arrays.asList("com.a", "com.b", "com.c"));
        Set<String> all = set("com.a", "com.b", "com.c");
        assertTrue(dock.moveLeft("com.c", all));
        assertEquals(Arrays.asList("com.a", "com.c", "com.b"), dock.items());
        assertTrue(dock.moveRight("com.a", all));
        assertEquals(Arrays.asList("com.c", "com.a", "com.b"), dock.items());
    }

    @Test public void moveStopsAtTheEnds() {
        DockModel dock = new DockModel(Arrays.asList("com.a", "com.b"));
        Set<String> all = set("com.a", "com.b");
        assertFalse(dock.moveLeft("com.a", all));
        assertFalse(dock.moveRight("com.b", all));
        assertFalse(dock.moveLeft("com.unknown", all));
    }

    @Test public void moveSkipsUninstalledApps() {
        DockModel dock = new DockModel(Arrays.asList("com.a", "com.gone", "com.c"));
        Set<String> installed = set("com.a", "com.c");
        assertTrue(dock.moveLeft("com.c", installed));
        assertEquals(Arrays.asList("com.c", "com.gone", "com.a"), dock.items());
        assertFalse(dock.moveLeft("com.c", installed));
    }

    @Test public void visibleHidesUninstalledAppsButKeepsThem() {
        DockModel dock = new DockModel(Arrays.asList("com.a", "com.gone", "com.c"));
        assertEquals(Arrays.asList("com.a", "com.c"), dock.visible(set("com.a", "com.c")));
        assertEquals(3, dock.items().size());
    }

    @Test public void itemsIsACopy() {
        DockModel dock = new DockModel(Arrays.asList("com.a"));
        try {
            dock.items().add("com.b");
        } catch (UnsupportedOperationException expected) {
            // fine
        }
        assertEquals(1, dock.items().size());
    }
}
