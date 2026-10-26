package com.cleanyourtv.launcher;

/** The accent colours on offer. To add one, add a line to ALL and its name to both strings.xml files. */
final class Accents {
    private Accents() { }

    static final class Option {
        final String id;
        final int color;
        final int label;

        Option(String id, int color, int label) {
            this.id = id;
            this.color = color;
            this.label = label;
        }
    }

    static final Option[] ALL = {
            new Option("blue", 0xFF4C8DFF, R.string.accent_blue),
            new Option("violet", 0xFF9B7BFF, R.string.accent_violet),
            new Option("green", 0xFF3DDC97, R.string.accent_green),
            new Option("orange", 0xFFFF9F43, R.string.accent_orange),
            new Option("pink", 0xFFFF6FA5, R.string.accent_pink),
            new Option("white", 0xFFE6E9F0, R.string.accent_white),
    };

    static int indexOf(String id) {
        for (int i = 0; i < ALL.length; i++) {
            if (ALL[i].id.equals(id)) return i;
        }
        return 0;
    }

    static Option byId(String id) {
        return ALL[indexOf(id)];
    }
}
