# Films, videos and weather: sources, keys, limits

Both sections need the network, so the app asks for the `INTERNET` permission and nothing else. All traffic is HTTPS, and `ManifestPolicyTest` fails the build on a plain `http://` address or on any other permission.

## What follows the dock

The panel above the dock changes with the selected tile (see ARCHITECTURE.md): a streaming app shows its own catalogue, YouTube shows videos, the weather tile shows the weather, anything else shows what is trending.

**How an app is matched to its catalogue.** The package name (for example `com.netflix.ninja`) is mapped to the service's name, and that name is looked up in TMDB's own list of watch providers for the TV's country (`watch/providers/movie?watch_region=XX`, cached a week). The id found there is used with `discover/movie` and `discover/tv`. No id is written in the code: an id that was wrong would silently show another service's catalogue. If TMDB has no provider by that name in your country, the panel says so and shows what is popular. The match by name was tested on samples only; whether TMDB's names match on your TV is checked on first use. The catalogue data comes from JustWatch through TMDB.

## YouTube

- **Source**: YouTube Data API v3, `videos.list` with `chart=mostPopular` in the TV's country. It needs a free API key from Google Cloud (create a project, enable "YouTube Data API v3", create a key), entered in Settings, YouTube API key. Without it the panel explains and shows nothing else. The key is stored on the TV only; the build never contains one.
- **Limits**: the default daily quota is 10,000 units and this call costs 1; the app asks once an hour. An invalid key (HTTP 400) and an exhausted quota (HTTP 403) both show "refused". I could not read Google's terms from the build sandbox: YouTube API Services have their own terms of service and branding rules, which apply to anyone who uses the key.
- **Opening a video**: OK sends a normal `https://www.youtube.com/watch?v=<id>` link to the YouTube app of the dock, falling back to any app that handles it. **Not tried on a TV.**

## Cinema: TMDB

- **Source**: The Movie Database API, `https://api.themoviedb.org/3`: `trending/all/week`, `movie/popular`, `tv/popular`, `discover/movie`, `discover/tv`, `watch/providers/movie`. Posters come from `image.tmdb.org` in the sizes TMDB documents (`w185` and `w342` posters, `w780` backdrops).
- **Key**: a free personal key is needed, from your account on themoviedb.org (Settings, API). The short v3 key and the long v4 "read access token" (it starts with `eyJ`) both work. **Without a key the page shows an explanation and nothing else.** No sample data is shown as if it were real.
- **Where the key lives**: typed in Settings, Cinema, TMDB key. It is stored in the app's private preferences on the TV. It is never in the repository. Optionally, `TMDB_API_KEY` (a Gradle property or environment variable) bakes a key into the APK at build time. **Do not do that for a build you share or upload**: anyone who gets the APK can extract the key. The GitHub workflow does not pass any secret.
- **Terms**: free for non-commercial use with attribution. The attribution sentence ("This product uses the TMDB API but is not endorsed or certified by TMDB.") is shown on the Cinema page and in Settings. **TMDB also asks for its logo, and the logo is not included**: I could not fetch it, and I did not draw a look-alike. Add the official logo from TMDB's brand page if you publish this app. I could not read TMDB's current terms or rate limits from the build sandbox; secondary sources quote roughly 40 requests per second, and the app makes a handful of requests per six hours.
- **Freshness**: answers are cached for 6 hours. When the network fails, the last answer is shown and labelled "Offline: data from <date>". Nothing is claimed to be real time.
- **No playback**: TMDB describes titles and gives no link to watch them. The detail screen says so and has no Play button.

## Weather: Open-Meteo

- **Source**: Open-Meteo forecast and geocoding APIs. No key, no account.
- **City**: typed by name in Settings, Weather city, and picked from the matches. **The location of the TV is never read** and no location permission exists.
- **Content**: current temperature and condition, and seven days with high and low, from the `current` and `daily` blocks of the forecast API (WMO weather codes, drawn as icons in code).
- **Freshness**: cached 30 minutes, never fetched more than once a minute whatever is pressed, with the time of the last update shown. Old data is labelled as such.
- **Terms**: free for non-commercial use, with attribution under CC BY 4.0: "Weather data by Open-Meteo.com" is shown on the page. I could not read Open-Meteo's terms or daily limit from the build sandbox.
- **Unit**: degrees Celsius only for now.

## What was not tested

The sandbox that built this blocked both services, so the JSON parsers were written from the documented answer shapes and tested on hand-written samples (`ParsersTest`), not on live answers. The first run on a TV with a key is the real test. If either service changed a field name, the page shows its "did not answer correctly" message instead of wrong data.
