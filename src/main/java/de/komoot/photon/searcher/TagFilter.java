package de.komoot.photon.searcher;

import de.komoot.photon.query.BadRequestException;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * A single filter instruction for OSM keys and values.
 *
 */
@Slf4j
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

    public static TagFilter buildOsmTagFilter(String filter) throws BadRequestException {
        TagFilterKind kind = null;
        String key = null;
        String value = null;

        String[] parts = filter.split(":");

        if (parts.length == 2) {
            boolean excludeKey = parts[0].startsWith("!");
            boolean excludeValue = parts[1].startsWith("!");

            key = excludeKey ? parts[0].substring(1) : parts[0];
            if (key.isEmpty()) {
                key = null;
            }
            value = excludeValue ? parts[1].substring(1) : parts[1];

            if (!value.isEmpty()) {
                if (key != null && !excludeKey && excludeValue) {
                    kind = TagFilterKind.EXCLUDE_VALUE;
                } else {
                    kind = excludeKey || excludeValue ? TagFilterKind.EXCLUDE : TagFilterKind.INCLUDE;
                }
            }
        } else if (parts.length == 1) {
            boolean exclude = filter.startsWith("!");

            key = exclude ? filter.substring(1) : filter;

            if (!key.isEmpty()) {
                kind = exclude ? TagFilterKind.EXCLUDE : TagFilterKind.INCLUDE;
            }
        }

        if (kind == null) {
            throw new BadRequestException(400, String.format("Invalid filter expression osm_tag=%s.", filter));
        }

        return new TagFilter(kind, key, value);
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
