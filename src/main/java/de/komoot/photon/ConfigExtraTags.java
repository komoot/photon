package de.komoot.photon;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigExtraTags {

    private final boolean allowAll;
    private final String[] tags;

    public ConfigExtraTags() {
        allowAll = false;
        tags = new String[0];
    }

    public ConfigExtraTags(List<String> tags) {
        this.allowAll = tags.size() == 1 && "ALL".equals(tags.getFirst());
        this.tags = allowAll ? null : tags.toArray(new String[0]);
    }

    public void writeFilteredExtraTags(JsonGenerator writer, String fieldName, Map<String, String> sourceTags) throws IOException  {
         if (!sourceTags.isEmpty()) {
            if (allowAll) {
                writer.writeObjectField(fieldName, sourceTags);
            } else if (tags.length > 0) {
                boolean foundTag = false;

                for (String tag : tags) {
                    String value = sourceTags.get(tag);
                    if (value != null) {
                        if (!foundTag) {
                            writer.writeObjectFieldStart(fieldName);
                            foundTag = true;
                        }
                        writer.writeStringField(tag, value);
                    }
                }

                if (foundTag) {
                    writer.writeEndObject();
                }
            }
        }
    }

    public Map<String, String> filterExtraTags(Map<String, String> sourceTags) {
        if (allowAll || sourceTags.isEmpty()) {
            return sourceTags;
        }

        if (tags.length == 0) {
            return Map.of();
        }

        final Map<String, String> newMap = new HashMap<>();
        for (var key : tags) {
            final var value = sourceTags.get(key);
            if (value != null) {
                newMap.put(key, value);
            }
        }

        return newMap;
    }

    public List<String> asConfigParam() {
        if (allowAll) {
            return List.of("ALL");
        }

        return Arrays.stream(tags).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return allowAll ? "<ALL>" : Arrays.toString(tags);
    }
}
