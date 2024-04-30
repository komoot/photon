package de.komoot.photon;

/**
 * Helper functions to convert a photon document to XContentBuilder object / JSON
 */
public class Utils {

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
        if ("place".equals(key) || "building".equals(key)) {
            return null;
        }

        if ("highway".equals(key)
            && ("unclassified".equals(value) || "residential".equals(value))) {
            return null;
        }

        for (char c : value.toCharArray()) {
            if (!(c == '_'
                  || ((c >= 'a') && (c <= 'z'))
                  || ((c >= 'A') && (c <= 'Z'))
                  || ((c >= '0') && (c <= '9')))) {
                return null;
            }
        }

        return "tpfld" + value.replace("_", "").toLowerCase() + "clsfld" + key.replace("_", "").toLowerCase();
    }
}
