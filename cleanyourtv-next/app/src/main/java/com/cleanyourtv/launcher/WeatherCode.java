package com.cleanyourtv.launcher;

/** WMO weather interpretation codes, as Open-Meteo returns them, folded into the nine looks the UI draws. */
enum WeatherCode {
    CLEAR(R.string.wx_clear),
    PARTLY(R.string.wx_partly),
    CLOUDY(R.string.wx_cloudy),
    FOG(R.string.wx_fog),
    DRIZZLE(R.string.wx_drizzle),
    RAIN(R.string.wx_rain),
    SNOW(R.string.wx_snow),
    SHOWERS(R.string.wx_showers),
    STORM(R.string.wx_storm);

    final int label;

    WeatherCode(int label) {
        this.label = label;
    }

    static WeatherCode of(int wmo) {
        if (wmo == 0) return CLEAR;
        if (wmo == 1 || wmo == 2) return PARTLY;
        if (wmo == 3) return CLOUDY;
        if (wmo == 45 || wmo == 48) return FOG;
        if (wmo >= 51 && wmo <= 57) return DRIZZLE;
        if ((wmo >= 61 && wmo <= 67)) return RAIN;
        if ((wmo >= 71 && wmo <= 77) || wmo == 85 || wmo == 86) return SNOW;
        if (wmo >= 80 && wmo <= 82) return SHOWERS;
        if (wmo >= 95 && wmo <= 99) return STORM;
        return CLOUDY;
    }
}
