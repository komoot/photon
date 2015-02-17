package de.komoot.photon;

import com.vividsolutions.jts.geom.Point;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class PhotonQueryBuilder {
    private final String query;
    private final Integer limit;
    private final Point locationForBias;
    private FilteredQueryBuilder filteredQueryBuilder;


    private PhotonQueryBuilder(String query, Integer limit, Point locationForBias) {
        this.query = query;
        this.limit = limit;
        this.locationForBias = locationForBias;
        FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(
                QueryBuilders.boolQuery().must(QueryBuilders.boolQuery().should().minimumShouldMatch()).should(QueryBuilders.), ScoreFunctionBuilders.scriptFunction
                        ("general-score", "mvel")
        ).boostMode("multiply").scoreMode("multiple");
        filteredQueryBuilder = QueryBuilders.filteredQuery(functionScoreQueryBuilder, FilterBuilders.andFilter());
    }

    public static PhotonQueryBuilder builder(String query, Integer limit, Point locationForBias) {
        return new PhotonQueryBuilder(query, limit, locationForBias);
    }


    public BytesReference build() {
        return filteredQueryBuilder.buildAsBytes();
    }
}
