package de.komoot.photon.query;

import de.komoot.photon.searcher.TagFilter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;

@NullMarked
public class RequestBase {
    private String language = "default";
    private int limit = 15;
    private boolean debug = false;
    private boolean dedupe = true;
    private boolean returnGeometry = false;

    private final List<TagFilter> osmTagFilters = new ArrayList<>(1);
    private final Set<String> layerFilters = new HashSet<>(1);
    private final Set<String> includeCategories = new HashSet<>();
    private final Set<String> excludeCategories = new HashSet<>();

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

    public Set<String> getIncludeCategories() {
        return includeCategories;
    }

    public Set<String> getExcludeCategories() {
        return excludeCategories;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setLimit(@Nullable Integer limit, int maxLimit) {
        if (limit != null) {
            this.limit = Integer.max(1, Integer.min(maxLimit, limit));
        }
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setDedupe(boolean dedupe) {
        this.dedupe = dedupe;
    }

    public void setReturnGeometry(boolean returnGeometry) {
        this.returnGeometry = returnGeometry;
    }

    void addOsmTagFilter(TagFilter filter) {
        osmTagFilters.add(filter);
    }

    void addLayerFilters(Collection<String> filters) {
        layerFilters.addAll(filters);
    }

    void addIncludeCategories(Collection<String> categories) {
        includeCategories.addAll(categories);
    }

    void addExcludeCategories(Collection<String> categories) {
        excludeCategories.addAll(categories);
    }
}
