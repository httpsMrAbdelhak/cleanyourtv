# Architecture

Plain Java on the Android framework. No AndroidX, no Kotlin, no layout XML: the three screens are built in code, so there is no resource to drift from the code and the APK carries no library.

```
com.cleanyourtv.launcher
  HomeActivity       greeting, clock, date, dock. Intent filters: HOME and LEANBACK_LAUNCHER
  LibraryActivity    every app, as rows of tiles in a ScrollView
  SettingsActivity   a column of rows; each change is saved at once
  LauncherApp        gives the icon cache back when the system asks for memory

  TileView           icon over label, grows a little on focus (shared by dock and library)
  ChoiceDialog       the dark list of choices (dock menu, confirmations, name entry)
  Ui, Glyphs, Palette   small view helpers, two drawn icons, the fixed dark colours

  AppRepository      lists launchable apps off the main thread, and launches them
  IconCache          icons drawn once at display size, in a 6 MB bounded cache
  Prefs              one SharedPreferences file

  Pure logic, covered by JVM tests (no android.* import):
  DockModel, DockDefaults, Greeting, ClockMode, Cycle, AppEntry, Accents, Palette
```

## Remote navigation

The D-pad uses Android's own focus search; no key code is intercepted except Back on Home (ignored) and Menu (same as holding OK).

- Dock: a horizontal row of focusable tiles. Left and Right move, OK opens, long OK edits.
- Library and settings: vertical lists inside a `ScrollView`, which scrolls to follow the focus.
- Every tile is `focusable` with a visible outline in the accent colour, so focus is never invisible.

## Memory (about 1 GB of RAM)

- Wallpapers are gradients, not bitmaps. No blur, no video.
- Icons are rendered at the size shown and cached under 6 MB; the cache halves when the UI is hidden and empties when the app goes to the background or memory is low.
- The only timer is a 60 second tick for the greeting, and it runs only while Home is visible. No service, no receiver.

## Extending

- **A wallpaper**: implement `WallpaperSource` and add it to `Wallpapers.ALL`; add its name to both `strings.xml`. Settings lists it by itself.
- **An accent colour**: one line in `Accents.ALL`, plus its name in both `strings.xml`.
- **A language**: add `values-xx/strings.xml` with every name of `values/strings.xml` (a test enforces the match for French) and add `xx` to `resourceConfigurations` in `app/build.gradle`.

## Weather: postponed

Weather needs the network, and this launcher has **no `INTERNET` permission** by design (`ManifestPolicyTest` enforces it). It does not block version 0.1. Two ways to add it later:

1. **A separate app** (as `addons/weather/` does with Open-Meteo, which needs no account or key), that this launcher would open from the dock. The launcher stays offline. Recommended.
2. **An optional switch** in the launcher, off by default, that adds the permission. Android grants a manifest permission at install, so "disabled" would only mean the code does nothing: the permission would still be there. That is why option 1 is preferred.

Whether Open-Meteo stays free and reliable is not verified here.

## Not in this version

Wallpaper from a picture or a USB drive (needs storage access), a weather widget, widget tiles on Home, and any change to how the Home button is handled (see `RECOVERY.md`).
