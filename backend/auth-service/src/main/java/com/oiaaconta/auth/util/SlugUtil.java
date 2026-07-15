package com.oiaaconta.auth.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtil {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private SlugUtil() {
    }

    public static String normalize(String texto) {
        String normalized = Normalizer.normalize(texto, Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalized)
            .replaceAll("")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("[\\s]+", "-")
            .replaceAll("-+", "-")
            .trim();
    }
}
