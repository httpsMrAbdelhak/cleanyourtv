package com.cleanyourtv.launcher;

import java.text.Normalizer;
import java.util.Locale;

/** Turns a published title into a TMDB search query and into a key two spellings of one title share. Pure Java. */
final class TitleKey {
    private TitleKey() { }

    /** "Friends - S01E10", "Friends (1994)" and "Friends : Saison 2" all search for "Friends". */
    static String query(String title) {
        if (title == null) return "";
        String text = title.trim();
        text = text.replaceAll("\\s*\\((?:19|20)\\d{2}\\)\\s*$", "");
        text = text.replaceAll("(?i)\\s*[-:·|,]?\\s*S\\d{1,2}\\s*E\\d{1,4}.*$", "");
        text = text.replaceAll("(?i)\\s*[-:·|,]?\\s*(?:saison|season|staffel|temporada)\\s*\\d{1,2}.*$", "");
        text = text.replaceAll("(?iu)\\s*[-:·|,]?\\s*(?:épisode|episode|ep\\.?)\\s*\\d{1,4}.*$", "");
        return text.trim();
    }

    /** Lower case, accents and punctuation dropped: "Là-haut" and "La Haut" give "lahaut". */
    static String key(String title) {
        if (title == null) return "";
        String plain = Normalizer.normalize(title, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }
}
