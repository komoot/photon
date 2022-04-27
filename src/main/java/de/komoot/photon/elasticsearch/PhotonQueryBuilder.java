package de.komoot.photon.elasticsearch;


import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import de.komoot.photon.searcher.TagFilter;
import de.komoot.photon.searcher.TagFilterKind;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.lucene.search.function.FiltersFunctionScoreQuery.ScoreMode;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder.FilterFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.query.functionscore.WeightBuilder;

import java.util.*;


/**
 * There are four {@link PhotonQueryBuilder.State states} that this query builder goes through before a query can be executed on elastic search. Of
 * these, three are of importance.
 * <ul>
 * <li>{@link PhotonQueryBuilder.State#PLAIN PLAIN} The query builder is being used to build a query without any tag filters.</li>
 * <li>{@link PhotonQueryBuilder.State#FILTERED FILTERED} The query builder is being used to build a query that has tag filters and can no longer
 * be used to build a PLAIN filter.</li>
 * <li>{@link PhotonQueryBuilder.State#FINISHED FINISHED} The query builder has been built and the query has been placed inside a
 * {@link QueryBuilder filtered query}. Further calls to any methods will have no effect on this query builder.</li>
 * </ul>
 * <p/>
 * Created by Sachin Dole on 2/12/2015.
 */
public class PhotonQueryBuilder {
    private static final String[] ALT_NAMES = new String[]{"alt", "int", "loc", "old", "reg", "housename"};

    private FunctionScoreQueryBuilder finalQueryWithoutTagFilterBuilder;

    private BoolQueryBuilder queryBuilderForTopLevelFilter;

    private State state;

    private BoolQueryBuilder orQueryBuilderForIncludeTagFiltering = null;

    private BoolQueryBuilder andQueryBuilderForExcludeTagFiltering = null;

    private GeoBoundingBoxQueryBuilder bboxQueryBuilder;

    private TermsQueryBuilder layerQueryBuilder;

    private BoolQueryBuilder finalQueryBuilder;

    protected ArrayList<FilterFunctionBuilder> alFilterFunction4QueryBuilder = new ArrayList<>(1);


