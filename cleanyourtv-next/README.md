# CleanYourTV Next

A new home screen for Android TV: dark, rounded, calm, and driven entirely by the remote.
Package `com.cleanyourtv.launcher`, written for a Xiaomi Mi TV Stick Full HD (Android TV 9, 32-bit ARM, about 1 GB of RAM).

It lives beside the reference launcher in `launcher/`, which it does not touch.

## What it does

- **Home**: a quiet clock and date on a wallpaper, a glass dock of up to six favourite apps, and a tab bar (Home, Cinema, Weather, settings) in the manner of tvOS.
- **Ambient light**: the selected dock icon grows, and a soft light in its colour spreads along the bottom of the screen.
- **Liquid Glass**: panels show a frosted copy of the wallpaper, a pale tint and a lit edge. A Performance level replaces it with flat glass.
- **Wallpapers**: six built-in cinematic lights, an optional slow animation, and a picture from the device. Preview before applying.
- **Cinema**: trending films and series from TMDB, with a backdrop that follows the focus. Needs a free TMDB key.
- **Weather**: current weather and seven days from Open-Meteo, for a city you choose. No key.
- **App library** and **settings**, driven by the remote: Up, Down, Left, Right, OK, Back. Hold OK (or press Menu) on a dock app to move or remove it.
- **Languages**: English and French.

## What it never does

- It never disables, replaces or hides Google's launcher or any system app.
- It asks for **one permission, INTERNET**, and declares no service, receiver or provider: no accessibility service, no storage, no location, no secure settings. `ManifestPolicyTest` fails the build if that changes.
- It runs no `adb` or `pm` command, and ships no script that does.
- It shows no data it does not have: no key, no network or no city each get a plain message.

## Status of this version (0.2.0)

What is verified and what is not:

| Item | State |
|---|---|
| Unit tests, lint, debug build | Run by the GitHub workflow `next.yml` on every push |
| Live TMDB and Open-Meteo answers | **Not tested**: the sandbox that wrote this could not reach them |
| Smoothness and memory on the Mi TV Stick | **Not measured**: needs the real device |
| "Continue watching" | **Not possible** for a third-party home screen, see `docs/PLAYBACK.md` |
| Home button on the Mi TV Stick | Unknown, see `docs/RECOVERY.md` |

## Next steps

- [`docs/BUILD.md`](docs/BUILD.md): build the APK and install it.
- [`docs/RECOVERY.md`](docs/RECOVERY.md): go back to Google's launcher.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md): how the code is organised and how to extend it.
- [`docs/CINEMA-WEATHER.md`](docs/CINEMA-WEATHER.md): the two data sources, keys, terms and limits.
- [`docs/PLAYBACK.md`](docs/PLAYBACK.md): why there is no "Continue watching".
