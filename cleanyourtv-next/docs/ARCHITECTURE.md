# Architecture

Plain Java on the Android framework. No AndroidX, no Kotlin, no layout XML: every screen is built in code, so there is no resource to drift from the code and the APK carries no library. One runtime permission (`INTERNET`), no service, no receiver, no native code (so 32-bit ARM, 64-bit ARM and x86 all run the same APK).

## Screens

```
HomeActivity          host: wallpaper, ambient light, three pages, tab bar, focus and Back rules
  HomePage            clock, date, glass dock
  CinemaPage          backdrop + rails of posters (TMDB)
  WeatherPage         current weather + seven days (Open-Meteo)
LibraryActivity       every installed app (opened from the dock)
SettingsActivity      sections of rows; every change saved at once
WallpaperActivity     gallery with live preview; picture from the device
DetailActivity        one film or series
```

Only the shown page is visible: the others are `GONE`, take no focus, and free their images.

## Design system

| Piece | What it is |
|---|---|
| `GlassDrawable` | Liquid Glass: a panel samples the frosted wallpaper at its own position, adds a tint brighter at the top and an edge lit from the top left. |
| `Backdrop` | The frosted wallpaper: drawn once into 320x180, box-blurred once (`BoxBlur`), rebuilt only when the wallpaper or effects level changes. Never recomputed per frame, never a full-size blur. |
| `AmbientGlow` | The light under the dock: one white radial gradient, tinted by a colour filter and stretched by the canvas. Moves, recolours and fades in 420 ms. |
| `DominantColor` | The colour of an icon: the most vivid hue of a 24x24 copy, lifted to glow brightness; a grey icon gives the accent colour. |
| `SceneDrawable` | A wallpaper made of soft radial lights over a dark gradient: no bitmap, sharp at any size. Animated only by moving the lights along a seamless loop. |
| `WallpaperView` | Draws the wallpaper and two shades. Animates at about 15 fps, only while visible, and falls back to the still picture for good if the TV cannot keep up. |
| Outfit font | Variable font, SIL OFL. Loaded once. |

Two effect levels (`Effects`): **Quality** (frosted glass, optional animation, sharper posters) and **Performance** (flat glass, still wallpaper, 16-bit smaller posters).

### Why glass is "sampled", not blurred live

Android 9 has no cheap live background blur (`RenderEffect` is Android 12). Re-blurring the screen every frame would starve a 1 GB stick. Because the wallpaper is known, it is blurred once, and each glass panel paints the part of that copy that lies behind it. Panels that scroll use flat glass instead, since a sampled copy would show the wrong part once they moved.

## Remote navigation

Focus uses Android's own search, with explicit rules where a mistake would lose it:

- **Tab bar**: Left and Right move over the tabs and the page follows; Down enters the page; Up does nothing.
- **Dock**: Left and Right move, stopping at both ends; Up goes to the tab bar; Down does nothing (no app grid opens by surprise); OK opens; long OK or Menu edits.
- **Rails**: Left and Right glide along the rail and stop at the ends; Up and Down change rail, Up on the first rail goes to the tab bar; OK opens the detail screen.
- **Back**: from content to the tab bar, from another tab to Home, on Home nothing (`NavLogic`).
- **Home key** while already on Home: back to the Home tab, on the first dock icon.
- After an app closes, `onResume` gives the focus back if it is gone.

## Network and memory (about 1 GB of RAM)

- `ImageLoader`: 12 MB memory cache, 60 MB disk cache (oldest removed first), three low-priority threads. A view reused for another picture ignores late answers. Pictures fade in over a dim placeholder: never a white flash.
- `RailWindow`: a rail of twenty posters holds bitmaps only for the first six and those near the focus.
- `JsonCache`: TMDB six hours, weather 30 minutes; old data is shown and labelled when the network fails.
- `LauncherApp.onTrimMemory`: empties the caches and the frosted copy when the system asks.
- The only timer is a 60-second tick for the greeting, while Home is visible. No service.

## Extending

- **A wallpaper**: implement `WallpaperSource`, add it to `Wallpapers.ALL`, add its name to both `strings.xml`. The gallery lists it.
- **An accent colour**: one line in `Accents.ALL` and its name in both `strings.xml`.
- **A "Continue watching" source**: implement `ResumeSource` and add it to `ResumeRegistry` (see `PLAYBACK.md`).
- **A language**: add `values-xx/strings.xml` with every name of `values/strings.xml` and add `xx` to `resourceConfigurations` in `app/build.gradle`.
