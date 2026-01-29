package de.komoot.photon.nominatim.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;

@NullMarked
public class ContextMap extends AbstractMap<String, Set<String>> {
    private final Map<String, Set<String>> entries = new HashMap<>();

    public void addName(String key, @Nullable String name) {
        if (name != null) {
            entries.computeIfAbsent(key, k -> new HashSet<>()).add(name);
        }
    }

    public void addAll(Map<String, @Nullable String> map) {
        for (var entry: map.entrySet()) {
            addName(entry.getKey(), entry.getValue());
        }
    }

    public void addAll(ContextMap map) {
        for (var entry : map.entrySet()) {
            entries.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).addAll(entry.getValue());
        }
    }

    @Override
    public Set<Entry<String, Set<String>>> entrySet() {
        return entries.entrySet();
    }
}
