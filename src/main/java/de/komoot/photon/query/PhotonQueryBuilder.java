package de.komoot.photon.query;


import com.google.common.collect.ImmutableSet;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.lucene.search.function.FiltersFunctionScoreQuery.ScoreMode;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder.FilterFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.query.functionscore.ScriptScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.WeightBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;

import java.util.*;

import static com.google.common.collect.Maps.newHashMap;


/**
 * There are four {@link de.komoot.photon.query.PhotonQueryBuilder.State states} that this query builder goes through before a query can be executed on elastic search. Of
 * these, three are of importance.
 * <ul>
 * <li>{@link de.komoot.photon.query.PhotonQueryBuilder.State#PLAIN PLAIN} The query builder is being used to build a query without any tag filters.</li>
 * <li>{@link de.komoot.photon.query.PhotonQueryBuilder.State#FILTERED FILTERED} The query builder is being used to build a query that has tag filters and can no longer
 * be used to build a PLAIN filter.</li>
 * <li>{@link de.komoot.photon.query.PhotonQueryBuilder.State#FINISHED FINISHED} The query builder has been built and the query has been placed inside a
 * {@link QueryBuilder filtered query}. Further calls to any methods will have no effect on this query builder.</li>
 * </ul>
 * <p/>
 * Created by Sachin Dole on 2/12/2015.
 */
public class PhotonQueryBuilder {
    private FunctionScoreQueryBuilder finalQueryWithoutTagFilterBuilder;

    private BoolQueryBuilder queryBuilderForTopLevelFilter;

    private State state;

    private BoolQueryBuilder orQueryBuilderForIncludeTagFiltering = null;

    private BoolQueryBuilder andQueryBuilderForExcludeTagFiltering = null;

    private GeoBoundingBoxQueryBuilder bboxQueryBuilder;

    private BoolQueryBuilder finalQueryBuilder;

    protected ArrayList<FilterFunctionBuilder> alFilterFunction4QueryBuilder = new ArrayList<>(1);

    protected BoolQueryBuilder query4QueryBuilder;


