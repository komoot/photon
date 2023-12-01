package de.komoot.photon.elasticsearch;


import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.GeoBoundingBoxQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.json.JsonData;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import de.komoot.photon.searcher.TagFilter;

import java.util.*;


/**
 * There are four {@link PhotonQueryBuilder.State states} that this query builder goes through before a query can be executed on elastic search. Of
 * these, three are of importance.
 * <ul>
 * <li>{@link PhotonQueryBuilder.State#PLAIN PLAIN} The query builder is being used to build a query without any tag filters.</li>
 * <li>{@link PhotonQueryBuilder.State#FILTERED FILTERED} The query builder is being used to build a query that has tag filters and can no longer
 * be used to build a PLAIN filter.</li>
 * <li>{@link PhotonQueryBuilder.State#FINISHED FINISHED} The query builder has been built and the query has been placed inside a
 * {@link BoolQuery.Builder filtered query}. Further calls to any methods will have no effect on this query builder.</li>
 * </ul>
 * <p/>
 * Created by Sachin Dole on 2/12/2015.
 */
public class PhotonQueryBuilder {
    private static final String[] ALT_NAMES = new String[]{"alt", "int", "loc", "old", "reg", "housename"};

    private FunctionScoreQuery.Builder finalQueryWithoutTagFilterBuilder;

    private BoolQuery.Builder queryBuilderForTopLevelFilter;

    private State state;

    private OsmTagFilter osmTagFilter;

    private GeoBoundingBoxQuery.Builder bboxQueryBuilder;

    private TermsQuery.Builder layerQueryBuilder;

    private BoolQuery.Builder finalQueryBuilder;



    private PhotonQueryBuilder(String query, String language, String[] languages, boolean lenient) {
        BoolQuery.Builder query4QueryBuilder = new BoolQuery.Builder();

        // 1. All terms of the query must be contained in the place record somehow. Be more lenient on second try.
        MultiMatchQuery.Builder builder = new MultiMatchQuery.Builder().query(query)
                .fields("collector.default^1.0")
                .type(lenient ? TextQueryType.BestFields : TextQueryType.CrossFields)
                .prefixLength(2)
                .analyzer("search_ngram")
                .tieBreaker(0.4)
                .minimumShouldMatch(lenient ? "-34%" : "100%");

        if (lenient) {
            builder.fuzziness("AUTO");
        }

        {
            List<String> languageFields = new ArrayList<>();
            for (String lang : languages) {
                languageFields.add(String.format("collector.%s.ngrams^%s", lang, lang.equals(language) ? "1.0" : "0.6"));
            }
            builder.fields(languageFields);
        }

        query4QueryBuilder.must(builder.build()._toQuery());

        // 2. Prefer records that have the full names in. For address records with housenumbers this is the main
        //    filter criterion because they have no name. Therefore, boost the score in this case.
        MultiMatchQuery.Builder hnrQueryBuilder = new MultiMatchQuery.Builder().query(query)
                .fields("collector.default.raw^1.0")
                .type(TextQueryType.BestFields);

        {
            List<String> languageFields = new ArrayList<>();
            for (String lang : languages) {
                languageFields.add(String.format("collector.%s.raw^%s", lang, lang.equals(language) ? "1.0" : "0.6"));
            }
            hnrQueryBuilder.fields(languageFields);
        }

        FunctionScoreQuery.Builder fnScoreQuery = new FunctionScoreQuery.Builder()
                .query(hnrQueryBuilder.build()._toQuery())
                .boost(0.3f)
                .functions(new FunctionScore.Builder()
                        .filter(q -> q
                                .match(v -> v
                                        .field("housenumber")
                                        .query(query)
                                        .analyzer("standard")
                                )
                        )
                        .weight(10d)
                        .build()
                );

        query4QueryBuilder.should(fnScoreQuery.build()._toQuery());


        // 3. Either the name or housenumber must be in the query terms.
        String defLang = "default".equals(language) ? languages[0] : language;
        MultiMatchQuery.Builder nameNgramQueryBuilder = new MultiMatchQuery.Builder()
                .query(query)
                .type(TextQueryType.BestFields)
                .fuzziness(lenient ? String.valueOf(1) : String.valueOf(0))
                .analyzer("search_ngram");

        {
            List<String> languageFields = new ArrayList<>();
            for (String lang : languages) {
                languageFields.add(String.format("name.%s.ngrams^%s", lang, lang.equals(defLang) ? "1.0" : "0.4"));
            }
            for (String alt : ALT_NAMES) {
                languageFields.add(String.format("name.%s.raw^%s", alt, "0.4"));
            }
            nameNgramQueryBuilder.fields(languageFields);
        }

        if (query.indexOf(',') < 0 && query.indexOf(' ') < 0) {
            query4QueryBuilder.must(nameNgramQueryBuilder.boost(2f).build()._toQuery());
        } else {
            query4QueryBuilder.must(new BoolQuery.Builder()
                    .should(nameNgramQueryBuilder.build()._toQuery())
                    .should(q -> q.match(v -> v.field("housenumber").query(query).analyzer("standard")))
                    .should(q -> q.match(v -> v.field("classification").query(query).boost(0.1f)))
                    .minimumShouldMatch("1")
                    .build()
                    ._toQuery()
            );

        }

        // 4. Re-rank results for having the full name in the default language.
        query4QueryBuilder.should(new MatchQuery.Builder()
                .field(String.format("name.%s.raw", language))
                .query(query)
                .fuzziness(lenient ? "AUTO" : String.valueOf(0))
                .build()
                ._toQuery()
        );

        // Weigh the resulting score by importance. Use a linear scale function that ensures that the weight
        // never drops to 0 and cancels out the ES score.
        finalQueryWithoutTagFilterBuilder = new FunctionScoreQuery.Builder()
                .query(query4QueryBuilder.build()._toQuery())
                .functions(
                        new FunctionScore.Builder()
                                .linear(fn -> fn
                                        .field("importance")
                                        .placement(p -> p
                                                .origin(JsonData.of("1.0"))
                                                .scale(JsonData.of("0.6"))
                                        )
                                )
                                .build(),
                        new FunctionScore.Builder()
                                .filter(new MatchQuery.Builder()
                                        .field("classification")
                                        .query(query)
                                        .build()
                                        ._toQuery()
                                )
                                .weight(0.1d)
                                .build()
                )
                .scoreMode(FunctionScoreMode.Sum);

        // Filter for later: records that have a housenumber and no name must only appear when the housenumber matches.
        queryBuilderForTopLevelFilter = new BoolQuery.Builder()
                .should(a -> a.bool(b -> b.mustNot(c -> c.exists(d -> d.field("housenumber")))))
                .should(a -> a.match(b -> b.field("housenumber").query(query).analyzer("standard")))
                .should(a -> a.exists(b -> b.field(String.format("name.%s.raw", language))));

        osmTagFilter = new OsmTagFilter();

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

        double finalScale = scale;
        finalQueryWithoutTagFilterBuilder = new FunctionScoreQuery.Builder()
                .query(finalQueryWithoutTagFilterBuilder.build()._toQuery())
                .functions(
                    new FunctionScore.Builder()
                            .exp(fn -> fn
                                    .field("coordinate")
                                    .placement(p -> p
                                            .origin(JsonData.of(String.format("%s, %s", point.getX(), point.getY())))
                                            .scale(JsonData.of(String.format("%skm", radius)))
                                            .offset(JsonData.of(String.format("%skm", radius / 10)))
                                            .decay(0.8)
                                    )
                            )
                            .build(),
                    new FunctionScore.Builder()
                            .linear(fn -> fn
                                    .field("importance")
                                    .placement(p -> p
                                            .origin(JsonData.of("1.0"))
                                            .scale(JsonData.of(finalScale))
                                    )
                            ).build()
                )
                .boostMode(FunctionBoostMode.Multiply)
                .scoreMode(FunctionScoreMode.Max);

        return this;
    }
    
