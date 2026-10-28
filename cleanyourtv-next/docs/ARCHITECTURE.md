# Architecture

Plain Java on the Android framework. No AndroidX, no Kotlin, no layout XML: every screen is built in code, so there is no resource to drift from the code and the APK carries no library. Two permissions (`INTERNET`, `READ_TV_LISTINGS`), no service, no receiver, no native code (so 32-bit ARM, 64-bit ARM and x86 all run the same APK).

## Screens

```
HomeActivity          host: the wallpaper under one HomeScreen; rebuilds when the theme flips; asks for the TV permission
  HomeScreen          top bar, greeting, chips, the page in the middle, glass dock, ambient light
    TopBar            avatar, sections (Home, Library, Explore, Apps, Games), search, round buttons, weather pill
    CardRail          a row of posters; the selected one widens to a landscape card; images loaded near the focus only
    LibraryShelf      Library (every row of the app) and Explore (trending films and series), one rail under the other
    WeatherPanel      current weather and seven days on one glass card (OK on the weather pill)
    MessageBox        a plain message with one button (no app, no permission, nothing published)
LibraryActivity       the app drawer: every app as wide banners, or the games, or a search; slides up from the dock
SettingsActivity      sections of rows; every change saved at once
WallpaperActivity     gallery with live preview; picture from the device
```

Where the content comes from (`TvChannels`): the TV's own provider of channels and programs, for the one app chosen in Settings (found by name, "Nuvio", when nothing is chosen). Rows read: Watch Next (shown as "Continue watching") and the app's preview channels. `EntryFilter` applies the chips, `IntentGuard` lets an entry start only the chosen app, `ProgramKind` turns the provider's type numbers into movie, series or other.

TMDB (`Tmdb`, with the pure `TmdbParser`, `TmdbIds`, `TitleKey` and `PosterSwap`): the trending rows of Explore, and the poster and backdrop of the series or film behind each published card, cached on disk. The key comes from Settings or from the build (`BuildConfig.TMDB_KEY`, empty unless `TMDB_API_KEY` is given); it is never in the sources.

## Design system

| Piece | What it is |
|---|---|
| `GlassDrawable` | Liquid Glass: a panel samples the frosted wallpaper at its own position, adds a tint brighter at the top and an edge lit from the top left. The dock uses its "clear" style (smoked, see-through) rather than the milky one. The dock selection is a thin ring plus a lift on the banner. |
| `Backdrop` | The frosted wallpaper: drawn once into 320x180, box-blurred once (`BoxBlur`), rebuilt only when the wallpaper or effects level changes. Never recomputed per frame, never a full-size blur. |
| `AmbientGlow` | The light along the bottom of the screen, corner to corner: a fixed band plus a brighter spot under the selected app, each one white gradient tinted by a colour filter. Only the spot slides; colour and fade animate in 420 ms. |
| `DominantColor` | The colour of an icon: the most vivid hue of a 24x24 copy, lifted to glow brightness; a grey icon gives the accent colour. |
| `CardView` | A card that animates its width from a portrait poster (2:3) to a landscape card (16:9) in 220 ms when selected. The landscape picture is loaded only while the card is selected; without one, the poster is shown cropped. `ShapePolicy` picks the pictures, `CardText` the badge and details line. |
| `HomeLayoutTest` | Runs the real home screen on a simulated 1920x1080 display (Robolectric) and checks the greeting is near the top, the dock in the lower fifth and centred, and the row between. |
| `SceneDrawable` | A wallpaper made of soft radial lights over a dark gradient: no bitmap, sharp at any size. Animated only by moving the lights along a seamless loop. |
| `WallpaperView` | Draws the wallpaper and two shades. Animates at about 15 fps, only while visible, and falls back to the still picture for good if the TV cannot keep up. |
| Outfit font | Variable font, SIL OFL. Loaded once. |
| `Palette` | Light or dark, chosen by the wallpaper (`Wallpaper.isLight`). Applied before any view is built; a flip rebuilds the screen. |
| `Ui.unit` | The screen is laid out as 960 units wide whatever its density (a TV set to 320 dpi or 213 dpi looks the same). |

Two effect levels (`Effects`): **Quality** (frosted glass, optional animation, sharper posters) and **Performance** (flat glass, still wallpaper, 16-bit smaller posters).

### Why glass is "sampled", not blurred live

Android 9 has no cheap live background blur (`RenderEffect` is Android 12). Re-blurring the screen every frame would starve a 1 GB stick. Because the wallpaper is known, it is blurred once, and each glass panel paints the part of that copy that lies behind it. Panels that scroll use flat glass instead, since a sampled copy would show the wrong part once they moved.

## Remote navigation

Four zones (`NavLogic`): top bar, chips (Home only), content, dock. The dock has the focus at start.

- **Dock**: Left and Right move and stop at both ends; Up goes to the content (or the chips, or the top bar, when there is none); **Down opens the app drawer**; OK opens the app; long OK or Menu edits.
- **Content**: Left and Right glide along the cards and stop at the ends; the selected poster widens. Up goes to the chips (Home) or the top bar; Down returns to the dock.
- **Top bar**: resting on Home, Library or Explore for 220 ms shows that page; Apps and Games open the drawer on OK. Down enters the page.
- **App drawer**: five banners per row; OK opens, long OK adds to or removes from the dock. **Up from the top row returns to the dock**, as Back does: no need to press Back.
- **Back**: from anywhere it goes down to the dock; on the dock it does nothing.
- **Home key** while already on Home: back to the dock.
- A late answer for a request already replaced is dropped.

## Network and memory (about 1 GB of RAM)

- `ImageLoader` (pictures of the app's rows, from https:// or its own content://): 12 MB memory cache, 60 MB disk cache (oldest removed first), three low-priority threads. A view reused for another picture ignores late answers. Pictures fade in over a dim placeholder: never a white flash.
- `RailWindow`: a rail of twenty posters holds bitmaps only for the first six and those near the focus.
- `JsonCache`: the weather, 30 minutes; old data is shown and labelled when the network fails. The streaming content is read live from the TV, never cached by us.
- `LauncherApp.onTrimMemory`: empties the caches and the frosted copy when the system asks.
- The only timer is a 60-second tick for the greeting, while Home is visible. No service.

## Extending

- **A wallpaper**: implement `WallpaperSource`, add it to `Wallpapers.ALL`, add its name to both `strings.xml`. The gallery lists it.
- **An accent colour**: one line in `Accents.ALL` and its name in both `strings.xml`.
- **A language**: add `values-xx/strings.xml` with every name of `values/strings.xml` and add `xx` to `resourceConfigurations` in `app/build.gradle`.
