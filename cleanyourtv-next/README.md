# CleanYourTV Next

A new home screen for Android TV: dark, rounded, calm, and driven entirely by the remote.
Package `com.cleanyourtv.launcher`, written for a Xiaomi Mi TV Stick Full HD (Android TV 9, 32-bit ARM, about 1 GB of RAM).

It lives beside the reference launcher in `launcher/`, which it does not touch.

## What it does

- **A clean, light look**: a pale wallpaper, dark text, frosted glass. A dark wallpaper switches the whole interface to light text on smoked glass: the theme follows the wallpaper.
- **A smoked-glass dock of wide banners**: each app shows its own TV banner (Netflix, YouTube and most streaming apps ship one); an app without one gets its icon on a gradient of its own colour. **A pane of glass slides from banner to banner behind the selected one and takes its colour**, with a soft light of the same colour under the dock.
- **Down on the dock opens the list of all apps**, as the same wide banners. Hold OK on a banner to add or remove it from the dock.
- **Content from your streaming app** (Nuvio): the panel above the dock shows what the app publishes for the Android TV home screen, with the tabs Explore, Movies and Series. No TMDB or YouTube key. It stays whichever dock tile is selected. See `docs/STREAMING-CONTENT.md`.
- **Weather**: the weather tile shows the temperature; selecting it shows the current weather and seven days for a city you choose. No key.
- **Wallpapers**: three light and six dark built-in scenes, four painted landscapes (sunset, mauve dawn, lake at dusk, misty mountains), an optional slow animation, and a picture from the device. Preview before applying.
- **Liquid Glass**, with a Performance level that flattens it.
- **Remote only**: Up, Down, Left, Right, OK, Back, and Menu.
- **Languages**: English and French.

## What it never does

- It never disables, replaces or hides Google's launcher or any system app.
- It asks for **two permissions**: INTERNET (weather, pictures) and READ_TV_LISTINGS (reading the content your streaming app publishes for the TV home screen; Android asks you), and declares no service, receiver or provider: no accessibility service, no storage, no location, no secure settings. `ManifestPolicyTest` fails the build if that changes.
- It runs no `adb` or `pm` command, and ships no script that does.
- It shows no data it does not have: no key, no network or no city each get a plain message.

## Status of this version (0.4.0)

What is verified and what is not:

| Item | State |
|---|---|
| Unit tests, lint, debug build | Run by the GitHub workflow `next.yml` on every push |
| Content of Nuvio in the panel | **Not tested on a TV.** Depends on Nuvio publishing home-screen rows and on your TV allowing a third-party home screen to read them: see `docs/STREAMING-CONTENT.md` |
| Live Open-Meteo answers | **Not tested**: the sandbox that wrote this could not reach it |
| Smoothness and memory on the Mi TV Stick | **Not measured**: needs the real device |
| Home button on the Mi TV Stick | Unknown, see `docs/RECOVERY.md` |
| Photographic wallpapers | **None bundled**: no photo site was reachable from the sandbox. The landscapes are painted in code; pick your own photo from the device in the gallery |

## Next steps

- [`docs/BUILD.md`](docs/BUILD.md): build the APK and install it.
- [`docs/RECOVERY.md`](docs/RECOVERY.md): go back to Google's launcher.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md): how the code is organised and how to extend it.
- [`docs/STREAMING-CONTENT.md`](docs/STREAMING-CONTENT.md): how the panel gets its content, what to turn on, what is unknown.
- [`docs/WEATHER.md`](docs/WEATHER.md): the weather source, terms and limits.
- [`docs/PLAYBACK.md`](docs/PLAYBACK.md): what Android allows for "Continue watching".
