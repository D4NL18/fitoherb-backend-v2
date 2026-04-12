package com.fitoherb.fitoherb_backend_v2.utils;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class StringUtils {

    private StringUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String toSlug(String input) {
        if (input == null) return null;

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

        Pattern pattern = Pattern.compile("\\p{M}");
        String accentRemoved = pattern.matcher(normalized).replaceAll("");

        return accentRemoved.toLowerCase()
                .replaceAll("[^a-z0-9 ]", "")
                .strip()
                .replaceAll("\\s+", "-");
    }
}