    private PhotonQueryBuilder(String query, String language, List<String> languages, boolean lenient) {
        query4QueryBuilder = QueryBuilders.boolQuery();

        String[] cjkLanguages = { "ja" };

        // 1. All terms of the quey must be contained in the place record somehow. Be more lenient on second try.
        QueryBuilder collectorQuery;
        if (lenient) {
            BoolQueryBuilder builder = QueryBuilders.boolQuery()
                    .should(QueryBuilders.matchQuery("collector.default", query)
                            .fuzziness(Fuzziness.ONE)
                            .prefixLength(2)
                            .analyzer("search_ngram")
                            .minimumShouldMatch("-1"))
                    .should(QueryBuilders.matchQuery(String.format("collector.%s.ngrams", language), query)
                            .fuzziness(Fuzziness.ONE)
                            .prefixLength(2)
                            .analyzer("search_ngram")
                            .minimumShouldMatch("-1"))
                    .minimumShouldMatch("1");

            if (Arrays.asList(cjkLanguages).contains(language)) {
                builder = builder
                        .should(QueryBuilders.matchQuery(String.format("collector.default.raw_%s", language), query)
                                .fuzziness(Fuzziness.ONE)
                                .prefixLength(2)
                                .operator(Operator.AND)
                                .fuzzyTranspositions(false)
                                .minimumShouldMatch("-1"))
                        .should(QueryBuilders.matchQuery(String.format("collector.%s.raw", language), query)
                                .fuzziness(Fuzziness.ONE)
                                .prefixLength(2)
                                .operator(Operator.AND)
                                .fuzzyTranspositions(false)
                                .minimumShouldMatch("-1"));
            }

            builder = builder.minimumShouldMatch("1");

            collectorQuery = builder;
        } else {
            MultiMatchQueryBuilder builderDefault =
                    QueryBuilders.multiMatchQuery(query).field("collector.default", 1.0f).type(MultiMatchQueryBuilder.Type.CROSS_FIELDS).prefixLength(2).analyzer("search_ngram").minimumShouldMatch("100%");

            for (String lang : languages) {
                builderDefault.field(String.format("collector.%s.ngrams", lang), lang.equals(language) ? 1.0f : 0.6f);
            }

            BoolQueryBuilder builder = QueryBuilders.boolQuery().should(builderDefault);

            if (Arrays.asList(cjkLanguages).contains(language)) {
                MultiMatchQueryBuilder builderJapaneseField =
                        QueryBuilders.multiMatchQuery(query)
                                .field(String.format("collector.default.raw_%s",language), 1.0f)
                                .type(MultiMatchQueryBuilder.Type.PHRASE)
                                .prefixLength(2)
                                .operator(Operator.AND)
                                .analyzer(String.format("%s_search_raw", language))
                                .minimumShouldMatch("100%");

                builderJapaneseField.field(String.format("collector.%s.raw", language), 1.0f);

                builder = builder.should(builderJapaneseField);
            }

            collectorQuery = builder;
        }

        query4QueryBuilder.must(collectorQuery);

        // 2. Prefer records that have the full names in. For address records with housenumbers this is the main
        //    filter creterion because they have no name. Therefore boost the score in this case.
        MultiMatchQueryBuilder hnrQuery = QueryBuilders.multiMatchQuery(query)
                .field(Arrays.asList(cjkLanguages).contains(language) ? String.format("collector.default.raw_%s", language): "collector.default.raw", 1.0f)
                .type(MultiMatchQueryBuilder.Type.BEST_FIELDS);

        for (String lang : languages) {
            hnrQuery.field(String.format("collector.%s.raw", lang), lang.equals(language) ? 1.0f : 0.6f);
        }

        query4QueryBuilder.should(QueryBuilders.functionScoreQuery(hnrQuery.boost(0.3f), new FilterFunctionBuilder[]{
                new FilterFunctionBuilder(QueryBuilders.matchQuery("housenumber", query).analyzer("standard"), new WeightBuilder().setWeight(10f))
        }));

        // 3. Either the name or housenumber must be in the query terms.
        String defLang = "default".equals(language) ? languages.get(0) : language;
        MultiMatchQueryBuilder nameNgramQuery = QueryBuilders.multiMatchQuery(query)
                .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
                .fuzziness(lenient ? Fuzziness.ONE : Fuzziness.ZERO)
                .analyzer("search_ngram");

        for (String lang: languages) {
            nameNgramQuery.field(String.format("name.%s.ngrams", lang), lang.equals(defLang) ? 1.0f : 0.4f);
        }

        if (query.indexOf(',') < 0 && query.indexOf(' ') < 0) {
            query4QueryBuilder.must(nameNgramQuery.boost(2f));
        } else {
            query4QueryBuilder.must(QueryBuilders.boolQuery()
                    .should(nameNgramQuery)
                    .should(QueryBuilders.matchQuery("housenumber", query).analyzer("standard"))
                    .minimumShouldMatch("1"));
        }

        // 4. Rerank results for having the full name in the default language.
        query4QueryBuilder
                .should(QueryBuilders.matchQuery(String.format("name.%s.raw", language), query));


        // Weigh the resulting score by importance. Use a linear scale function that ensures that the weight
        // never drops to 0 and cancels out the ES score.
        finalQueryWithoutTagFilterBuilder = QueryBuilders.functionScoreQuery(query4QueryBuilder, new FilterFunctionBuilder[]{
                new FilterFunctionBuilder(ScoreFunctionBuilders.linearDecayFunction("importance", "1.0", "0.6"))
        });

        // Filter for later: records that have a housenumber and no name must only appear when the housenumber matches.
        queryBuilderForTopLevelFilter = QueryBuilders.boolQuery()
                .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("housenumber")))
                .should(QueryBuilders.matchQuery("housenumber", query).analyzer("standard"))
                .should(QueryBuilders.existsQuery(String.format("name.%s.raw", language)));

        state = State.PLAIN;
    }


    /**
     * Create an instance of this builder which can then be embellished as needed.
     *
     * @param query    the value for photon query parameter "q"
     * @param language
     * @return An initialized {@link PhotonQueryBuilder photon query builder}.
     */
    public static PhotonQueryBuilder builder(String query, String language, List<String> languages, boolean lenient) {
        return new PhotonQueryBuilder(query, language, languages, lenient);
    }

    public PhotonQueryBuilder withLocationBias(Point point, double scale) {
        if (point == null) return this;
        Map<String, Object> params = newHashMap();
        params.put("lon", point.getX());
        params.put("lat", point.getY());

        scale = Math.abs(scale);
        String strCode = "double dist = doc['coordinate'].planeDistance(params.lat, params.lon); " +
                "double score = 0.1 + " + scale + " / (1.0 + dist * 0.001 / 10.0); " +
                "score";
        ScriptScoreFunctionBuilder builder = ScoreFunctionBuilders.scriptFunction(new Script(ScriptType.INLINE, "painless", strCode, params));
        alFilterFunction4QueryBuilder.add(new FilterFunctionBuilder(builder));
        finalQueryWithoutTagFilterBuilder =
                new FunctionScoreQueryBuilder(query4QueryBuilder, alFilterFunction4QueryBuilder.toArray(new FilterFunctionBuilder[0]))
                        .boostMode(CombineFunction.MULTIPLY);
        return this;
    }
    
    public PhotonQueryBuilder withBoundingBox(Envelope bbox) {
        if (bbox == null) return this;
        bboxQueryBuilder = new GeoBoundingBoxQueryBuilder("coordinate");
        bboxQueryBuilder.setCorners(bbox.getMaxY(), bbox.getMinX(), bbox.getMinY(), bbox.getMaxX());
        
        return this;
    }

    public PhotonQueryBuilder withTags(Map<String, Set<String>> tags) {
        if (!checkTags(tags)) return this;

        ensureFiltered();

        List<BoolQueryBuilder> termQueries = new ArrayList<BoolQueryBuilder>(tags.size());
        for (String tagKey : tags.keySet()) {
            Set<String> valuesToInclude = tags.get(tagKey);
            TermQueryBuilder keyQuery = QueryBuilders.termQuery("osm_key", tagKey);
            TermsQueryBuilder valueQuery = QueryBuilders.termsQuery("osm_value", valuesToInclude.toArray(new String[valuesToInclude.size()]));
            BoolQueryBuilder includeAndQuery = QueryBuilders.boolQuery().must(keyQuery).must(valueQuery);
            termQueries.add(includeAndQuery);
        }
        this.appendIncludeTermQueries(termQueries);
        return this;
    }


    public PhotonQueryBuilder withKeys(Set<String> keys) {
        if (!checkTags(keys)) return this;

        ensureFiltered();

        List<TermsQueryBuilder> termQueries = new ArrayList<TermsQueryBuilder>(keys.size());
        termQueries.add(QueryBuilders.termsQuery("osm_key", keys.toArray()));
        this.appendIncludeTermQueries(termQueries);
        return this;
    }


    public PhotonQueryBuilder withValues(Set<String> values) {
        if (!checkTags(values)) return this;

        ensureFiltered();

        List<TermsQueryBuilder> termQueries = new ArrayList<TermsQueryBuilder>(values.size());
        termQueries.add(QueryBuilders.termsQuery("osm_value", values.toArray(new String[values.size()])));
        this.appendIncludeTermQueries(termQueries);
        return this;
    }


    public PhotonQueryBuilder withTagsNotValues(Map<String, Set<String>> tags) {
        if (!checkTags(tags)) return this;

        ensureFiltered();

        List<BoolQueryBuilder> termQueries = new ArrayList<BoolQueryBuilder>(tags.size());
        for (String tagKey : tags.keySet()) {
            Set<String> valuesToInclude = tags.get(tagKey);
            TermQueryBuilder keyQuery = QueryBuilders.termQuery("osm_key", tagKey);
            TermsQueryBuilder valueQuery = QueryBuilders.termsQuery("osm_value", valuesToInclude.toArray(new String[valuesToInclude.size()]));

            BoolQueryBuilder includeAndQuery = QueryBuilders.boolQuery().must(keyQuery).mustNot(valueQuery);

            termQueries.add(includeAndQuery);
        }
        this.appendIncludeTermQueries(termQueries);
        return this;
    }


    public PhotonQueryBuilder withoutTags(Map<String, Set<String>> tagsToExclude) {
        if (!checkTags(tagsToExclude)) return this;

        ensureFiltered();

        List<QueryBuilder> termQueries = new ArrayList<>(tagsToExclude.size());
        for (String tagKey : tagsToExclude.keySet()) {
            Set<String> valuesToExclude = tagsToExclude.get(tagKey);
            TermQueryBuilder keyQuery = QueryBuilders.termQuery("osm_key", tagKey);
            TermsQueryBuilder valueQuery = QueryBuilders.termsQuery("osm_value", valuesToExclude.toArray(new String[valuesToExclude.size()]));

            BoolQueryBuilder withoutTagsQuery = QueryBuilders.boolQuery().mustNot(QueryBuilders.boolQuery().must(keyQuery).must(valueQuery));

            termQueries.add(withoutTagsQuery);
        }

        this.appendExcludeTermQueries(termQueries);

        return this;
    }


    public PhotonQueryBuilder withoutKeys(Set<String> keysToExclude) {
        if (!checkTags(keysToExclude)) return this;

        ensureFiltered();

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery().mustNot(QueryBuilders.termsQuery("osm_key", keysToExclude.toArray()));

        LinkedList<QueryBuilder> lList = new LinkedList<>();
        lList.add(boolQuery);
        this.appendExcludeTermQueries(lList);

        return this;
    }


    public PhotonQueryBuilder withoutValues(Set<String> valuesToExclude) {
        if (!checkTags(valuesToExclude)) return this;

        ensureFiltered();

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery().mustNot(QueryBuilders.termsQuery("osm_value", valuesToExclude.toArray()));

        LinkedList<QueryBuilder> lList = new LinkedList<>();
        lList.add(boolQuery);
        this.appendExcludeTermQueries(lList);

        return this;
    }


    public PhotonQueryBuilder withKeys(String... keys) {
        return this.withKeys(ImmutableSet.<String>builder().add(keys).build());
    }


    public PhotonQueryBuilder withValues(String... values) {
        return this.withValues(ImmutableSet.<String>builder().add(values).build());
    }


    public PhotonQueryBuilder withoutKeys(String... keysToExclude) {
        return this.withoutKeys(ImmutableSet.<String>builder().add(keysToExclude).build());
    }


    public PhotonQueryBuilder withoutValues(String... valuesToExclude) {
        return this.withoutValues(ImmutableSet.<String>builder().add(valuesToExclude).build());
    }


    /**
     * When this method is called, all filters are placed inside their {@link OrQueryBuilder OR} or {@link AndQueryBuilder AND} containers and the top level filter
     * builder is built. Subsequent invocations of this method have no additional effect. Note that after this method is called, calling other methods on this class also
     * have no effect.
     */
    public QueryBuilder buildQuery() {
        if (state.equals(State.FINISHED)) return finalQueryBuilder;

        finalQueryBuilder = QueryBuilders.boolQuery().must(finalQueryWithoutTagFilterBuilder).filter(queryBuilderForTopLevelFilter);

        if (state.equals(State.FILTERED)) {
            BoolQueryBuilder tagFilters = QueryBuilders.boolQuery();
            if (orQueryBuilderForIncludeTagFiltering != null)
                tagFilters.must(orQueryBuilderForIncludeTagFiltering);
            if (andQueryBuilderForExcludeTagFiltering != null)
                tagFilters.must(andQueryBuilderForExcludeTagFiltering);
            finalQueryBuilder.filter(tagFilters);
        }
        
        if (bboxQueryBuilder != null) 
            queryBuilderForTopLevelFilter.filter(bboxQueryBuilder);

        state = State.FINISHED;

        return finalQueryBuilder;
    }


    private Boolean checkTags(Set<String> keys) {
        return !(keys == null || keys.isEmpty());
    }


    private Boolean checkTags(Map<String, Set<String>> tags) {
        return !(tags == null || tags.isEmpty());
    }


    private void appendIncludeTermQueries(List<? extends QueryBuilder> termQueries) {

        if (orQueryBuilderForIncludeTagFiltering == null)
            orQueryBuilderForIncludeTagFiltering = QueryBuilders.boolQuery();

        for (QueryBuilder eachTagFilter : termQueries)
            orQueryBuilderForIncludeTagFiltering.should(eachTagFilter);
    }


    private void appendExcludeTermQueries(List<QueryBuilder> termQueries) {

        if (andQueryBuilderForExcludeTagFiltering == null)
            andQueryBuilderForExcludeTagFiltering = QueryBuilders.boolQuery();

        for (QueryBuilder eachTagFilter : termQueries)
            andQueryBuilderForExcludeTagFiltering.must(eachTagFilter);
    }


    private void ensureFiltered() {
        state = State.FILTERED;
    }


    private enum State {
        PLAIN, FILTERED, QUERY_ALREADY_BUILT, FINISHED,
    }
}
