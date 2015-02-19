package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Point;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class PhotonQueryBuilder {
    private FilteredQueryBuilder filteredQueryBuilder;
    private FunctionScoreQueryBuilder functionScoreQueryBuilder;
    private Integer limit = 50;


    private PhotonQueryBuilder(String query) {
        functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(
                QueryBuilders.boolQuery().must(
                        QueryBuilders.boolQuery().should(
                                QueryBuilders.matchQuery("collector.default", query).fuzziness(Fuzziness.ONE).prefixLength(2).analyzer("search_ngram").minimumShouldMatch("100%")
                        ).should(
                                QueryBuilders.matchQuery("collector.en", query).fuzziness(Fuzziness.ONE).prefixLength(2).analyzer("search_ngram").minimumShouldMatch("100%")
                        ).minimumShouldMatch("1")
                ).should(
                        QueryBuilders.matchQuery("name.en.raw", query).boost(200).analyzer("search_raw")
                ).should(
                        QueryBuilders.matchQuery("collector.en.raw", query).boost(100).analyzer("search_raw")
                ),
                ScoreFunctionBuilders.scriptFunction("general-score", "mvel")
        ).boostMode("multiply").scoreMode("multiply");
        filteredQueryBuilder = QueryBuilders.filteredQuery(
                functionScoreQueryBuilder,
                FilterBuilders.orFilter(
                        FilterBuilders.missingFilter("housenumber"),
                        FilterBuilders.queryFilter(
                                QueryBuilders.matchQuery("housenumber", query).analyzer("standard")
                        ),
                        FilterBuilders.existsFilter("name.en.raw")
                )

        );
    }

    public static PhotonQueryBuilder builder(String query) {
        return new PhotonQueryBuilder(query);
    }

    public PhotonQueryBuilder withLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public PhotonQueryBuilder withLocation(Point point) {
        functionScoreQueryBuilder.add(ScoreFunctionBuilders.scriptFunction("location-biased-score", "mvel").param("lon", point.getX()).param("lat", point.getY()));
        return this;
    }

    public BytesReference build() {
        return filteredQueryBuilder.buildAsBytes();
    }

    public Integer getLimit() {
        return limit;
    }
}
