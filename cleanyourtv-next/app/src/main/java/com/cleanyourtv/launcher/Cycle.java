package com.cleanyourtv.launcher;

/** Index arithmetic for the settings rows that step through a short list with Left, Right or OK. */
final class Cycle {
    private Cycle() { }

    /** index moved by dir (+1 or -1), wrapping around a list of the given size. */
    static int wrap(int index, int size, int dir) {
        if (size <= 0) return 0;
        return ((index + dir) % size + size) % size;
    }
}
