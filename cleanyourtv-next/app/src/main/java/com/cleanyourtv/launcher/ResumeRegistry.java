package com.cleanyourtv.launcher;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/** The registered resume sources. Empty today: the home screen then shows no "Continue" section at all. */
final class ResumeRegistry {
    private ResumeRegistry() { }

    static final ResumeSource[] SOURCES = {};

    static boolean available() {
        return SOURCES.length > 0;
    }

    static List<ResumeSource.Entry> loadAll(Context context) {
        List<ResumeSource.Entry> all = new ArrayList<>();
        for (ResumeSource source : SOURCES) all.addAll(source.load(context));
        return all;
    }
}
