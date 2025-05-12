package de.komoot.photon;

public class ConfigClassificationTerm {

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

    public String getClassificationString() {
        return Utils.buildClassificationString(key, value);
    }
}
