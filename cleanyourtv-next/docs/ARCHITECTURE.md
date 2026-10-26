# Architecture

Plain Java on the Android framework. No AndroidX, no Kotlin, no layout XML: every screen is built in code, so there is no resource to drift from the code and the APK carries no library. One runtime permission (`INTERNET`), no service, no receiver, no native code (so 32-bit ARM, 64-bit ARM and x86 all run the same APK).

## Screens

```
HomeActivity          host: the wallpaper under one HomeScreen; rebuilds when the theme flips
  HomeScreen          header (greeting, tabs, clock, two buttons), content panel, glass dock, ambient light
    ContentPanel      the panel above the dock: films, videos or weather, depending on the dock selection
      CardRail        a row of cards (wide, poster, video thumbnail), loaded near the focus only
      WeatherPanel    current weather and seven days on one glass card
LibraryActivity       every installed app (the grid button)
SettingsActivity      sections of rows; every change saved at once
WallpaperActivity     gallery with live preview; picture from the device
DetailActivity        one film or series, with an "Open <app>" button when it came from a service
```

What the panel shows (`HomeScreen.select`, `ProviderMap`):

| Dock tile | Panel |
|---|---|
| a known streaming app | that service's films and series (TMDB `discover` with the provider id found by name in TMDB's own provider list, never hard-coded) |
| YouTube (`com.google.android.youtube.tv`, SmartTube) | most popular videos (YouTube Data API) |
| the weather tile | the weather |
| any other app | trending on TMDB |

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
| `Palette` | Light or dark, chosen by the wallpaper (`Wallpaper.isLight`). Applied before any view is built; a flip rebuilds the screen. |
| `Ui.unit` | The screen is laid out as 960 units wide whatever its density (a TV set to 320 dpi or 213 dpi looks the same). |

Two effect levels (`Effects`): **Quality** (frosted glass, optional animation, sharper posters) and **Performance** (flat glass, still wallpaper, 16-bit smaller posters).

### Why glass is "sampled", not blurred live

Android 9 has no cheap live background blur (`RenderEffect` is Android 12). Re-blurring the screen every frame would starve a 1 GB stick. Because the wallpaper is known, it is blurred once, and each glass panel paints the part of that copy that lies behind it. Panels that scroll use flat glass instead, since a sampled copy would show the wrong part once they moved.

## Remote navigation

Three zones (`NavLogic`): header on top, panel in the middle, dock at the bottom. The dock has the focus at start.

- **Dock**: Left and Right move and stop at both ends; Up goes to the panel (or the header when the panel is empty); Down does nothing, so no app grid ever opens by surprise; OK opens the app; long OK or Menu edits.
- **Panel**: Left and Right glide along the cards and stop at the ends; Up goes to the header; Down returns to the dock.
- **Header**: Down enters the panel. The three tabs only apply to film panels: elsewhere they are dimmed and skipped.
- **Back**: from the panel or the header it goes down to the dock; on the dock it does nothing.
- **Home key** while already on Home: back to the dock.
- The panel changes 220 ms after the focus rests on a tile, so crossing the dock fast does not start a load per tile; a late answer for a tile already left is dropped.

## Network and memory (about 1 GB of RAM)

- `ImageLoader`: 12 MB memory cache, 60 MB disk cache (oldest removed first), three low-priority threads. A view reused for another picture ignores late answers. Pictures fade in over a dim placeholder: never a white flash.
- `RailWindow`: a rail of twenty posters holds bitmaps only for the first six and those near the focus.
- `JsonCache`: TMDB six hours, weather 30 minutes; old data is shown and labelled when the network fails.
- `LauncherApp.onTrimMemory`: empties the caches and the frosted copy when the system asks.
- The only timer is a 60-second tick for the greeting, while Home is visible. No service.

## Extending

- **A wallpaper**: implement `WallpaperSource`, add it to `Wallpapers.ALL`, add its name to both `strings.xml`. The gallery lists it.
- **An accent colour**: one line in `Accents.ALL` and its name in both `strings.xml`.
- **A streaming service**: add its package and TMDB provider names to `ProviderMap.SERVICES`.
- **A "Continue watching" source**: implement `ResumeSource` and add it to `ResumeRegistry` (see `PLAYBACK.md`).
- **A language**: add `values-xx/strings.xml` with every name of `values/strings.xml` and add `xx` to `resourceConfigurations` in `app/build.gradle`.
