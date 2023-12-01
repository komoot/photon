package de.komoot.photon.elasticsearch;

import java.util.List;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.util.ObjectBuilder;

import de.komoot.photon.searcher.TagFilter;
import de.komoot.photon.searcher.TagFilterKind;

public class OsmTagFilter {
    private BoolQuery.Builder orQueryBuilderForIncludeTagFiltering = null;
    private BoolQuery.Builder andQueryBuilderForExcludeTagFiltering = null;
    
    public void withOsmTagFilters(List<TagFilter> filters) {
        for (TagFilter filter : filters) {
            addOsmTagFilter(filter);
        }
    }

    public Query getTagFiltersQuery() {
        if (orQueryBuilderForIncludeTagFiltering != null || andQueryBuilderForExcludeTagFiltering != null) {
            BoolQuery.Builder tagFilters = new BoolQuery.Builder();
            if (orQueryBuilderForIncludeTagFiltering != null)
                tagFilters.must(orQueryBuilderForIncludeTagFiltering.build()._toQuery());
            if (andQueryBuilderForExcludeTagFiltering != null)
                tagFilters.mustNot(andQueryBuilderForExcludeTagFiltering.build()._toQuery());
            return tagFilters.build()._toQuery();
        }
        return null;
    }

    private OsmTagFilter addOsmTagFilter(TagFilter filter) {
        if (filter.getKind() == TagFilterKind.EXCLUDE_VALUE) {
            appendIncludeTermQuery(new BoolQuery.Builder()
                    .must(q -> q
                            .term(t -> t
                                    .field("osm_key")
                                    .value(filter.getKey()
                                    )
                            )
                    )
                    .mustNot(q -> q
                            .term(t -> t
                                    .field("osm_value")
                                    .value(filter.getValue())
                            )
                    )
                    .build()
                    ._toQuery()
            );
        } else {
            ObjectBuilder<Query> builder;
            if (filter.isKeyOnly()) {
                builder = new Query.Builder()
                        .term(t -> t
                            .field("osm_key")
                            .value(filter.getKey())
                        );
            } else if (filter.isValueOnly()) {
                builder = new Query.Builder()
                        .term(t -> t
                            .field("osm_value")
                            .value(filter.getValue())
                        );
            } else {
                builder = new Query.Builder()
                        .bool(b -> b
                            .must(q -> q
                                    .term(t -> t
                                            .field("osm_key")
                                            .value(filter.getKey())
                                    )
                            )
                            .must(q -> q
                                    .term(t -> t
                                            .field("osm_value")
                                            .value(filter.getValue())
                                    )
                            )
                        );
            }
            if (filter.getKind() == TagFilterKind.INCLUDE) {
                appendIncludeTermQuery(builder.build());
            } else {
                appendExcludeTermQuery(builder.build());
            }
        }
        return this;
    }

    private void appendIncludeTermQuery(Query termQuery) {
        if (orQueryBuilderForIncludeTagFiltering == null)
            orQueryBuilderForIncludeTagFiltering = new BoolQuery.Builder();

        orQueryBuilderForIncludeTagFiltering.should(termQuery);
    }


    private void appendExcludeTermQuery(Query termQuery) {
        if (andQueryBuilderForExcludeTagFiltering == null)
            andQueryBuilderForExcludeTagFiltering = new BoolQuery.Builder();

        andQueryBuilderForExcludeTagFiltering.should(termQuery);
    }
}
