package de.komoot.photon.query;

import de.komoot.photon.searcher.TagFilter;

import java.util.*;
import java.util.stream.Collectors;

public class RequestBase {
    private String language = "default";
    private int limit = 15;
    private boolean debug = false;
    private boolean dedupe = true;
    private boolean returnGeometry = false;

    private final List<TagFilter> osmTagFilters = new ArrayList<>(1);
    private final Set<String> layerFilters = new HashSet<>(1);
    private final List<String> includeCategories = new ArrayList<>();
    private final List<String> excludeCategories = new ArrayList<>();

    public String getLanguage() {
        return language;
    }

    public int getLimit() {
        return limit;
    }

    public boolean getDebug() {
        return debug;
    }

    public boolean getDedupe() {
        return dedupe;
    }

    public boolean getReturnGeometry() {
        return returnGeometry;
    }

    public List<TagFilter> getOsmTagFilters() {
        return osmTagFilters;
    }

    public Set<String> getLayerFilters() {
        return layerFilters;
    }

    public List<String> getIncluded() { return includeCategories; }

    public List<String> getExcluded() { return excludeCategories; }

    public void setLanguage(String language) {
        if (language != null) {
            this.language = language;
        }
    }

    public void setLimit(Integer limit, int maxLimit) {
        if (limit != null) {
            this.limit = Integer.max(1, Integer.min(maxLimit, limit));
        }
    }

    public void setDebug(Boolean debug) {
        if (debug != null) {
            this.debug = debug;
        }
    }

    public void setDedupe(Boolean dedupe) {
        if (dedupe != null) {
            this.dedupe = dedupe;
        }
    }

    public void setReturnGeometry(Boolean returnGeometry) {
        if (returnGeometry != null) {
            this.returnGeometry = returnGeometry;
        }
    }

    void addOsmTagFilter(TagFilter filter) {
        osmTagFilters.add(filter);
    }

    void addLayerFilters(Collection<String> filters) {
        layerFilters.addAll(filters);
    }

    void addIncludeCategory(List<String> cats) {
        includeCategories.addAll(cats);
    }

    void addExcludeCategory(List<String> cats) {
        excludeCategories.addAll(cats);
    }
}