    public PhotonQueryBuilder withBoundingBox(Envelope bbox) {
        if (bbox == null) return this;
        bboxQueryBuilder = new GeoBoundingBoxQuery.Builder()
                .field("coordinate")
                .boundingBox(bb -> bb
                        .coords(c -> c
                                .top(bbox.getMaxY())
                                .bottom(bbox.getMinY())
                                .right(bbox.getMaxX())
                                .left(bbox.getMinX())
                        )
                );

        return this;
    }

    public PhotonQueryBuilder withOsmTagFilters(List<TagFilter> filters) {
        state = State.FILTERED;
        osmTagFilter.withOsmTagFilters(filters);
        return this;
    }

    public PhotonQueryBuilder withLayerFilters(Set<String> filters) {
        if (!filters.isEmpty()) {
            layerQueryBuilder = new TermsQuery.Builder()
                    .field("type")
                    .terms(tv -> tv
                            .value(filters
                                    .stream()
                                    .map(FieldValue::of)
                                    .toList()
                            )
                    );
        }

        return this;
    }


    /**
     * When this method is called, all filters are placed inside their containers and the top level filter
     * builder is built. Subsequent invocations of this method have no additional effect. Note that after this method
     * is called, calling other methods on this class also have no effect.
     */
    public Query buildQuery() {
        if (state.equals(State.FINISHED)) return finalQueryBuilder.build()._toQuery();

        finalQueryBuilder = new BoolQuery.Builder().must(finalQueryWithoutTagFilterBuilder.build()._toQuery());

        Query tagFilters = osmTagFilter.getTagFiltersQuery();

        if (state.equals(State.FILTERED) && tagFilters != null) {
            finalQueryBuilder.filter(tagFilters);
        }
        
        if (bboxQueryBuilder != null) 
            queryBuilderForTopLevelFilter.filter(bboxQueryBuilder.build()._toQuery());

        if (layerQueryBuilder != null)
            queryBuilderForTopLevelFilter.filter(layerQueryBuilder.build()._toQuery());

        finalQueryBuilder.filter(queryBuilderForTopLevelFilter.build()._toQuery());
        state = State.FINISHED;

        return finalQueryBuilder.build()._toQuery();
    }

    private enum State {
        PLAIN, FILTERED, QUERY_ALREADY_BUILT, FINISHED,
    }
}
