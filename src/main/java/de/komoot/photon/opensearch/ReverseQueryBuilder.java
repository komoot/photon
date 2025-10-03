package de.komoot.photon.opensearch;

import org.locationtech.jts.geom.Point;

public class ReverseQueryBuilder extends BaseQueryBuilder {

    public ReverseQueryBuilder(Point location, double radius) {
        outerQuery.filter(fq -> fq
                    .geoDistance(gd -> gd
                            .field("coordinate")
                            .location(l -> l.latlon(ll -> ll.lat(location.getY()).lon(location.getX())))
                            .distance(radius + "km")));
    }

    public void addQueryFilter(String query) {
        if (query != null && !query.isEmpty()) {
            outerQuery.must(qst -> qst.queryString(qs -> qs
                    .query(query)
            ));
        }
    }
}
