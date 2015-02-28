package de.komoot.photon.query;

import com.google.common.collect.ImmutableSet;
import com.vividsolutions.jts.geom.Point;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;

<<<<<<< HEAD
=======
import java.io.IOException;
>>>>>>> origin/master
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Sachin Dole on 2/12/2015.
 * @see de.komoot.photon.query.TagFilterQueryBuilder  
 */
public class PhotonQueryBuilder implements TagFilterQueryBuilder {
    private FunctionScoreQueryBuilder queryBuilder;
    private Integer limit = 50;
    private FilterBuilder filterBuilder;
    private State state;
    private OrFilterBuilder orFilterBuilderForIncludeTagFiltering = null;
    private AndFilterBuilder andFilterBuilderForExcludeTagFiltering = null;
    private MatchQueryBuilder defaultMatchQueryBuilder;
    private MatchQueryBuilder enMatchQueryBuilder;
    private FilteredQueryBuilder finalFilteredQueryBuiler;

    private PhotonQueryBuilder(String query) {
        defaultMatchQueryBuilder = QueryBuilders.matchQuery("collector.default", query).fuzziness(Fuzziness.ONE).prefixLength(2).analyzer("search_ngram").minimumShouldMatch("-1");
        enMatchQueryBuilder = QueryBuilders.matchQuery("collector.en", query).fuzziness(Fuzziness.ONE).prefixLength(2).analyzer("search_ngram").minimumShouldMatch("-1");
        queryBuilder = QueryBuilders.functionScoreQuery(
                QueryBuilders.boolQuery().must(
                        QueryBuilders.boolQuery().should(
                                defaultMatchQueryBuilder
                        ).should(
                                enMatchQueryBuilder
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

    /**
     * Create an instance of this builder which can then be embellished as needed.
     * @param query the value for photon query parameter "q"
     * @return An initialized {@link TagFilterQueryBuilder photon query builder}.
     */
    public static TagFilterQueryBuilder builder(String query) {
        return new PhotonQueryBuilder(query);
    }

    @Override
    public TagFilterQueryBuilder withLimit(Integer limit) {
        this.limit = limit == null || limit < 1 ? 15 : limit;
        this.limit = this.limit > 50 ? 50 : this.limit;
        return this;
    }

    @Override
    public TagFilterQueryBuilder withLocationBias(Point point) {
        if (point == null) return this;
        queryBuilder.add(ScoreFunctionBuilders.scriptFunction("location-biased-score", "mvel").param("lon", point.getX()).param("lat", point.getY()));
        return this;
    }

    @Override
    public TagFilterQueryBuilder withTags(Map<String, Set<String>> tags) {
        if (!checkTags(tags)) return this;
        ensureFiltered();
        List<AndFilterBuilder> termFilters = new ArrayList<AndFilterBuilder>(tags.size());
        for (String tagKey : tags.keySet()) {
            Set<String> valuesToInclude = tags.get(tagKey);
            TermFilterBuilder keyFilter = FilterBuilders.termFilter("osm_key", tagKey);
            TermsFilterBuilder valueFilter = FilterBuilders.termsFilter("osm_value", valuesToInclude.toArray(new String[valuesToInclude.size()]));
            AndFilterBuilder includeAndFilter = FilterBuilders.andFilter(keyFilter, valueFilter);
            termFilters.add(includeAndFilter);
        }
        this.appendIncludeTermFilters(termFilters);
        return this;
    }

    @Override
    public TagFilterQueryBuilder withKeys(Set<String> keys) {
        if (!checkTags(keys)) return this;
        ensureFiltered();
        List<TermsFilterBuilder> termFilters = new ArrayList<TermsFilterBuilder>(keys.size());
        termFilters.add(FilterBuilders.termsFilter("osm_key", keys.toArray()));
        this.appendIncludeTermFilters(termFilters);
        return this;
    }

    @Override
    public TagFilterQueryBuilder withValues(Set<String> values) {
        if (!checkTags(values)) return this;
        ensureFiltered();
        List<TermsFilterBuilder> termFilters = new ArrayList<TermsFilterBuilder>(values.size());
        termFilters.add(FilterBuilders.termsFilter("osm_value", values.toArray(new String[values.size()])));
        this.appendIncludeTermFilters(termFilters);
        return this;
    }

    @Override
    public TagFilterQueryBuilder withTagsNotValues(Map<String, Set<String>> tags) {
        if (!checkTags(tags)) return this;
        ensureFiltered();
        List<AndFilterBuilder> termFilters = new ArrayList<AndFilterBuilder>(tags.size());
        for (String tagKey : tags.keySet()) {
            Set<String> valuesToInclude = tags.get(tagKey);
            TermFilterBuilder keyFilter = FilterBuilders.termFilter("osm_key", tagKey);
            TermsFilterBuilder valueFilter = FilterBuilders.termsFilter("osm_value", valuesToInclude.toArray(new String[valuesToInclude.size()]));
            NotFilterBuilder negatedValueFilter = FilterBuilders.notFilter(valueFilter);
            AndFilterBuilder includeAndFilter = FilterBuilders.andFilter(keyFilter, negatedValueFilter);
            termFilters.add(includeAndFilter);
        }
        this.appendIncludeTermFilters(termFilters);
        return this;
    }

    @Override
    public TagFilterQueryBuilder withoutTags(Map<String, Set<String>> tagsToExclude) {
        if (!checkTags(tagsToExclude)) return this;
        ensureFiltered();
        List<NotFilterBuilder> termFilters = new ArrayList<NotFilterBuilder>(tagsToExclude.size());
        for (String tagKey : tagsToExclude.keySet()) {
            Set<String> valuesToExclude = tagsToExclude.get(tagKey);
            TermFilterBuilder keyFilter = FilterBuilders.termFilter("osm_key", tagKey);
            TermsFilterBuilder valueFilter = FilterBuilders.termsFilter("osm_value", valuesToExclude.toArray(new String[valuesToExclude.size()]));
            AndFilterBuilder andFilterForExclusions = FilterBuilders.andFilter(keyFilter, valueFilter);
            termFilters.add(FilterBuilders.notFilter(andFilterForExclusions));
        }
        this.appendExcludeTermFilters(termFilters);
        return this;
    }

    @Override
    public TagFilterQueryBuilder withoutKeys(Set<String> keysToExclude) {
        if (!checkTags(keysToExclude)) return this;
        ensureFiltered();
        List<NotFilterBuilder> termFilters = new ArrayList<NotFilterBuilder>(keysToExclude.size());
        termFilters.add(FilterBuilders.notFilter(FilterBuilders.termsFilter("osm_key", keysToExclude.toArray())));
        this.appendExcludeTermFilters(termFilters);
        return this;
    }

    @Override
    public TagFilterQueryBuilder withoutValues(Set<String> valuesToExclude) {
        if (!checkTags(valuesToExclude)) return this;
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
    public TagFilterQueryBuilder withStrictMatch() {
        defaultMatchQueryBuilder.minimumShouldMatch("100%");
        enMatchQueryBuilder.minimumShouldMatch("100%");
        return this;
    }

    @Override
    public TagFilterQueryBuilder withLenientMatch() {
        defaultMatchQueryBuilder.minimumShouldMatch("-1");
        enMatchQueryBuilder.minimumShouldMatch("-1");
        return this;
    }

    @Override
    public QueryBuilder buildQuery() {
        if (state.equals(State.FINISHED)) {
            return finalFilteredQueryBuiler;
        }
        if (state.equals(State.FILTERED)) {
            if (orFilterBuilderForIncludeTagFiltering != null)
                ((AndFilterBuilder) filterBuilder).add(orFilterBuilderForIncludeTagFiltering);
            if (andFilterBuilderForExcludeTagFiltering != null)
                ((AndFilterBuilder) filterBuilder).add(andFilterBuilderForExcludeTagFiltering);
        }
        state = State.FINISHED;
        finalFilteredQueryBuiler = QueryBuilders.filteredQuery(queryBuilder, filterBuilder);
        return finalFilteredQueryBuiler;
    }

    @Override
    public Integer getLimit() {
        return limit;
    }

    private Boolean checkTags(Set<String> keys) {
        return !(keys == null || keys.isEmpty());
    }

    private Boolean checkTags(Map<String, Set<String>> tags) {
        return !(tags == null || tags.isEmpty());
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
            //good! nothing to do because query builder is already filtered.
        } else {
            throw new RuntimeException("This code is not in valid state. It is expected that the filter builder field should either be AndFilterBuilder or OrFilterBuilder. Found" +
                                               " " + filterBuilder.getClass() + " instead.");
        }
        state = State.FILTERED;
    }

    private enum State {
        PLAIN, FILTERED, QUERY_ALREADY_BUILT, FINISHED,
    }

}
