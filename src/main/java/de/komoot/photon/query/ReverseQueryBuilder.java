package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Point;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * @author svantulden
 */
public class ReverseQueryBuilder {
    private Double radius;
    private Point location;
    private String queryStringFilter;

    private ReverseQueryBuilder(Point location, Double radius, String queryStringFilter) {
        this.location = location;
        this.radius = radius;
        this.queryStringFilter = queryStringFilter;
    }

    public static ReverseQueryBuilder builder(Point location, Double radius, String queryStringFilter) {
        return new ReverseQueryBuilder(location, radius, queryStringFilter);
    }

    public QueryBuilder buildQuery() {
        QueryBuilder fb = QueryBuilders.geoDistanceQuery("coordinate").point(location.getY(), location.getX())
                .distance(radius, DistanceUnit.KILOMETERS);

        BoolQueryBuilder finalQuery;

        if (queryStringFilter != null && queryStringFilter.trim().length() > 0)
            finalQuery = QueryBuilders.boolQuery().must(QueryBuilders.queryStringQuery(queryStringFilter)).filter(fb);
        else
            finalQuery = QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery()).filter(fb);

        return finalQuery;
    }
}
