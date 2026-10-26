package com.cleanyourtv.launcher;

import android.app.Application;
import android.content.ComponentCallbacks2;

/** Gives memory back on a 1 GB TV: the caches are the only thing the launcher can drop. */
public final class LauncherApp extends Application {
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            IconCache.get().clear();
            ImageLoader.get(this).trimMemory();
            Backdrop.release();
        } else if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            IconCache.get().shrink();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        IconCache.get().clear();
        ImageLoader.get(this).trimMemory();
    }
}
