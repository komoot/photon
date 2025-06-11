package de.komoot.photon.searcher;

import java.util.Objects;

/**
 * Filter description for a single filter by OSM key or key/value.
 */
public class TagFilter {
    private final TagFilterKind kind;
    private final String key;
    private final String value;

    public TagFilter(TagFilterKind kind, String key, String value) {
        this.kind = kind;
        this.key = key;
        this.value = value;
    }

    public TagFilterKind getKind() {
        return kind;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public boolean isKeyOnly() {
        return value == null;
    }

    public boolean isValueOnly() {
        return key == null;
    }

    /**
     * Create a new tag filter from a osm-tag filter description.
     *
     * @param filter  Tag filter description.
     *
     * @return The appropriate tag filter object or null if the filter string has an invalid format.
     */
    public static TagFilter buildOsmTagFilter(String filter) {
        TagFilterKind kind = null;
        String key = null;
        String value = null;

        String[] parts = splitFilter(filter);

        if (parts.length == 2) {
            boolean excludeKey = parts[0].startsWith("!");
            boolean excludeValue = parts[1].startsWith("!");

            key = (excludeKey ? parts[0].substring(1) : parts[0]).trim();
            if (key.isEmpty()) {
                key = null;
            }
            value = (excludeValue ? parts[1].substring(1) : parts[1]).trim();

            if (!value.isEmpty()) {
                if (key != null && !excludeKey && excludeValue) {
                    kind = TagFilterKind.EXCLUDE_VALUE;
                } else {
                    kind = excludeKey || excludeValue ? TagFilterKind.EXCLUDE : TagFilterKind.INCLUDE;
                }
            }
        } else if (parts.length == 1 && parts[0].equals(filter)) {
            boolean exclude = filter.startsWith("!");

            key = exclude ? filter.substring(1) : filter;

            if (!key.isEmpty()) {
                kind = exclude ? TagFilterKind.EXCLUDE : TagFilterKind.INCLUDE;
            }
        }

        return (kind == null) ? null : new TagFilter(kind, key, value);
    }

    // Split the filter string by colon, and handle values with colons in "extra" tags.
    private static String[] splitFilter(String filter) {
        String[] parts = filter.split(":");
        if (parts.length > 2 && parts[0].startsWith("extra.")) {
            return filter.split(":", 2);
        }
        return parts;
    }

    @Override
    public String toString() {
        return "TagFilter{" +
                "kind=" + kind +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TagFilter tagFilter = (TagFilter) o;
        return kind == tagFilter.kind && Objects.equals(key, tagFilter.key) && Objects.equals(value, tagFilter.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, key, value);
    }
}
