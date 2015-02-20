package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Point;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Sachin Dole on 2/12/2015.
 */
public class PhotonQueryBuilder {
    private FunctionScoreQueryBuilder queryBuilder;
    private Integer limit = 50;
    private FilterBuilder filterBuilder;
    private State state;
    private OrFilterBuilder orFilterBuilderForTagFiltering = null;


    private PhotonQueryBuilder(String query) {
        queryBuilder = QueryBuilders.functionScoreQuery(
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
        filterBuilder = FilterBuilders.orFilter(
                FilterBuilders.missingFilter("housenumber"),
                FilterBuilders.queryFilter(
                        QueryBuilders.matchQuery("housenumber", query).analyzer("standard")
                ),
                FilterBuilders.existsFilter("name.en.raw")
        );
        state = State.PLAIN;
    }

    public static PhotonQueryBuilder builder(String query) {
        return new PhotonQueryBuilder(query);
    }

    public PhotonQueryBuilder withLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public PhotonQueryBuilder withLocation(Point point) {
        queryBuilder.add(ScoreFunctionBuilders.scriptFunction("location-biased-score", "mvel").param("lon", point.getX()).param("lat", point.getY()));
        return this;
    }

    public PhotonQueryBuilder withTags(Map<String, String> tags) {
        ensureFiltered();
        List<AndFilterBuilder> termFilters = new ArrayList<AndFilterBuilder>(tags.size());
        for (String tagKey : tags.keySet()) {
            String value = tags.get(tagKey);
            termFilters.add(FilterBuilders.andFilter(FilterBuilders.termFilter("osm_key", tagKey), FilterBuilders.termFilter("osm_value", value)));
        }
        this.appendTermFilters(termFilters);
        return this;
    }

    public PhotonQueryBuilder withValues(Set<String> values) {
        ensureFiltered();
        List<TermsFilterBuilder> termFilters = new ArrayList<TermsFilterBuilder>(values.size());
        termFilters.add(FilterBuilders.termsFilter("osm_value", values.toArray(new String[values.size()])));
        this.appendTermFilters(termFilters);
        return this;
    }

    public PhotonQueryBuilder withKeys(Set<String> keys) {
        ensureFiltered();
        List<TermsFilterBuilder> termFilters = new ArrayList<TermsFilterBuilder>(keys.size());
        termFilters.add(FilterBuilders.termsFilter("osm_key", keys.toArray()));
        this.appendTermFilters(termFilters);
        return this;
    }

    private void appendTermFilters(List<? extends FilterBuilder> termFilters) {
        if (orFilterBuilderForTagFiltering == null) {
            orFilterBuilderForTagFiltering = FilterBuilders.orFilter(termFilters.toArray(new FilterBuilder[termFilters.size()]));
        } else {
            for (FilterBuilder eachTagFilter : termFilters) {
                orFilterBuilderForTagFiltering.add(eachTagFilter);
            }
        }
    }

    private void ensureFiltered() {
        if (state.equals(State.PLAIN)) {
            filterBuilder = FilterBuilders.andFilter(filterBuilder);
        } else if (filterBuilder instanceof AndFilterBuilder) {
            //good! nothing to do
        } else {
            throw new RuntimeException("This code is not in valid state. It is expected that the filter builder field should either be AndFilterBuilder or OrFilterBuilder. Found" +
                                               " " + filterBuilder.getClass() + " instead.");
        }
        state = State.TAG_FILTERED;
    }

    public PhotonQueryBuilder withoutTags(Map<String, String> tagsToExclude) {return this;}

    public PhotonQueryBuilder withoutKeys(Set<String> keysToExclude) {return this;}

    public PhotonQueryBuilder withoutValues(Set<String> valuesToExclude) {return this;}

    public QueryBuilder buildQuery() {
        if (state.equals(State.FINISHED)) {
            throw new IllegalStateException("query building is already done.");
        }
        if (state.equals(State.TAG_FILTERED))
            ((AndFilterBuilder) filterBuilder).add(orFilterBuilderForTagFiltering);
        state = State.FINISHED;
        return QueryBuilders.filteredQuery(queryBuilder, filterBuilder);
    }

    public String buildQueryJson() throws IOException {
        BytesReference bytes = this.buildQuery().toXContent(JsonXContent.contentBuilder(), new ToXContent.MapParams(null)).bytes();
        return new String(bytes.toBytes(), "UTF-8");
    }

    public Integer getLimit() {
        return limit;
    }

    private enum State {
        PLAIN, ALREADY_BUILT, FINISHED, TAG_FILTERED
    }

}
