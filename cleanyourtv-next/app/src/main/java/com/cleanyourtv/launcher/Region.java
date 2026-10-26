package com.cleanyourtv.launcher;

import java.util.Locale;

/** The country sent to TMDB and YouTube so that catalogues and charts match where the TV is. */
final class Region {
    private Region() { }

    /** An upper-case ISO country from the locale, "FR" for a French locale with no country, "US" otherwise. */
    static String of(Locale locale) {
        String country = locale.getCountry();
        if (country != null && country.length() == 2) return country.toUpperCase(Locale.ROOT);
        return "fr".equals(locale.getLanguage()) ? "FR" : "US";
    }
}
