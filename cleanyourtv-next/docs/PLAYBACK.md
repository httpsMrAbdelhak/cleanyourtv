# "Continue watching": what Android allows, and what was built

**Result: no section is shown, and no data is simulated.** This page says why, and what is ready if a real source appears.

## What was asked

Cards for films and episodes really in progress in the streaming apps: poster, title, progress, a button to resume in the right app.

## What Android offers a home screen

1. **The Watch Next row.** Android TV apps publish "continue watching" entries to a system provider (`content://android.media.tv/watch_next_program`, see `TvContractCompat.WatchNextPrograms`). The system home screen reads them. Android's developer documentation describes how an app *writes* its own entries; it describes no way for another app to *read* the entries of all apps. Reading the provider is guarded by the `READ_EPG_DATA` permission, which as far as I could establish is for privileged system components, not for an app installed by hand. **I could not confirm this on a device or from the permission's source**, so treat it as strongly expected, not proven. Declaring the permission would only add a request that is never granted, so the manifest does not.
2. **Media sessions.** An app that plays media exposes a session, but listing other apps' sessions needs either a privileged permission or the user's grant of notification-listener access. The second reads the content of every notification of every app: far more than this feature needs, and against the "no reading of other apps' private data" rule of this project. Not done.
3. **Accessibility services.** They could read another app's screen. Forbidden here, and fragile.
4. **App-specific APIs.** Netflix, Prime Video, Disney+ and the others publish no documented, permission-free interface for a third party to read a user's progress. None was found, and none was guessed at.
5. **"Recently opened" is not "in progress".** An app opened an hour ago says nothing about a film paused at minute 41. The two are never mixed up in this launcher.

## What exists in the code

`ResumeSource` (an interface: title, subtitle, progress, poster, app) and `ResumeRegistry` (the list of sources, **empty**). The home screen would show a "Continue" section as soon as one source returns entries; with none, the section does not exist. Settings, under Integrations, states "Not available" with the reason.

## When this can change

- An app starts offering a documented, permission-free way to read its own progress (a content provider with a public contract). Implement `ResumeSource` for it and add one line to `ResumeRegistry.SOURCES`.
- The launcher is installed as a privileged system app (a different device setup, not something to do on a Mi TV Stick).
- The user types in what they are watching. That is a note-taking feature, not "continue watching", and is not built.
