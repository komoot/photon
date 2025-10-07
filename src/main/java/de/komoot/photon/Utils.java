package de.komoot.photon;

import java.util.regex.Pattern;

/**
 * Helper functions to convert a photon document to XContentBuilder object / JSON
 */
public class Utils {
    static final Pattern STRING_PATTERN = Pattern.compile("[^a-zA-Z0-9_-]");

    // http://stackoverflow.com/a/4031040/1437096
    public static String stripNonDigits(
            final CharSequence input /* inspired by seh's comment */) {
        final StringBuilder sb = new StringBuilder(
                input.length() /* also inspired by seh's comment */);
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (c > 47 && c < 58) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String buildClassificationString(String key, String value) {
        if (STRING_PATTERN.matcher(key).find() || STRING_PATTERN.matcher(value).find()) {
            return null;
        }

        return String.format("#osm.%s.%s", key, value);
    }
}
