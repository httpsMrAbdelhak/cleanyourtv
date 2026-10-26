package com.cleanyourtv.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** The favourite apps in the dock, in order. Pure logic: no Android classes, so it runs in plain JVM tests. */
final class DockModel {
    /** Six apps plus the two fixed tiles fit a 1080p screen at 48 dp margins. */
    static final int MAX = 6;

    private static final Pattern PACKAGE = Pattern.compile("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*");

    private final ArrayList<String> items = new ArrayList<>();

    DockModel(List<String> initial) {
        for (String packageName : initial) add(packageName);
    }

    /** Reads what {@link #serialize()} wrote; anything that is not a package name is dropped. */
    static DockModel parse(String raw) {
        List<String> found = new ArrayList<>();
        if (raw != null) {
            for (String line : raw.split("\n")) found.add(line.trim());
        }
        return new DockModel(found);
    }

    String serialize() {
        StringBuilder out = new StringBuilder();
        for (String packageName : items) {
            if (out.length() > 0) out.append('\n');
            out.append(packageName);
        }
        return out.toString();
    }

    List<String> items() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    /** The saved apps that are still installed, in dock order. The saved list itself is not changed. */
    List<String> visible(Set<String> installed) {
        List<String> out = new ArrayList<>();
        for (String packageName : items) {
            if (installed.contains(packageName)) out.add(packageName);
        }
        return out;
    }

    boolean contains(String packageName) {
        return items.contains(packageName);
    }

    boolean isFull() {
        return items.size() >= MAX;
    }

    boolean add(String packageName) {
        if (packageName == null || !PACKAGE.matcher(packageName).matches()) return false;
        if (isFull() || contains(packageName)) return false;
        items.add(packageName);
        return true;
    }

    boolean remove(String packageName) {
        return items.remove(packageName);
    }

    /**
     * Swaps the app with its nearest neighbour on the left that is still installed, so one press always
     * moves something you can see. Saved apps that were uninstalled keep their place.
     */
    boolean moveLeft(String packageName, Set<String> installed) {
        return move(packageName, -1, installed);
    }

    boolean moveRight(String packageName, Set<String> installed) {
        return move(packageName, 1, installed);
    }

    private boolean move(String packageName, int direction, Set<String> installed) {
        int at = items.indexOf(packageName);
        if (at < 0) return false;
        int to = at + direction;
        while (to >= 0 && to < items.size() && !installed.contains(items.get(to))) to += direction;
        if (to < 0 || to >= items.size()) return false;
        Collections.swap(items, at, to);
        return true;
    }
}
