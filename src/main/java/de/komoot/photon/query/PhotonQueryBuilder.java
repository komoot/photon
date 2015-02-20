package de.komoot.photon.query;

import com.google.common.collect.ImmutableSet;
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
public class PhotonQueryBuilder implements TagFilterQueryBuilder {
    private FunctionScoreQueryBuilder queryBuilder;
    private Integer limit = 50;
    private FilterBuilder filterBuilder;
    private State state;
    private OrFilterBuilder orFilterBuilderForIncludeTagFiltering = null;
    private AndFilterBuilder andFilterBuilderForExcludeTagFiltering = null;


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

    @Override
    public TagFilterQueryBuilder withLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public TagFilterQueryBuilder withLocationBias(Point point) {
        queryBuilder.add(ScoreFunctionBuilders.scriptFunction("location-biased-score", "mvel").param("lon", point.getX()).param("lat", point.getY()));
        return this;
    }

    @Override
    public TagFilterQueryBuilder withTags(Map<String, String> tags) {
        ensureFiltered();
        List<AndFilterBuilder> termFilters = new ArrayList<AndFilterBuilder>(tags.size());
        for (String tagKey : tags.keySet()) {
            String value = tags.get(tagKey);
            termFilters.add(FilterBuilders.andFilter(FilterBuilders.termFilter("osm_key", tagKey), FilterBuilders.termFilter("osm_value", value)));
        }
        this.appendIncludeTermFilters(termFilters);
        return this;
    }

    @Override
    public TagFilterQueryBuilder withKeys(Set<String> keys) {
        ensureFiltered();
        List<TermsFilterBuilder> termFilters = new ArrayList<TermsFilterBuilder>(keys.size());
        termFilters.add(FilterBuilders.termsFilter("osm_key", keys.toArray()));
        this.appendIncludeTermFilters(termFilters);
        return this;
    }

    @Override
    public TagFilterQueryBuilder withValues(Set<String> values) {
        ensureFiltered();
        List<TermsFilterBuilder> termFilters = new ArrayList<TermsFilterBuilder>(values.size());
        termFilters.add(FilterBuilders.termsFilter("osm_value", values.toArray(new String[values.size()])));
        this.appendIncludeTermFilters(termFilters);
        return this;
    }

    @Override
    public TagFilterQueryBuilder withoutTags(Map<String, String> tagsToExclude) {
        ensureFiltered();
        List<NotFilterBuilder> termFilters = new ArrayList<NotFilterBuilder>(tagsToExclude.size());
        for (String tagKey : tagsToExclude.keySet()) {
            String value = tagsToExclude.get(tagKey);
            termFilters.add(FilterBuilders.notFilter(FilterBuilders.andFilter(FilterBuilders.termFilter("osm_key", tagKey), FilterBuilders.termFilter("osm_value", value))));
        }
        this.appendExcludeTermFilters(termFilters);
        return this;
    }

    @Override
    public TagFilterQueryBuilder withoutKeys(Set<String> keysToExclude) {
        ensureFiltered();
        List<NotFilterBuilder> termFilters = new ArrayList<NotFilterBuilder>(keysToExclude.size());
        termFilters.add(FilterBuilders.notFilter(FilterBuilders.termsFilter("osm_key", keysToExclude.toArray())));
        this.appendExcludeTermFilters(termFilters);
        return this;
    }

    @Override
    public TagFilterQueryBuilder withoutValues(Set<String> valuesToExclude) {
        ensureFiltered();
        List<NotFilterBuilder> termFilters = new ArrayList<NotFilterBuilder>(valuesToExclude.size());
        termFilters.add(FilterBuilders.notFilter(FilterBuilders.termsFilter("osm_value", valuesToExclude.toArray())));
        this.appendExcludeTermFilters(termFilters);
        return this;
    }

    @Override
    public TagFilterQueryBuilder withKeys(String... keys) {
        return this.withKeys(ImmutableSet.<String>builder().add(keys).build());
    }

    @Override
    public TagFilterQueryBuilder withValues(String... values) {
        return this.withValues(ImmutableSet.<String>builder().add(values).build());
    }

    @Override
    public TagFilterQueryBuilder withoutKeys(String... keysToExclude) {
        return this.withoutKeys(ImmutableSet.<String>builder().add(keysToExclude).build());
    }

    @Override
    public TagFilterQueryBuilder withoutValues(String... valuesToExclude) {
        return this.withoutValues(ImmutableSet.<String>builder().add(valuesToExclude).build());
    }

    @Override
    public QueryBuilder buildQuery() {
        if (state.equals(State.FINISHED)) {
            throw new IllegalStateException("query building is already done.");
        }
        if (state.equals(State.FILTERED)) {
            if (orFilterBuilderForIncludeTagFiltering != null)
                ((AndFilterBuilder) filterBuilder).add(orFilterBuilderForIncludeTagFiltering);
            if (andFilterBuilderForExcludeTagFiltering != null)
                ((AndFilterBuilder) filterBuilder).add(andFilterBuilderForExcludeTagFiltering);
        }
        state = State.FINISHED;
        return QueryBuilders.filteredQuery(queryBuilder, filterBuilder);
    }

    @Override
    public Integer getLimit() {
        return limit;
    }

    private void appendIncludeTermFilters(List<? extends FilterBuilder> termFilters) {
        if (orFilterBuilderForIncludeTagFiltering == null) {
            orFilterBuilderForIncludeTagFiltering = FilterBuilders.orFilter(termFilters.toArray(new FilterBuilder[termFilters.size()]));
        } else {
            for (FilterBuilder eachTagFilter : termFilters) {
                orFilterBuilderForIncludeTagFiltering.add(eachTagFilter);
            }
        }
    }

    private void appendExcludeTermFilters(List<NotFilterBuilder> termFilters) {
        if (andFilterBuilderForExcludeTagFiltering == null) {
            andFilterBuilderForExcludeTagFiltering = FilterBuilders.andFilter(termFilters.toArray(new FilterBuilder[termFilters.size()]));
        } else {
            for (FilterBuilder eachTagFilter : termFilters) {
                andFilterBuilderForExcludeTagFiltering.add(eachTagFilter);
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
        state = State.FILTERED;
    }

    public String buildQueryJson() throws IOException {
        BytesReference bytes = this.buildQuery().toXContent(JsonXContent.contentBuilder(), new ToXContent.MapParams(null)).bytes();
        return new String(bytes.toBytes(), "UTF-8");
    }

    private enum State {
        PLAIN, FILTERED, QUERY_ALREADY_BUILT, FINISHED,
    }

}
