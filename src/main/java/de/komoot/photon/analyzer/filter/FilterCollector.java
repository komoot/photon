package de.komoot.photon.analyzer.filter;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.analyzer.config.FilterConfig;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@NullMarked
public class FilterCollector {
    boolean hasLang = false;
    boolean hasCountry = false;
    Set<String> categories = new HashSet<>();
    Set<String> extra = new HashSet<>();

    public void update(FilterConfig cfg) {
        if (cfg.getLang() != null) {
            hasLang = true;
        }
        if (cfg.getCountry() != null) {
            hasCountry = true;
        }
        categories.addAll(cfg.getCategories());
        extra.addAll(cfg.getExtra().keySet());
    }

    public List<Function<PhotonDoc, @Nullable String>> createPhotonDocFilters() {
        var funcs = new ArrayList<Function<PhotonDoc, @Nullable String>>();
        if (hasCountry) {
            funcs.add(PhotonDoc::getCountryCode);
        }
        for (var c : categories) {
            funcs.add(p -> p.getCategories().contains(c) ? "1" : "0");
        }
        for (var e : extra) {
            funcs.add(new PhotonDocExtraFilter(e));
        }

        return funcs;
    }

    public List<Function<String, @Nullable String>> createNameFilters() {
        if (hasLang) {
            return List.of(s -> s);
        }

        return List.of();
    }

    public List<@Nullable String> getTransformerFilter(FilterConfig config) {
        var filters = new ArrayList<@Nullable String>();
        if (hasCountry) {
            filters.add(config.getCountry() == null ? null : config.getCountry().toUpperCase());
        }
        for (var c : categories) {
            filters.add(config.getCategories().contains(c) ? "1" : "0");
        }
        for (var e : extra) {
            filters.add(config.getExtra().get(e));
        }
        if (hasLang) {
            filters.add(config.getLang());
        }

        return filters;
    }

    private record PhotonDocExtraFilter(String extraKey) implements Function<PhotonDoc, @Nullable String> {

        @Override
        public @Nullable String apply(PhotonDoc photonDoc) {
            var value = photonDoc.getExtratags().get(extraKey);
            return (value instanceof String) ? (String) value : null;
        }
    }
}