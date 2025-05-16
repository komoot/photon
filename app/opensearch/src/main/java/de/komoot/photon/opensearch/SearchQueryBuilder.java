package de.komoot.photon.opensearch;

import de.komoot.photon.searcher.TagFilter;
import de.komoot.photon.query.StructuredSearchRequest;
import de.komoot.photon.Constants;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.*;
import org.opensearch.client.util.ObjectBuilder;

import java.util.*;

public class SearchQueryBuilder {
    private ObjectBuilder<Query> finalQueryWithoutTagFilterBuilder;
    private BoolQuery.Builder queryBuilderForTopLevelFilter;
    private OsmTagFilter osmTagFilter = new OsmTagFilter();
    private GeoBoundingBoxQuery.Builder bboxQueryBuilder;
    private TermsQuery.Builder layerQueryBuilder;
    private Query finalQuery = null;

    public SearchQueryBuilder(String query, String language, String[] languages, boolean lenient) {
        var query4QueryBuilder = QueryBuilders.bool();

        // 1. All terms of the query must be contained in the place record somehow. Be more lenient on second try.
        query4QueryBuilder.must(base -> base.match(q -> {
            q.query(fn -> fn.stringValue(query));
            q.analyzer("search");
            q.field("collector.base");

            if (lenient) {
                q.fuzziness("AUTO");
                q.prefixLength(2);
                q.minimumShouldMatch("-34%");
            } else {
                q.operator(Operator.And);
            }
            return q;
        }));

        // 2. Prefer records that have the full names in. For address records with house numbers this is the main
        //    filter criterion because they have no name. Boost the score in this case.
        query4QueryBuilder.should(shd -> shd.functionScore(fs -> fs
                .query(q -> q.multiMatch(mm -> {
                    mm.query(query).type(TextQueryType.BestFields).analyzer("search");
                    mm.fields(String.format(Locale.ROOT, "%s^%f", "collector.default", 1.0f));

                    for (String lang : languages) {
                        mm.fields(String.format(Locale.ROOT, "collector.%s^%f", lang, lang.equals(language) ? 1.0f : 0.6f));
                    }

                    return mm.boost(0.3f);
                }))
                .functions(fn -> fn
                        .filter(flt -> flt
                                .match(m -> m
                                        .query(q -> q.stringValue(query))
                                        .field("housenumber")))
                        .weight(10.0)
                )
        ));

        // 3. Either the name or house number must be in the query terms.
        final String defLang = "default".equals(language) ? languages[0] : language;
        var nameNgramQuery = MultiMatchQuery.of(q -> {
            q.query(query).type(TextQueryType.BestFields).analyzer("search");

            if (lenient) {
                q.fuzziness("AUTO").prefixLength(2);
            }

            for (String lang : languages) {
                q.fields(String.format(Locale.ROOT, "name.%s.ngrams^%f", lang, lang.equals(defLang) ? 1.0f : 0.4f));
            }

            q.fields("name.other^0.4");

            if (query.indexOf(',') < 0 && query.indexOf(' ') < 0) {
                q.boost(2f);
            }

            return q;
        });

        if (query.indexOf(',') < 0 && query.indexOf(' ') < 0) {
            query4QueryBuilder.must(nameNgramQuery.toQuery());
        } else {
            query4QueryBuilder.must(m -> m.bool(q -> q
                    .should(nameNgramQuery.toQuery())
                    .should(shd1 -> shd1
                            .match(m1 -> m1
                                    .query(q1 -> q1.stringValue(query))
                                    .field("housenumber")
                                    .analyzer("standard")))
                    .should(shd2 -> shd2
                            .match(m2 -> m2
                                    .query(q2 -> q2.stringValue(query))
                                    .field("classification")
                                    .boost(0.1f)))
                    .minimumShouldMatch("1")
            ));
        }

        // 4. Rerank results for having the full name in the default language.
        query4QueryBuilder.should(m -> m.match(inner -> inner
                .query(q -> q.stringValue(query))
                .field(String.format("name.%s.raw", language))
                .analyzer("search")
                .fuzziness(lenient ? "auto" : "0")
        ));

        // Weigh the resulting score by importance. Use a linear scale function that ensures that the weight
        // never drops to 0 and cancels out the ES score.
        finalQueryWithoutTagFilterBuilder = new Query.Builder().functionScore(fs -> fs
                .query(query4QueryBuilder.build().toQuery())
                .functions(fn1 -> fn1
                        .linear(df1 -> df1
                                .field("importance")
                                .placement(p1 -> p1
                                        .origin(JsonData.of(1.0))
                                        .scale(JsonData.of(0.6))
                                        .decay(0.5))))
                .functions(fn2 -> fn2
                        .filter(flt -> flt
                                .match(m -> m
                                        .query(q -> q.stringValue(query))
                                        .field("classification")))
                        .weight(0.1))
                .scoreMode(FunctionScoreMode.Sum)
        );

        // Filter for later: records that have a house number and no name must only appear when the house number matches.
        queryBuilderForTopLevelFilter = QueryBuilders.bool()
                .should(q1 -> q1.bool(qin -> qin
                        .mustNot(mn -> mn.exists(ex -> ex.field("housenumber")))))
                .should(q2 -> q2.match(m2 -> m2
                        .query(iq -> iq.stringValue(query))
                        .field("housenumber")
                        .analyzer("standard")))
                .should(q3 -> q3.exists(ex2 -> ex2
                        .field(String.format("name.%s.raw", language))));
    }

