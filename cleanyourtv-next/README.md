# CleanYourTV Next

A new home screen for Android TV: dark, rounded, calm, and driven entirely by the remote.
Package `com.cleanyourtv.launcher`, written for a Xiaomi Mi TV Stick Full HD (Android TV 9, 32-bit ARM, about 1 GB of RAM).

It lives beside the reference launcher in `launcher/`, which it does not touch.

## What it does

- **Home**: greeting (with an optional first name), clock, date, and a dock of up to six favourite apps.
- **App library**: every installed app with its icon, alphabetical.
- **Settings**: accent colour, wallpaper, 12/24 hour clock, first name, show or hide apps not made for TV, reset the dock.
- **Remote only**: Up, Down, Left, Right, OK, Back. Hold OK (or press Menu) on an app to edit the dock.
- **Languages**: English and French.

## What it never does

- It never disables, replaces or hides Google's launcher or any system app.
- It asks for **no permission** and declares no service, receiver or provider: no accessibility service, no network, no storage, no secure settings. `app/src/test/.../ManifestPolicyTest` fails the build if that changes.
- It runs no `adb` or `pm` command, and ships no script that does.

## Status of this version (0.1.0)

| Item | State |
|---|---|
| Code type-checks against the Android 9 framework | Done in the sandbox where it was written |
| 28 JUnit tests | Pass (run with `javac` and JUnit, see `docs/BUILD.md`) |
| `./gradlew assembleDebug` | **Not run yet**: the sandbox had no Android SDK. The CI workflow `next.yml` runs it |
| Run on a real TV | **Not done** |
| Weather widget | Postponed after this first version, see `docs/ARCHITECTURE.md` |

## Next steps

- [`docs/BUILD.md`](docs/BUILD.md): build the APK and install it.
- [`docs/RECOVERY.md`](docs/RECOVERY.md): go back to Google's launcher, and what to expect from Xiaomi's Home button.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md): how the code is organised and how to extend it.
