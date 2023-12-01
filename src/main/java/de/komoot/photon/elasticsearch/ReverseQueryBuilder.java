package de.komoot.photon.elasticsearch;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.vividsolutions.jts.geom.Point;
import de.komoot.photon.searcher.TagFilter;

import java.util.List;
import java.util.Set;

/**
 * @author svantulden
 */
public class ReverseQueryBuilder {
    private final Double radius;
    private final Point location;
    private final String queryStringFilter;
    private final Set<String> layerFilter;
    private final OsmTagFilter osmTagFilter;

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

    public Query buildQuery() {
        Query fb = new Query.Builder().geoDistance(gdq -> gdq
                .field("coordinate")
                .location(loc -> loc.latlon(l -> l.lat(location.getY()).lon(location.getX())))
                .distance(String.format("%skm", radius))
        ).build();

        BoolQuery.Builder finalQuery = new BoolQuery.Builder();

        boolean matchAll = true;

        if (queryStringFilter != null && !queryStringFilter.trim().isEmpty()) {
            finalQuery.must(qb -> qb.queryString(q -> q.query(queryStringFilter)));
            matchAll = false;
        }


        if (!layerFilter.isEmpty()) {
            finalQuery.must(queryBuilder -> queryBuilder
                    .terms(t -> t
                            .field("type")
                            .terms(tv -> tv
                                    .value(layerFilter
                                            .stream()
                                            .map(FieldValue::of)
                                            .toList()
                                    )
                            )
                    )
            );
            matchAll = false;
        }

        Query tagFilters = osmTagFilter.getTagFiltersQuery();
        if (tagFilters != null) {
            finalQuery.filter(tagFilters);
        }

        if (matchAll) {
            finalQuery.must(q -> q.matchAll(x -> x));
        }

        return finalQuery.filter(fb).build()._toQuery();
    }

    public ReverseQueryBuilder withOsmTagFilters(List<TagFilter> filters) {
        osmTagFilter.withOsmTagFilters(filters);
        return this;
    }
}
