package de.komoot.photon;

import java.util.regex.Pattern;

public class ConfigClassificationTerm {
    private static final Pattern LABEL_PATTERN = Pattern.compile(
            String.format("[%s]+", PhotonDoc.CATEGORY_VALID_CHARS)
    );

    private String key;
    private String value;
    private String[] terms;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String[] getTerms() {
        return terms;
    }

    public void setTerms(String[] terms) {
        this.terms = terms;
    }

    public boolean isValidCategory() {
        return LABEL_PATTERN.matcher(key).matches() && LABEL_PATTERN.matcher(value).matches();
    }

    public String getClassificationString() {
        return String.format("#osm.%s.%s", key, value);
    }
}
