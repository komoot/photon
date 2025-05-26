package de.komoot.photon.nominatim.model;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ContextMap extends AbstractMap<String, Set<String>> {
    private final Map<String, Set<String>> entries = new HashMap<>();

    public void addName(String key, String name) {
        if (name != null) {
            entries.computeIfAbsent(key, k -> new HashSet<>()).add(name);
        }
    }

    public void addAll(Map<String, String> map) {
        for (var entry: map.entrySet()) {
            addName(entry.getKey(), entry.getValue());
        }
    }

    public void addAll(ContextMap map) {
        for (var entry : map.entrySet()) {
            entries.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).addAll(entry.getValue());
        }
    }

    @NotNull
    @Override
    public Set<Entry<String, Set<String>>> entrySet() {
        return entries.entrySet();
    }
}
