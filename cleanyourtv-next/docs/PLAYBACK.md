# "Continue watching": what Android allows

**Short version: it is the app's own "Continue watching", shown if the app publishes it and the TV allows it.** CleanYourTV does not spy on other apps and never guesses.

## What Android offers a home screen

1. **The Watch Next row.** Android TV apps publish "continue watching" entries to a system provider (`content://android.media.tv/watch_next_program`). The stock home screen reads them. CleanYourTV reads the same provider, for the one app you chose, with the permission `READ_TV_LISTINGS` (requested at run time). Each entry has a title, a picture, a position and a duration (shown as a progress bar), and an intent to resume. See `STREAMING-CONTENT.md`.
2. **Media sessions** (what is playing now) need a privileged permission or notification-listener access, which reads every notification of every app. Not used.
3. **Accessibility services** could read another app's screen. Never used.
4. **"Recently opened" is not "in progress".** An app opened an hour ago says nothing about a film paused at minute 41. The two are never mixed up.

## What decides whether it shows

- The app must publish its Watch Next / home-screen rows (many streaming apps have an option for it).
- The TV's provider must hand them to a third-party home screen. I could not confirm this on a device, and in an earlier version of this document I wrongly assumed it was impossible: the permission turned out to be requestable. Whether Xiaomi's firmware accepts it is for your TV to answer.

When something is missing, the screen says what (no app, no permission, nothing published, refused), and no list is ever invented.
