package com.cleanyourtv.launcher;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lists the apps that can be opened, and opens them. It only reads the package list: it changes nothing. */
final class AppRepository {
    private AppRepository() { }

    interface Callback {
        void onLoaded(List<AppEntry> apps);
    }

    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "apps");
        thread.setDaemon(true);
        return thread;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** Loads off the main thread and answers on it. The caller checks that its screen is still alive. */
    static void loadAsync(final Context context, final boolean includeNonTv, final Callback callback) {
        final Context app = context.getApplicationContext();
        POOL.execute(() -> {
            final List<AppEntry> apps = load(app.getPackageManager(), app.getPackageName(), includeNonTv);
            MAIN.post(() -> callback.onLoaded(apps));
        });
    }

    /** Apps with a TV entry first, then (optionally) those with a phone entry only. This app is left out. */
    static List<AppEntry> load(PackageManager pm, String ownPackage, boolean includeNonTv) {
        Map<String, AppEntry> found = new LinkedHashMap<>();
        collect(pm, Intent.CATEGORY_LEANBACK_LAUNCHER, ownPackage, found);
        if (includeNonTv) collect(pm, Intent.CATEGORY_LAUNCHER, ownPackage, found);
        List<AppEntry> apps = new ArrayList<>(found.values());
        AppEntry.sort(apps);
        return apps;
    }

    private static void collect(PackageManager pm, String category, String ownPackage, Map<String, AppEntry> into) {
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
        for (ResolveInfo info : pm.queryIntentActivities(query, 0)) {
            if (info.activityInfo == null) continue;
            String packageName = info.activityInfo.packageName;
            if (packageName.equals(ownPackage) || into.containsKey(packageName)) continue;
            CharSequence label = info.loadLabel(pm);
            into.put(packageName, new AppEntry(label == null ? packageName : label.toString(), packageName));
        }
    }

    /** Opens the app's TV entry, else its phone entry. False when the app cannot be opened. */
    static boolean launch(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLeanbackLaunchIntentForPackage(packageName);
        if (intent == null) intent = pm.getLaunchIntentForPackage(packageName);
        if (intent == null) return false;
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }
}