    public SearchQueryBuilder(StructuredSearchRequest request, String language, String[] languages, boolean lenient)
    {
        var hasSubStateField = request.hasCounty() || request.hasCityOrPostCode() || request.hasDistrict() || request.hasStreet();
        var query4QueryBuilder = new AddressQueryBuilder(lenient, language, languages)
                .addCountryCode(request.getCountryCode(), request.hasState() || hasSubStateField)
                .addState(request.getState(), hasSubStateField)
                .addCounty(request.getCounty(), request.hasCityOrPostCode() || request.hasDistrict() || request.hasStreet())
                .addCity(request.getCity(), request.hasDistrict(), request.hasStreet(), request.hasPostCode())
                .addPostalCode(request.getPostCode())
                .addDistrict(request.getDistrict(), request.hasStreet())
                .addStreetAndHouseNumber(request.getStreet(), request.getHouseNumber())
                .getQuery();

        finalQueryWithoutTagFilterBuilder = new Query.Builder().functionScore(fs -> fs
                .query(query4QueryBuilder)
                .functions(fn1 -> fn1
                        .linear(df1 -> df1
                                .field("importance")
                                .placement(p1 -> p1
                                        .origin(JsonData.of(1.0))
                                        .scale(JsonData.of(1.0))
                                        .decay(0.5))))
                .scoreMode(FunctionScoreMode.Sum));

        var hasHouseNumberQuery = QueryBuilders.exists().field(Constants.HOUSENUMBER).build().toQuery();
        var isHouseQuery = QueryBuilders.term().field(Constants.OBJECT_TYPE).value(FieldValue.of("house")).build().toQuery();
        var typeOtherQuery = QueryBuilders.term().field(Constants.OBJECT_TYPE).value(FieldValue.of("other")).build().toQuery();
        if (!request.hasHouseNumber())
        {
            queryBuilderForTopLevelFilter = QueryBuilders.bool().mustNot(hasHouseNumberQuery)
                    .mustNot(isHouseQuery)
                    .mustNot(typeOtherQuery);
        }
        else {
            var noHouseOrHouseNumber = QueryBuilders.bool().should(hasHouseNumberQuery)
                    .should(QueryBuilders.bool().mustNot(isHouseQuery).build().toQuery())
                    .build()
                    .toQuery();
            queryBuilderForTopLevelFilter = QueryBuilders.bool().must(noHouseOrHouseNumber)
                    .mustNot(typeOtherQuery);
        }

        osmTagFilter = new OsmTagFilter();
    }

    public SearchQueryBuilder withLocationBias(Point point, double scale, int zoom) {
        if (point == null || zoom < 4) return this;

        if (zoom > 18) {
            zoom = 18;
        }
        double radius = (1 << (18 - zoom)) * 0.25;
        final double fnscale = (scale <= 0.0) ? 0.0000001 : scale;

        Map<String, Object> params = new HashMap<>();
        params.put("lon", point.getX());
        params.put("lat", point.getY());

        finalQueryWithoutTagFilterBuilder = new Query.Builder().functionScore(fs -> fs
                .query(finalQueryWithoutTagFilterBuilder.build())
                .functions(fn1 -> fn1.exp(ex -> ex
                        .field("coordinate")
                        .placement(p -> p.origin(JsonData.of(params)).scale(JsonData.of(radius + "km")))))
                .functions(fn2 -> fn2.linear(lin -> lin
                        .field("importance")
                        .placement(p -> p.origin(JsonData.of(1.0)).scale(JsonData.of(fnscale)).decay(0.5))))
                .boostMode(FunctionBoostMode.Multiply)
                .scoreMode(FunctionScoreMode.Max)
        );

        return this;
    }

    public SearchQueryBuilder withBoundingBox(Envelope bbox) {
        if (bbox != null) {
            bboxQueryBuilder = QueryBuilders.geoBoundingBox()
                    .field("coordinate")
                    .boundingBox(b -> b.coords(c -> c
                            .top(bbox.getMaxY())
                            .bottom(bbox.getMinY())
                            .left(bbox.getMinX())
                            .right(bbox.getMaxX())));
        }

        return this;
    }

    public SearchQueryBuilder withOsmTagFilters(List<TagFilter> filters) {
        osmTagFilter.withOsmTagFilters(filters);
        return this;
    }

    public SearchQueryBuilder withLayerFilters(Set<String> filters) {
        if (!filters.isEmpty()) {
            List<FieldValue> terms = new ArrayList<>();
            for (var filter : filters) {
                terms.add(FieldValue.of(filter));
            }
            layerQueryBuilder = QueryBuilders.terms().field("type").terms(t -> t.value(terms));
        }

        return this;
    }

    public Query buildQuery() {
        if (finalQuery == null) {
            finalQuery = BoolQuery.of(q -> {
                q.must(finalQueryWithoutTagFilterBuilder.build());
                if (queryBuilderForTopLevelFilter != null) {
                    q.filter(queryBuilderForTopLevelFilter.build().toQuery());
                }

                q.filter(f -> f.bool(fb -> fb
                        .mustNot(n -> n.ids(i -> i.values(PhotonIndex.PROPERTY_DOCUMENT_ID)))
                ));

                final var tagFilters = osmTagFilter.build();
                if (tagFilters != null) {
                    q.filter(tagFilters);
                }

                if (bboxQueryBuilder != null) {
                    q.filter(bboxQueryBuilder.build().toQuery());
                }

                if (layerQueryBuilder != null) {
                    q.filter(layerQueryBuilder.build().toQuery());
                }

                return q;
            }).toQuery();
        }

        return finalQuery;
    }
}