    private PhotonQueryBuilder(String query, String language, String[] languages, boolean lenient) {
        BoolQueryBuilder query4QueryBuilder = QueryBuilders.boolQuery();

        // 1. All terms of the quey must be contained in the place record somehow. Be more lenient on second try.
        QueryBuilder collectorQuery;
        if (lenient) {
            collectorQuery = QueryBuilders.boolQuery()
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
        } else {
            MultiMatchQueryBuilder builder =
                    QueryBuilders.multiMatchQuery(query).field("collector.default", 1.0f).type(MultiMatchQueryBuilder.Type.CROSS_FIELDS).prefixLength(2).analyzer("search_ngram").minimumShouldMatch("100%");

            for (String lang : languages) {
                builder.field(String.format("collector.%s.ngrams", lang), lang.equals(language) ? 1.0f : 0.6f);
            }

            collectorQuery = builder;
        }

        query4QueryBuilder.must(collectorQuery);

        // 2. Prefer records that have the full names in. For address records with housenumbers this is the main
        //    filter creterion because they have no name. Therefore boost the score in this case.
        MultiMatchQueryBuilder hnrQuery = QueryBuilders.multiMatchQuery(query)
                .field("collector.default.raw", 1.0f)
                .type(MultiMatchQueryBuilder.Type.BEST_FIELDS);

        for (String lang : languages) {
            hnrQuery.field(String.format("collector.%s.raw", lang), lang.equals(language) ? 1.0f : 0.6f);
        }

        query4QueryBuilder.should(QueryBuilders.functionScoreQuery(hnrQuery.boost(0.3f), new FilterFunctionBuilder[]{
                new FilterFunctionBuilder(QueryBuilders.matchQuery("housenumber", query).analyzer("standard"), new WeightBuilder().setWeight(10f))
        }));

        // 3. Either the name or housenumber must be in the query terms.
        String defLang = "default".equals(language) ? languages[0] : language;
        MultiMatchQueryBuilder nameNgramQuery = QueryBuilders.multiMatchQuery(query)
                .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
                .fuzziness(lenient ? Fuzziness.ONE : Fuzziness.ZERO)
                .analyzer("search_ngram");

        for (String lang: languages) {
            nameNgramQuery.field(String.format("name.%s.ngrams", lang), lang.equals(defLang) ? 1.0f : 0.4f);
        }

        for (String alt: ALT_NAMES) {
            nameNgramQuery.field(String.format("name.%s.raw", alt), 0.4f);
        }

        if (query.indexOf(',') < 0 && query.indexOf(' ') < 0) {
            query4QueryBuilder.must(nameNgramQuery.boost(2f));
        } else {
            query4QueryBuilder.must(QueryBuilders.boolQuery()
                                        .should(nameNgramQuery)
                                        .should(QueryBuilders.matchQuery("housenumber", query).analyzer("standard"))
                                        .should(QueryBuilders.matchQuery("classification", query).boost(0.1f))
                                        .minimumShouldMatch("1"));
        }

        // 4. Rerank results for having the full name in the default language.
        query4QueryBuilder
                .should(QueryBuilders.matchQuery(String.format("name.%s.raw", language), query));


        // Weigh the resulting score by importance. Use a linear scale function that ensures that the weight
        // never drops to 0 and cancels out the ES score.
        finalQueryWithoutTagFilterBuilder = QueryBuilders.functionScoreQuery(query4QueryBuilder, new FilterFunctionBuilder[]{
                new FilterFunctionBuilder(ScoreFunctionBuilders.linearDecayFunction("importance", "1.0", "0.6")),
                new FilterFunctionBuilder(QueryBuilders.matchQuery("classification", query), ScoreFunctionBuilders.weightFactorFunction(0.1f))
        }).scoreMode(ScoreMode.SUM);

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
    public static PhotonQueryBuilder builder(String query, String language, String[] languages, boolean lenient) {
        return new PhotonQueryBuilder(query, language, languages, lenient);
    }

    public PhotonQueryBuilder withLocationBias(Point point, double scale, int zoom) {
        if (point == null || zoom < 4) return this;

        if (zoom > 18) {
            zoom = 18;
        }
        double radius = (1 << (18 - zoom)) * 0.25;

        if (scale <= 0.0) {
            scale = 0.0000001;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("lon", point.getX());
        params.put("lat", point.getY());

        finalQueryWithoutTagFilterBuilder =
                QueryBuilders.functionScoreQuery(finalQueryWithoutTagFilterBuilder, new FilterFunctionBuilder[] {
                     new FilterFunctionBuilder(ScoreFunctionBuilders.exponentialDecayFunction("coordinate", params, radius + "km", radius / 10 + "km", 0.8)),
                     new FilterFunctionBuilder(ScoreFunctionBuilders.linearDecayFunction("importance", "1.0", scale))
                }).boostMode(CombineFunction.MULTIPLY).scoreMode(ScoreMode.MAX);
        return this;
    }
    
    public PhotonQueryBuilder withBoundingBox(Envelope bbox) {
        if (bbox == null) return this;
        bboxQueryBuilder = new GeoBoundingBoxQueryBuilder("coordinate");
        bboxQueryBuilder.setCorners(bbox.getMaxY(), bbox.getMinX(), bbox.getMinY(), bbox.getMaxX());
        
        return this;
    }

    public PhotonQueryBuilder withOsmTagFilters(List<TagFilter> filters) {
        for (TagFilter filter : filters) {
            addOsmTagFilter(filter);
        }
        return this;
    }

    public PhotonQueryBuilder addOsmTagFilter(TagFilter filter) {
        state = State.FILTERED;

        if (filter.getKind() == TagFilterKind.EXCLUDE_VALUE) {
            appendIncludeTermQuery(QueryBuilders.boolQuery()
                    .must(QueryBuilders.termQuery("osm_key", filter.getKey()))
                    .mustNot(QueryBuilders.termQuery("osm_value", filter.getValue())));
        } else {
            QueryBuilder builder;
            if (filter.isKeyOnly()) {
                builder = QueryBuilders.termQuery("osm_key", filter.getKey());
            } else if (filter.isValueOnly()) {
                builder = QueryBuilders.termQuery("osm_value", filter.getValue());
            } else {
                builder = QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery("osm_key", filter.getKey()))
                        .must(QueryBuilders.termQuery("osm_value", filter.getValue()));
            }
            if (filter.getKind() == TagFilterKind.INCLUDE) {
                appendIncludeTermQuery(builder);
            } else {
                appendExcludeTermQuery(builder);
            }
        }
        return this;
    }

    public PhotonQueryBuilder withLayerFilters(Set<String> filters) {
        if (filters.size() > 0) {
            layerQueryBuilder = new TermsQueryBuilder("type", filters);
        }

        return this;
    }



    /**
     * When this method is called, all filters are placed inside their containers and the top level filter
     * builder is built. Subsequent invocations of this method have no additional effect. Note that after this method
     * is called, calling other methods on this class also have no effect.
     */
    public QueryBuilder buildQuery() {
        if (state.equals(State.FINISHED)) return finalQueryBuilder;

        finalQueryBuilder = QueryBuilders.boolQuery().must(finalQueryWithoutTagFilterBuilder).filter(queryBuilderForTopLevelFilter);

        if (state.equals(State.FILTERED)) {
            BoolQueryBuilder tagFilters = QueryBuilders.boolQuery();
            if (orQueryBuilderForIncludeTagFiltering != null)
                tagFilters.must(orQueryBuilderForIncludeTagFiltering);
            if (andQueryBuilderForExcludeTagFiltering != null)
                tagFilters.mustNot(andQueryBuilderForExcludeTagFiltering);
            finalQueryBuilder.filter(tagFilters);
        }
        
        if (bboxQueryBuilder != null) 
            queryBuilderForTopLevelFilter.filter(bboxQueryBuilder);

        if (layerQueryBuilder != null)
            queryBuilderForTopLevelFilter.filter(layerQueryBuilder);

        state = State.FINISHED;

        return finalQueryBuilder;
    }


    private void appendIncludeTermQuery(QueryBuilder termQuery) {

        if (orQueryBuilderForIncludeTagFiltering == null)
            orQueryBuilderForIncludeTagFiltering = QueryBuilders.boolQuery();

        orQueryBuilderForIncludeTagFiltering.should(termQuery);
    }


    private void appendExcludeTermQuery(QueryBuilder termQuery) {

        if (andQueryBuilderForExcludeTagFiltering == null)
            andQueryBuilderForExcludeTagFiltering = QueryBuilders.boolQuery();

        andQueryBuilderForExcludeTagFiltering.should(termQuery);
    }


    private enum State {
        PLAIN, FILTERED, QUERY_ALREADY_BUILT, FINISHED,
    }
}
