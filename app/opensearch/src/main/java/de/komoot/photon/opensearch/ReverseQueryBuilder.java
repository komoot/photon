package de.komoot.photon.opensearch;

import de.komoot.photon.searcher.TagFilter;
import org.locationtech.jts.geom.Point;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ReverseQueryBuilder {
    final private double radius;
    final private Point location;
    final private String queryStringFilter;
    final private Set<String> layerFilter;

    final private OsmTagFilter osmTagFilter = new OsmTagFilter();

    public ReverseQueryBuilder(Point location, double radius, String queryStringFilter, Set<String> layerFilter) {
        this.radius = radius;
        this.location = location;
        this.queryStringFilter = queryStringFilter != null && queryStringFilter.trim().length() > 0 ? queryStringFilter.trim() : null;
        this.layerFilter = layerFilter;
    }

   public Query buildQuery() {
        return BoolQuery.of(q -> {
            q.filter(fq -> fq
                    .geoDistance(gd -> gd
                            .field("coordinate")
                            .location(l -> l.latlon(ll -> ll.lat(location.getY()).lon(location.getX())))
                            .distance(radius + "km")));

            boolean hasQuery = false;

            if (queryStringFilter != null) {
                q.must(qst -> qst.queryString(qs -> qs.query(queryStringFilter)));
                hasQuery = true;
            }

            if (!layerFilter.isEmpty()) {
                q.must(ftq -> ftq.terms(tq -> {
                    List<FieldValue> terms = new ArrayList<>();
                    for (var filter : layerFilter) {
                        terms.add(FieldValue.of(filter));
                    }
                    return tq.field("type").terms(tt -> tt.value(terms));
                }));
                hasQuery = true;
            }

            if (!hasQuery) {
                q.must(mq -> mq.matchAll(ma -> ma));
            }

            final var tagFilters = osmTagFilter.build();
            if (tagFilters != null) {
                q.filter(tagFilters);
            }

            return q;
        }).toQuery();
    }

    public ReverseQueryBuilder withOsmTagFilters(List<TagFilter> filters) {
        osmTagFilter.withOsmTagFilters(filters);
        return this;
    }
}
