package com.cleanyourtv.launcher;

/**
 * An app publishes the intent that opens each of its titles. This launcher starts it only when it targets
 * the app the user chose as the source: a published intent may not send you to some other app.
 */
final class IntentGuard {
    private IntentGuard() { }

    /** componentPackage and explicitPackage come from the intent (either may be null); both must not name another app. */
    static boolean allowed(String sourcePackage, String componentPackage, String explicitPackage) {
        if (sourcePackage == null) return false;
        if (componentPackage == null && explicitPackage == null) return false;
        if (componentPackage != null && !sourcePackage.equals(componentPackage)) return false;
        return explicitPackage == null || sourcePackage.equals(explicitPackage);
    }
}
