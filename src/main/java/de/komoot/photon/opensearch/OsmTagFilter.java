package de.komoot.photon.opensearch;

import de.komoot.photon.searcher.TagFilter;
import de.komoot.photon.searcher.TagFilterKind;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TermsQuery;

import java.util.Collections;
import java.util.List;

public class OsmTagFilter {
    private BoolQuery.Builder includeTagQueryBuilder = null;
    private BoolQuery.Builder excludeTagQueryBuilder = null;

    public OsmTagFilter withOsmTagFilters(List<TagFilter> filters) {
        for (var filter : filters) {
            addOsmTagFilter(filter);
        }
        return this;
    }

    public Query build() {
        if (includeTagQueryBuilder != null || excludeTagQueryBuilder != null) {
            return BoolQuery.of(q -> {
                if (includeTagQueryBuilder != null) {
                    q.must(includeTagQueryBuilder.build().toQuery());
                }
                if (excludeTagQueryBuilder != null) {
                    q.mustNot(excludeTagQueryBuilder.build().toQuery());
                }
                return q;
            }).toQuery();
        }

        return null;
    }

    private void addOsmTagFilter(TagFilter filter) {
        if (filter.kind() == TagFilterKind.EXCLUDE_VALUE) {
            appendIncludeTerm(BoolQuery.of(q -> q
                    .must(makeTermsQuery("osm_key", filter.key()))
                    .mustNot(makeTermsQuery("osm_value", filter.value()))).toQuery());
        } else {
            Query query;
            if (filter.isKeyOnly()) {
                query = makeTermsQuery("osm_key", filter.key());
            } else if (filter.isValueOnly()) {
                query = makeTermsQuery("osm_value", filter.value());
            } else {
                query = BoolQuery.of(q -> q
                        .must(makeTermsQuery("osm_key", filter.key()))
                        .must(makeTermsQuery("osm_value", filter.value()))).toQuery();
            }

            if (filter.kind() == TagFilterKind.INCLUDE) {
                appendIncludeTerm(query);
            } else {
                appendExcludeTerm(query);
            }
        }
    }

    private void appendIncludeTerm(Query query) {
        if (includeTagQueryBuilder == null) {
            includeTagQueryBuilder = new BoolQuery.Builder();
        }

        includeTagQueryBuilder.should(query);
    }

    private void appendExcludeTerm(Query query) {
        if (excludeTagQueryBuilder == null) {
            excludeTagQueryBuilder = new BoolQuery.Builder();
        }

        excludeTagQueryBuilder.should(query);
    }

    private static Query makeTermsQuery(String field, String term) {
        return TermsQuery.of(q -> q
                .field(field)
                .terms(t -> t.value(Collections.singletonList(FieldValue.of(term))))).toQuery();
    }
}
