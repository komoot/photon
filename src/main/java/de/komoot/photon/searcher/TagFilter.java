package de.komoot.photon.searcher;

import de.komoot.photon.query.BadRequestException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Filter description for a single filter by OSM key or key/value.
 */
@NullMarked
public record TagFilter(TagFilterKind kind, @Nullable String key, @Nullable String value) {

    public boolean isKeyOnly() {
        return value == null;
    }

    public boolean isValueOnly() {
        return key == null;
    }

    /**
     * Create a new tag filter from a osm-tag filter description.
     *
     * @param filter Tag filter description.
     * @return The appropriate tag filter object.
     * @throws BadRequestException when the format of the value is wrong.
     */
    public static TagFilter buildOsmTagFilter(String filter) {
        TagFilterKind kind = null;
        String key = null;
        String value = null;

        String[] parts = filter.split(":");

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

        if (kind == null) {
            throw new BadRequestException(400, "Invalid format for osm_tag parameter.");
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

}
