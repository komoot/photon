package de.komoot.photon.nominatim.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@NullMarked
public class NameMap extends AbstractMap<String, String> {
    private final LinkedHashMap<String, String> entries = new LinkedHashMap<>();

    private static final Map<String, String[]> LOCALE_KEYS = new ConcurrentHashMap<>();

    @Override
    public Set<Entry<String, String>> entrySet() {
        return entries.entrySet();
    }

    @Override
    public boolean containsKey(Object key) {
        return entries.containsKey(key);
    }

    @Override
    public String get(Object key) {
        return entries.get(key);
    }

    public static NameMap makeForPlace(Map<String, @Nullable String> source, Iterable<String> languages) {
        return new NameMap()
                .setLocaleNames(source, languages)
                .setName("alt", source, "_place_alt_name", "alt_name")
                .setName("int", source, "_place_int_name", "int_name")
                .setName("loc", source, "_place_loc_name", "loc_name")
                .setName("old", source, "_place_old_name", "old_name")
                .setName("reg", source, "_place_reg_name", "reg_name")
                .setName("housename", source,"addr:housename");
    }

    NameMap setLocaleNames(Map<String, @Nullable String> source, Iterable<String> languages) {
        setName("default", source, "_place_name", "name");
        for (var lang : languages) {
            String[] keys = LOCALE_KEYS.computeIfAbsent(lang,
                    l -> new String[]{"_place_name:" + l, "name:" + l});
            setName(lang, source, keys);
        }
        return this;
    }

    NameMap setName(String field, Map<String, @Nullable String> source, String... keys) {
        if (!entries.containsKey(field)) {
            for (String key : keys) {
                String val = source.get(key);
                if (val != null) {
                    entries.put(field, val);
                    break;
                }
            }
        }
        return this;
    }
}
