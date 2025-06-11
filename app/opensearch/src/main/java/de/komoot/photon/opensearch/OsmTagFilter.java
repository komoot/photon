package de.komoot.photon.opensearch;

import de.komoot.photon.searcher.TagFilter;
import de.komoot.photon.searcher.TagFilterKind;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TermsQuery;
import org.opensearch.client.opensearch._types.query_dsl.WildcardQuery;

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
        String effectiveKey = filter.getKey();
        String value = filter.getValue();
        boolean isExtraTag = effectiveKey != null && effectiveKey.startsWith("extra.");

        if (filter.getKind() == TagFilterKind.EXCLUDE_VALUE) {
            Query condition;
            if (isExtraTag) {
                BoolQuery.Builder boolQuery = new BoolQuery.Builder();
                boolQuery.must(q -> q.exists(e -> e.field(effectiveKey)));
                if (hasWildcard(value)) {
                    boolQuery.mustNot(makeWildcardQuery(effectiveKey, value));
                } else {
                    boolQuery.mustNot(makeTermsQuery(effectiveKey, value));
                }
                condition = boolQuery.build().toQuery();
            } else {
                condition = BoolQuery.of(bq -> bq
                        .must(makeTermsQuery("osm_key", effectiveKey))
                        .mustNot(makeTermsQuery("osm_value", value))
                ).toQuery();
            }
            appendIncludeTerm(condition);
        } else {
            Query query;
            if (filter.isKeyOnly()) {
                query = makeTermsQuery("osm_key", effectiveKey);
            } else if (filter.isValueOnly()) {
                query = makeTermsQuery("osm_value", value);
            } else {
                if (isExtraTag) {
                    if (hasWildcard(value)) {
                        query = makeWildcardQuery(effectiveKey, value);
                    } else {
                        query = makeTermsQuery(effectiveKey, value);
                    }
                } else {
                    query = BoolQuery.of(bq -> bq
                            .must(makeTermsQuery("osm_key", effectiveKey))
                            .must(makeTermsQuery("osm_value", value))
                    ).toQuery();
                }
            }

            if (filter.getKind() == TagFilterKind.INCLUDE) {
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

    private Query makeWildcardQuery(String fieldName, String wildcardValue) {
        return WildcardQuery.of(w -> w
                .field(fieldName)
                .wildcard(wildcardValue) // Use .wildcard for the pattern
        ).toQuery();
    }

    private static boolean hasWildcard(String value) {
        return value != null && (value.contains("*") || value.contains("?"));
    }
}
