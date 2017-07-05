package de.komoot.photon.query;



import static com.google.common.collect.Maps.newHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.lucene.search.function.FiltersFunctionScoreQuery.ScoreMode;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder.FilterFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.query.functionscore.ScriptScoreFunctionBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;

import com.google.common.collect.ImmutableSet;
import com.vividsolutions.jts.geom.Point;



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
 *
 * @see de.komoot.photon.query.TagFilterQueryBuilder
 */
public class PhotonQueryBuilder implements TagFilterQueryBuilder
{
    private FunctionScoreQueryBuilder queryBuilder;

    private Integer limit = 50;

    private BoolQueryBuilder queryBuilderForTopLevelFilter;

    private State state;

    // XXX old
    // private OrQueryBuilder orQueryBuilderForIncludeTagFiltering = null;
    //
    // private AndQueryBuilder andQueryBuilderForExcludeTagFiltering = null;

    private BoolQueryBuilder orQueryBuilderForIncludeTagFiltering = null;

    private BoolQueryBuilder andQueryBuilderForExcludeTagFiltering = null;



    private MatchQueryBuilder defaultMatchQueryBuilder;

    private MatchQueryBuilder languageMatchQueryBuilder;

    private QueryBuilder finalQueryBuilder;

    protected ArrayList<FilterFunctionBuilder> m_alFilterFunction4QueryBuilder = new ArrayList<>(1);

    protected QueryBuilder m_query4QueryBuilder;



