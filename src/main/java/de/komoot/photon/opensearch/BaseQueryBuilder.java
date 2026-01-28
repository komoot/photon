package de.komoot.photon.opensearch;

import de.komoot.photon.searcher.TagFilter;
import org.jspecify.annotations.NullMarked;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides the basic query structure as well as functions to
 * add sub-queries for the common query parameters.
 */
@NullMarked
public class BaseQueryBuilder {
    protected final BoolQuery.Builder outerQuery = new BoolQuery.Builder();

    public Query build() {
        return outerQuery.build().toQuery();
    }

    public void addOsmTagFilter(List<TagFilter> filters) {
        if (!filters.isEmpty()) {
            var query = new OsmTagFilter().withOsmTagFilters(filters).build();
            if (query != null) {
                outerQuery.filter(query);
            }
        }
    }

    public void addLayerFilter(Collection<String> layers) {
        if (!layers.isEmpty()) {
            outerQuery.filter(f -> f.terms(t -> t
                    .field("type")
                    .terms(tm -> tm.value(
                            layers.stream().map(FieldValue::of).collect(Collectors.toList())
                    ))
            ));
        }
    }

    public void includeCategories(Collection<String> queryTerms) {
        for (var term : queryTerms) {
            outerQuery.filter(new CategoryFilter(term).buildIncludeQuery());
        }
    }

    public void excludeCategories(Collection<String> queryTerms) {
        for (var term : queryTerms) {
            outerQuery.filter(new CategoryFilter(term).buildExcludeQuery());
        }
    }
}
