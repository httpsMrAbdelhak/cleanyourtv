# CleanYourTV Next

A new home screen for Android TV: night blue, rounded, calm, and driven entirely by the remote.
Package `com.cleanyourtv.launcher`, written for a Xiaomi Mi TV Stick Full HD (Android TV 9, 32-bit ARM, about 1 GB of RAM).

It lives beside the reference launcher in `launcher/`, which it does not touch.

## What it does

- **A top bar of sections**: an avatar, Home, Library, My list, Apps, Games and Search; on the right, round buttons (light or dark wallpaper, effects, settings) and a pill with the weather and the time.
- **A bold "Hello <name>" greeting** and the chips Explore, Movies and Series over a row of posters.
- **A night-blue look by default**: light text on smoked glass. The moon button switches to a pale wallpaper with dark text: the theme follows the wallpaper.
- **A Liquid Glass dock of wide banners, labelled "Favourite apps"**: each app shows its own TV banner (Netflix, YouTube and most streaming apps ship one); an app without one gets its icon on a gradient of its own colour. The selected banner gets a thin ring and lifts. **A light in the selected app's colour runs along the whole bottom of the screen, corner to corner, and stays put; only a brighter spot under the selected app moves.**
- **Down on the dock opens the app drawer**, as the same wide banners; Up from its top row (or Back) slides it away and returns to the dock. Hold OK on a banner to add or remove it from the dock. Apps and Games in the top bar open the same drawer, Games showing only the apps Android marks as games.
- **Content from your streaming app** (Nuvio): the row above the dock shows the posters of the films and series the app publishes for the Android TV home screen (not episode pictures). The selected poster widens into a landscape card with its title and details; the others stay portrait posters. Library shows every row of the app, My list its watchlist. No TMDB or YouTube key. See `docs/STREAMING-CONTENT.md`.
- **Weather**: the pill of the top bar shows the temperature; OK on it shows the current weather and seven days for a city you choose. No key.
- **Wallpapers**: three light and seven dark built-in scenes (night blue by default), an optional slow animation, and a picture from the device. Preview before applying.
- **Liquid Glass**, with a Performance level that flattens it.
- **Remote only**: Up, Down, Left, Right, OK, Back, and Menu.
- **Languages**: English and French.

## What it never does

- It never disables, replaces or hides Google's launcher or any system app.
- It asks for **two permissions**: INTERNET (weather, pictures) and READ_TV_LISTINGS (reading the content your streaming app publishes for the TV home screen; Android asks you), and declares no service, receiver or provider: no accessibility service, no storage, no location, no secure settings. `ManifestPolicyTest` fails the build if that changes.
- It runs no `adb` or `pm` command, and ships no script that does.
- It shows no data it does not have: no key, no network or no city each get a plain message.

## Status of this version (0.5.0)

What is verified and what is not:

| Item | State |
|---|---|
| Unit tests, lint, debug build | Run by the GitHub workflow `next.yml` on every push |
| Content of Nuvio on Home, Library and My list | **Not tested on a TV.** Depends on Nuvio publishing home-screen rows and on your TV allowing a third-party home screen to read them: see `docs/STREAMING-CONTENT.md` |
| Live Open-Meteo answers | **Not tested**: the sandbox that wrote this could not reach it |
| Smoothness and memory on the Mi TV Stick | **Not measured**: needs the real device |
| Home button on the Mi TV Stick | Unknown, see `docs/RECOVERY.md` |

## Next steps

- [`docs/BUILD.md`](docs/BUILD.md): build the APK and install it.
- [`docs/RECOVERY.md`](docs/RECOVERY.md): go back to Google's launcher.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md): how the code is organised and how to extend it.
- [`docs/STREAMING-CONTENT.md`](docs/STREAMING-CONTENT.md): how the rows get their content, what to turn on, what is unknown.
- [`docs/WEATHER.md`](docs/WEATHER.md): the weather source, terms and limits.
- [`docs/PLAYBACK.md`](docs/PLAYBACK.md): what Android allows for "Continue watching".
