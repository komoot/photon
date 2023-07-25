package de.komoot.photon.elasticsearch;

import com.vividsolutions.jts.geom.Point;
import de.komoot.photon.searcher.TagFilter;
import de.komoot.photon.searcher.TagFilterKind;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;

import java.util.List;
import java.util.Set;

/**
 * @author svantulden
 */
public class ReverseQueryBuilder {
    private Double radius;
    private Point location;
    private String queryStringFilter;
    private Set<String> layerFilter;

    private BoolQueryBuilder orQueryBuilderForIncludeTagFiltering = null;
    private BoolQueryBuilder andQueryBuilderForExcludeTagFiltering = null;

    private ReverseQueryBuilder(Point location, Double radius, String queryStringFilter, Set<String> layerFilter) {
        this.location = location;
        this.radius = radius;
        this.queryStringFilter = queryStringFilter;
        this.layerFilter = layerFilter;
    }

    public static ReverseQueryBuilder builder(Point location, Double radius, String queryStringFilter, Set<String> layerFilter) {
        return new ReverseQueryBuilder(location, radius, queryStringFilter, layerFilter);
    }

    public QueryBuilder buildQuery() {
        QueryBuilder fb = QueryBuilders.geoDistanceQuery("coordinate").point(location.getY(), location.getX())
                .distance(radius, DistanceUnit.KILOMETERS);

        BoolQueryBuilder finalQuery = QueryBuilders.boolQuery();

        if (queryStringFilter != null && queryStringFilter.trim().length() > 0)
            finalQuery.must(QueryBuilders.queryStringQuery(queryStringFilter));

        if (layerFilter.size() > 0) {
            finalQuery.must(new TermsQueryBuilder("type", layerFilter));
        }

        if (orQueryBuilderForIncludeTagFiltering != null || andQueryBuilderForExcludeTagFiltering != null) {
            BoolQueryBuilder tagFilters = QueryBuilders.boolQuery();
            if (orQueryBuilderForIncludeTagFiltering != null)
                tagFilters.must(orQueryBuilderForIncludeTagFiltering);
            if (andQueryBuilderForExcludeTagFiltering != null)
                tagFilters.mustNot(andQueryBuilderForExcludeTagFiltering);
            finalQuery.filter(tagFilters);
        }

        if (finalQuery.must().size() == 0) {
            finalQuery.must(QueryBuilders.matchAllQuery());
        }

        return finalQuery.filter(fb);
    }

    public ReverseQueryBuilder withOsmTagFilters(List<TagFilter> filters) {
        for (TagFilter filter : filters) {
            addOsmTagFilter(filter);
        }
        return this;
    }

    public ReverseQueryBuilder addOsmTagFilter(TagFilter filter) {
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
