package de.komoot.photon.elasticsearch;

import org.locationtech.jts.geom.Point;
import de.komoot.photon.searcher.TagFilter;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;

import java.util.List;
import java.util.Set;

/**
 * Query builder creating a ElasticSearch query for reverse searching.
 */
public class ReverseQueryBuilder {
    private Double radius;
    private Point location;
    private String queryStringFilter;
    private Set<String> layerFilter;

    private OsmTagFilter osmTagFilter;

    private ReverseQueryBuilder(Point location, Double radius, String queryStringFilter, Set<String> layerFilter) {
        this.location = location;
        this.radius = radius;
        this.queryStringFilter = queryStringFilter;
        this.layerFilter = layerFilter;
        this.osmTagFilter = new OsmTagFilter();
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

        if (!layerFilter.isEmpty()) {
            finalQuery.must(new TermsQueryBuilder("type", layerFilter));
        }

        BoolQueryBuilder tagFilters = osmTagFilter.getTagFiltersQuery();
        if (tagFilters != null) {
            finalQuery.filter(tagFilters);
        }

        if (finalQuery.must().isEmpty()) {
            finalQuery.must(QueryBuilders.matchAllQuery());
        }

        return finalQuery.filter(fb);
    }

    public ReverseQueryBuilder withOsmTagFilters(List<TagFilter> filters) {
        osmTagFilter.withOsmTagFilters(filters);
        return this;
    }
}
