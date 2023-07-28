package de.komoot.photon.elasticsearch;

import java.util.List;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import de.komoot.photon.searcher.TagFilter;
import de.komoot.photon.searcher.TagFilterKind;

public class OsmTagFilter {
    private BoolQueryBuilder orQueryBuilderForIncludeTagFiltering = null;
    private BoolQueryBuilder andQueryBuilderForExcludeTagFiltering = null;
    
    public OsmTagFilter withOsmTagFilters(List<TagFilter> filters) {
        for (TagFilter filter : filters) {
            addOsmTagFilter(filter);
        }
        return this;
    }

    public BoolQueryBuilder getTagFiltersQuery() {
        if (orQueryBuilderForIncludeTagFiltering != null || andQueryBuilderForExcludeTagFiltering != null) {
            BoolQueryBuilder tagFilters = QueryBuilders.boolQuery();
            if (orQueryBuilderForIncludeTagFiltering != null)
                tagFilters.must(orQueryBuilderForIncludeTagFiltering);
            if (andQueryBuilderForExcludeTagFiltering != null)
                tagFilters.mustNot(andQueryBuilderForExcludeTagFiltering);
            return tagFilters;
        }
        return null;
    }

    private OsmTagFilter addOsmTagFilter(TagFilter filter) {
        if (filter.getKind() == TagFilterKind.EXCLUDE_VALUE) {
            appendIncludeTermQuery(QueryBuilders.boolQuery()
                    .must(QueryBuilders.termQuery("osm_key", filter.getKey()))
                    .mustNot(QueryBuilders.termQuery("osm_value", filter.getValue())));
        } else {
            QueryBuilder builder;
            if (filter.isKeyOnly()) {
                builder = QueryBuilders.termQuery("osm_key", filter.getKey());
            } else if (filter.isValueOnly()) {
                builder = QueryBuilders.termQuery("osm_value", filter.getValue());
            } else {
                builder = QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery("osm_key", filter.getKey()))
                        .must(QueryBuilders.termQuery("osm_value", filter.getValue()));
            }
            if (filter.getKind() == TagFilterKind.INCLUDE) {
                appendIncludeTermQuery(builder);
            } else {
                appendExcludeTermQuery(builder);
            }
        }
        return this;
    }

    private void appendIncludeTermQuery(QueryBuilder termQuery) {
        if (orQueryBuilderForIncludeTagFiltering == null)
            orQueryBuilderForIncludeTagFiltering = QueryBuilders.boolQuery();

        orQueryBuilderForIncludeTagFiltering.should(termQuery);
    }


    private void appendExcludeTermQuery(QueryBuilder termQuery) {
        if (andQueryBuilderForExcludeTagFiltering == null)
            andQueryBuilderForExcludeTagFiltering = QueryBuilders.boolQuery();

        andQueryBuilderForExcludeTagFiltering.should(termQuery);
    }
}
