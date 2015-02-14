package de.komoot.photon;

import com.vividsolutions.jts.geom.Point;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.io.IOException;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class PhotonQueryBuilder {
    private final String query;
    private final Integer limit;
    private final Point locationForBias;
    private FilteredQueryBuilder filteredQueryBuilder;


    private PhotonQueryBuilder(String query, Integer limit, Point locationForBias) throws IOException {
        this.query = query;
        this.limit = limit;
        this.locationForBias = locationForBias;
        filteredQueryBuilder = QueryBuilders.filteredQuery(QueryBuilders.boolQuery(), FilterBuilders.andFilter());
        BytesReference bytes = filteredQueryBuilder.toXContent(JsonXContent.contentBuilder(), new ToXContent.MapParams(null)).bytes();
        String s = new String(bytes.toBytes(), "UTF-8");
    }

    public static PhotonQueryBuilder builder(String query, Integer limit, Point locationForBias) {

        try {
            return new PhotonQueryBuilder(query, limit, locationForBias);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public BytesReference build() {
        return filteredQueryBuilder.buildAsBytes();
    }
}
