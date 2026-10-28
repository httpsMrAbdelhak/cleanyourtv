# Weather

- **Source**: Open-Meteo forecast and geocoding APIs. No key, no account.
- **City**: typed by name in Settings, Weather city, and picked from the matches. **The location of the TV is never read** and there is no location permission.
- **Content**: current temperature and condition, and seven days with high and low (WMO weather codes, drawn as icons in code). The pill at the right of the top bar shows the current temperature and the time; OK on it shows the full card on Home.
- **Freshness**: cached 30 minutes, never fetched more than once a minute whatever is pressed, with the time of the last update shown. Old data is labelled as such.
- **Terms**: free for non-commercial use, with attribution under CC BY 4.0: "Weather data by Open-Meteo.com" is shown on the card. I could not read Open-Meteo's terms or daily limit from the build sandbox.
- **Unit**: degrees Celsius only for now.
- **Not tested live**: the sandbox that built this blocked the service, so the parser was tested on hand-written samples of the documented answer (`ParsersTest`). If a field name changed, the card shows "did not answer correctly" instead of wrong data.
