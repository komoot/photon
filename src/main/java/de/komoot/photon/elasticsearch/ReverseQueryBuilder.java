package de.komoot.photon.elasticsearch;

import com.vividsolutions.jts.geom.Point;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;

import java.util.Set;

/**
 * @author svantulden
 */
public class ReverseQueryBuilder {
    private Double radius;
    private Point location;
    private String queryStringFilter;
    private Set<String> objectTypeFilter;

    private ReverseQueryBuilder(Point location, Double radius, String queryStringFilter, Set<String> objectTypeFilter) {
        this.location = location;
        this.radius = radius;
        this.queryStringFilter = queryStringFilter;
        this.objectTypeFilter = objectTypeFilter;
    }

    public static ReverseQueryBuilder builder(Point location, Double radius, String queryStringFilter, Set<String> objectTypeFilter) {
        return new ReverseQueryBuilder(location, radius, queryStringFilter, objectTypeFilter);
    }

    public QueryBuilder buildQuery() {
        QueryBuilder fb = QueryBuilders.geoDistanceQuery("coordinate").point(location.getY(), location.getX())
                .distance(radius, DistanceUnit.KILOMETERS);

        BoolQueryBuilder finalQuery = QueryBuilders.boolQuery();

        if (queryStringFilter != null && queryStringFilter.trim().length() > 0)
            finalQuery.must(QueryBuilders.queryStringQuery(queryStringFilter));

        if (objectTypeFilter.size() > 0) {
            finalQuery.must(new TermsQueryBuilder("object_type", objectTypeFilter));
        }

        if (finalQuery.must().size() == 0) {
            finalQuery.must(QueryBuilders.matchAllQuery());
        }

        return finalQuery.filter(fb);
    }
}
