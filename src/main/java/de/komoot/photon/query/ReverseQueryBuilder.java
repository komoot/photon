package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Point;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.*;

/**
 * @author svantulden
 */
public class ReverseQueryBuilder {
    private Double radius;
    private Point location;
    private String queryStringFilter;

    private Set<String> extraKeys;

    private ReverseQueryBuilder(Point location, Double radius, String queryStringFilter, Set<String> extraKeys) {
        this.location = location;
        this.radius = radius;
        this.queryStringFilter = queryStringFilter;
        this.extraKeys = extraKeys;
    }

    private Boolean checkTags(Set<String> keys) {
        return !(keys == null || keys.isEmpty());
    }

    public static ReverseQueryBuilder builder(Point location, Double radius, String queryStringFilter, Set<String> extraKeys) {
        return new ReverseQueryBuilder(location, radius, queryStringFilter, extraKeys);
    }

    public QueryBuilder buildQuery() {
        QueryBuilder fb = QueryBuilders.geoDistanceQuery("coordinate").point(location.getY(), location.getX())
                .distance(radius, DistanceUnit.KILOMETERS);

        BoolQueryBuilder finalQuery;

        if (queryStringFilter != null && queryStringFilter.trim().length() > 0)
            finalQuery = QueryBuilders.boolQuery().must(QueryBuilders.queryStringQuery(queryStringFilter)).filter(fb);
        else
            finalQuery = QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery()).filter(fb);

        if (checkTags(extraKeys) != null) {
            BoolQueryBuilder orQueryExtraTagFiltering = QueryBuilders.boolQuery();

            for (String key : extraKeys) {
                orQueryExtraTagFiltering.should(QueryBuilders.existsQuery("extra." + key));
            }

            finalQuery.filter(QueryBuilders.boolQuery().must(orQueryExtraTagFiltering));
        }

        return finalQuery;
    }
}