    private PhotonQueryBuilder(String query, String language)
    {
        defaultMatchQueryBuilder =
                QueryBuilders.matchQuery("collector.default", query).fuzziness(Fuzziness.ONE).prefixLength(2).analyzer("search_ngram").minimumShouldMatch("100%");

        languageMatchQueryBuilder = QueryBuilders.matchQuery(String.format("collector.%s.ngrams", language), query).fuzziness(Fuzziness.ONE).prefixLength(2)
                .analyzer("search_ngram").minimumShouldMatch("100%");


        // XXX old:
//        queryBuilder = QueryBuilders.functionScoreQuery(
//                QueryBuilders.boolQuery().must(QueryBuilders.boolQuery().should(defaultMatchQueryBuilder).should(languageMatchQueryBuilder).minimumShouldMatch("1"))
//                        .should(QueryBuilders.matchQuery(String.format("name.%s.raw", language), query).boost(200).analyzer("search_raw"))
//                        .should(QueryBuilders.matchQuery(String.format("collector.%s.raw", language), query).boost(100).analyzer("search_raw")),
//                ScoreFunctionBuilders.scriptFunction("general-score", "groovy")).boostMode("multiply").scoreMode("multiply");


        m_query4QueryBuilder =
                QueryBuilders.boolQuery().must(QueryBuilders.boolQuery().should(defaultMatchQueryBuilder).should(languageMatchQueryBuilder).minimumShouldMatch("1"))
                        .should(QueryBuilders.matchQuery(String.format("name.%s.raw", language), query).boost(200).analyzer("search_raw"))
                        .should(QueryBuilders.matchQuery(String.format("collector.%s.raw", language), query).boost(100).analyzer("search_raw"));


        ScriptScoreFunctionBuilder functionBuilder4QueryBuilder =
                ScoreFunctionBuilders.scriptFunction(new Script(ScriptType.FILE, "painless", "general-score", null, new HashMap<String, Object>()));
        m_alFilterFunction4QueryBuilder.add(new FilterFunctionBuilder(functionBuilder4QueryBuilder));

        queryBuilder = new FunctionScoreQueryBuilder(m_query4QueryBuilder, m_alFilterFunction4QueryBuilder.toArray(new FilterFunctionBuilder[0]))
                .boostMode(CombineFunction.MULTIPLY).scoreMode(ScoreMode.MULTIPLY);

        
        
        // XXX old:
        // queryBuilderForTopLevelFilter = QueryBuilders.orQuery(QueryBuilders.missingQuery("housenumber"),
        // QueryBuilders.queryFilter(QueryBuilders.matchQuery("housenumber", query).analyzer("standard")),
        // QueryBuilders.existsQuery(String.format("name.%s.raw", language)));

        // deprecated stuff, see https://static.javadoc.io/org.elasticsearch/elasticsearch/2.3.0/deprecated-list.html
        queryBuilderForTopLevelFilter = QueryBuilders.boolQuery().should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("housenumber")))
                .should(QueryBuilders.matchQuery("housenumber", query).analyzer("standard")).should(QueryBuilders.existsQuery(String.format("name.%s.raw", language)));

        state = State.PLAIN;
    }



    /**
     * Create an instance of this builder which can then be embellished as needed.
     *
     * @param query the value for photon query parameter "q"
     * @param language
     * @return An initialized {@link TagFilterQueryBuilder photon query builder}.
     */
    public static TagFilterQueryBuilder builder(String query, String language)
    {
        return new PhotonQueryBuilder(query, language);
    }



    @Override
    public TagFilterQueryBuilder withLimit(Integer limit)
    {
        this.limit = limit == null || limit < 1 ? 15 : limit;
        this.limit = this.limit > 50 ? 50 : this.limit;
        return this;
    }



    @Override
    public TagFilterQueryBuilder withLocationBias(Point point)
    {
        if(point == null) return this;
        Map<String, Object> params = newHashMap();
        params.put("lon", point.getX());
        params.put("lat", point.getY());
        ScriptScoreFunctionBuilder builder =
                ScoreFunctionBuilders.scriptFunction(new Script(ScriptType.FILE, "painless", "location-biased-score", null, params));

        // old:
        // queryBuilder.add();

        // new - add method is removed. we have to create the object again
        m_alFilterFunction4QueryBuilder.add(new FilterFunctionBuilder(builder));

        queryBuilder = new FunctionScoreQueryBuilder(m_query4QueryBuilder, m_alFilterFunction4QueryBuilder.toArray(new FilterFunctionBuilder[0]))
                .boostMode(CombineFunction.MULTIPLY).scoreMode(ScoreMode.MULTIPLY);

        return this;
    }



    @Override
    public TagFilterQueryBuilder withTags(Map<String, Set<String>> tags)
    {
        if(!checkTags(tags)) return this;

        ensureFiltered();

        List<BoolQueryBuilder> termQueries = new ArrayList<BoolQueryBuilder>(tags.size());
        for (String tagKey : tags.keySet())
        {
            Set<String> valuesToInclude = tags.get(tagKey);
            TermQueryBuilder keyQuery = QueryBuilders.termQuery("osm_key", tagKey);
            TermsQueryBuilder valueQuery = QueryBuilders.termsQuery("osm_value", valuesToInclude.toArray(new String[valuesToInclude.size()]));
            BoolQueryBuilder includeAndQuery = QueryBuilders.boolQuery().must(keyQuery).must(valueQuery);
            termQueries.add(includeAndQuery);
        }
        this.appendIncludeTermQueries(termQueries);
        return this;
    }



    @Override
    public TagFilterQueryBuilder withKeys(Set<String> keys)
    {
        if(!checkTags(keys)) return this;

        ensureFiltered();

        List<TermsQueryBuilder> termQueries = new ArrayList<TermsQueryBuilder>(keys.size());
        termQueries.add(QueryBuilders.termsQuery("osm_key", keys.toArray()));
        this.appendIncludeTermQueries(termQueries);
        return this;
    }



    @Override
    public TagFilterQueryBuilder withValues(Set<String> values)
    {
        if(!checkTags(values)) return this;

        ensureFiltered();

        List<TermsQueryBuilder> termQueries = new ArrayList<TermsQueryBuilder>(values.size());
        termQueries.add(QueryBuilders.termsQuery("osm_value", values.toArray(new String[values.size()])));
        this.appendIncludeTermQueries(termQueries);
        return this;
    }



    @Override
    public TagFilterQueryBuilder withTagsNotValues(Map<String, Set<String>> tags)
    {
        if(!checkTags(tags)) return this;

        ensureFiltered();

        List<BoolQueryBuilder> termQueries = new ArrayList<BoolQueryBuilder>(tags.size());
        for (String tagKey : tags.keySet())
        {
            Set<String> valuesToInclude = tags.get(tagKey);
            TermQueryBuilder keyQuery = QueryBuilders.termQuery("osm_key", tagKey);
            TermsQueryBuilder valueQuery = QueryBuilders.termsQuery("osm_value", valuesToInclude.toArray(new String[valuesToInclude.size()]));
            // XXX old
            // NotQueryBuilder negatedValueQuery = QueryBuilders.notQuery(valueQuery);
            // AndQueryBuilder includeAndQuery = QueryBuilders.andQuery(keyQuery, negatedValueQuery);

            BoolQueryBuilder includeAndQuery = QueryBuilders.boolQuery().must(keyQuery).mustNot(valueQuery);

            termQueries.add(includeAndQuery);
        }
        this.appendIncludeTermQueries(termQueries);
        return this;
    }



    @Override
    public TagFilterQueryBuilder withoutTags(Map<String, Set<String>> tagsToExclude)
    {
        if(!checkTags(tagsToExclude)) return this;

        ensureFiltered();

        List<QueryBuilder> termQueries = new ArrayList<>(tagsToExclude.size());
        for (String tagKey : tagsToExclude.keySet())
        {
            Set<String> valuesToExclude = tagsToExclude.get(tagKey);
            TermQueryBuilder keyQuery = QueryBuilders.termQuery("osm_key", tagKey);
            TermsQueryBuilder valueQuery = QueryBuilders.termsQuery("osm_value", valuesToExclude.toArray(new String[valuesToExclude.size()]));
            // XXX old
            // AndQueryBuilder andQueryForExclusions = QueryBuilders.andQuery(keyQuery, valueQuery);
            // termQueries.add(QueryBuilders.notQuery(andQueryForExclusions));

            BoolQueryBuilder withoutTagsQuery = QueryBuilders.boolQuery().mustNot(QueryBuilders.boolQuery().must(keyQuery).must(valueQuery));

            termQueries.add(withoutTagsQuery);
        }

        this.appendExcludeTermQueries(termQueries);


        return this;
    }



    @Override
    public TagFilterQueryBuilder withoutKeys(Set<String> keysToExclude)
    {
        if(!checkTags(keysToExclude)) return this;

        ensureFiltered();


        // XXX old
        // List<BoolQueryBuilder> termQueries = new ArrayList<BoolQueryBuilder>(keysToExclude.size());
        // termQueries.add(QueryBuilders.notQuery(QueryBuilders.termsQuery("osm_key", keysToExclude.toArray())));

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery().mustNot(QueryBuilders.termsQuery("osm_key", keysToExclude.toArray()));

        LinkedList<QueryBuilder> lList = new LinkedList<>();
        lList.add(boolQuery);
        this.appendExcludeTermQueries(lList);


        return this;
    }



    @Override
    public TagFilterQueryBuilder withoutValues(Set<String> valuesToExclude)
    {
        if(!checkTags(valuesToExclude)) return this;

        ensureFiltered();

        // XXX old
        // List<BoolQueryBuilder> termQueries = new ArrayList<BoolQueryBuilder>(valuesToExclude.size());
        // termQueries.add(QueryBuilders.notQuery(QueryBuilders.termsQuery("osm_value", valuesToExclude.toArray())));

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery().mustNot(QueryBuilders.termsQuery("osm_value", valuesToExclude.toArray()));

        LinkedList<QueryBuilder> lList = new LinkedList<>();
        lList.add(boolQuery);
        this.appendExcludeTermQueries(lList);


        return this;
    }



    @Override
    public TagFilterQueryBuilder withKeys(String... keys)
    {
        return this.withKeys(ImmutableSet.<String>builder().add(keys).build());
    }



    @Override
    public TagFilterQueryBuilder withValues(String... values)
    {
        return this.withValues(ImmutableSet.<String>builder().add(values).build());
    }



    @Override
    public TagFilterQueryBuilder withoutKeys(String... keysToExclude)
    {
        return this.withoutKeys(ImmutableSet.<String>builder().add(keysToExclude).build());
    }



    @Override
    public TagFilterQueryBuilder withoutValues(String... valuesToExclude)
    {
        return this.withoutValues(ImmutableSet.<String>builder().add(valuesToExclude).build());
    }



    @Override
    public TagFilterQueryBuilder withStrictMatch()
    {
        defaultMatchQueryBuilder.minimumShouldMatch("100%");
        languageMatchQueryBuilder.minimumShouldMatch("100%");
        return this;
    }



    @Override
    public TagFilterQueryBuilder withLenientMatch()
    {
        defaultMatchQueryBuilder.minimumShouldMatch("-1");
        languageMatchQueryBuilder.minimumShouldMatch("-1");
        return this;
    }



    /**
     * When this method is called, all filters are placed inside their {@link OrQueryBuilder OR} or {@link AndQueryBuilder AND} containers and the top level filter
     * builder is built. Subsequent invocations of this method have no additional effect. Note that after this method is called, calling other methods on this class also
     * have no effect.
     *
     * @see TagFilterQueryBuilder#buildQuery()
     */
    @Override
    public QueryBuilder buildQuery()
    {
        if(state.equals(State.FINISHED)) return finalQueryBuilder;


        if(state.equals(State.FILTERED))
        {
            // XXX old
            // if(orQueryBuilderForIncludeTagFiltering != null) ((AndQueryBuilder) queryBuilderForTopLevelFilter).add(orQueryBuilderForIncludeTagFiltering);
            // if(andQueryBuilderForExcludeTagFiltering != null) ((AndQueryBuilder) queryBuilderForTopLevelFilter).add(andQueryBuilderForExcludeTagFiltering);

            if(orQueryBuilderForIncludeTagFiltering != null) queryBuilderForTopLevelFilter.must(orQueryBuilderForIncludeTagFiltering);
            if(andQueryBuilderForExcludeTagFiltering != null) queryBuilderForTopLevelFilter.must(andQueryBuilderForExcludeTagFiltering);

        }

        state = State.FINISHED;


        // XXX old
        // finalQueryBuilder = QueryBuilders.filteredQuery(queryBuilder, queryBuilderForTopLevelFilter);

        finalQueryBuilder = QueryBuilders.boolQuery().must(queryBuilder).filter(queryBuilderForTopLevelFilter);

        return finalQueryBuilder;
    }



    @Override
    public Integer getLimit()
    {
        return limit;
    }



    private Boolean checkTags(Set<String> keys)
    {
        return !(keys == null || keys.isEmpty());
    }



    private Boolean checkTags(Map<String, Set<String>> tags)
    {
        return !(tags == null || tags.isEmpty());
    }



    private void appendIncludeTermQueries(List<? extends QueryBuilder> termQueries)
    {


        // XXX old
        // if(orQueryBuilderForIncludeTagFiltering == null)
        // {
        // orQueryBuilderForIncludeTagFiltering = QueryBuilders.orQuery(termQueries.toArray(new QueryBuilder[termQueries.size()]));
        // }
        // else
        // {
        // for (QueryBuilder eachTagFilter : termQueries)
        // {
        // orQueryBuilderForIncludeTagFiltering.add(eachTagFilter);
        // }
        // }

        if(orQueryBuilderForIncludeTagFiltering == null) orQueryBuilderForIncludeTagFiltering = QueryBuilders.boolQuery();

        for (QueryBuilder eachTagFilter : termQueries)
            orQueryBuilderForIncludeTagFiltering.should(eachTagFilter);
    }



    private void appendExcludeTermQueries(List<QueryBuilder> termQueries)
    {

        // XXX old
        // if(andQueryBuilderForExcludeTagFiltering == null)
        // {
        // andQueryBuilderForExcludeTagFiltering = QueryBuilders.andQuery(termQueries.toArray(new QueryBuilder[termQueries.size()]));
        // }
        // else
        // {
        // for (QueryBuilder eachTagFilter : termQueries)
        // {
        // andQueryBuilderForExcludeTagFiltering.add(eachTagFilter);
        // }
        // }

        if(andQueryBuilderForExcludeTagFiltering == null) andQueryBuilderForExcludeTagFiltering = QueryBuilders.boolQuery();

        for (QueryBuilder eachTagFilter : termQueries)
            andQueryBuilderForExcludeTagFiltering.must(eachTagFilter);
    }



    private void ensureFiltered()
    {


        // XXX old: this method is not needed anymmore - now the toplevel queryBuilder is a BoolQueryBuilder in any case.
        // if(state.equals(State.PLAIN))
        // {
        // queryBuilderForTopLevelFilter = QueryBuilders.andQuery(queryBuilderForTopLevelFilter);
        // }
        // else if(queryBuilderForTopLevelFilter instanceof AndQueryBuilder)
        // {
        // // good! nothing to do because query builder is already filtered.
        // }
        // else
        // {
        // throw new RuntimeException(
        // "This code is not in valid state. It is expected that the filter builder field should either be AndQueryBuilder or OrQueryBuilder. Found" + " "
        // + queryBuilderForTopLevelFilter.getClass() + " instead.");
        // }
        state = State.FILTERED;
    }




    private enum State {
        PLAIN, FILTERED, QUERY_ALREADY_BUILT, FINISHED,
    }
}
