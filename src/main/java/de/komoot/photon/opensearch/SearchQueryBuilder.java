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
import java.util.stream.Collectors;

public class SearchQueryBuilder {
    private ObjectBuilder<Query> finalQueryWithoutTagFilterBuilder;
    private BoolQuery.Builder queryBuilderForTopLevelFilter;
    private OsmTagFilter osmTagFilter = new OsmTagFilter();
    private GeoBoundingBoxQuery.Builder bboxQueryBuilder;
    private TermsQuery.Builder layerQueryBuilder;
    private Query finalQuery = null;

    public SearchQueryBuilder(String query, boolean lenient) {
        if (query.length() < 4 || query.matches("^\\p{IsAlphabetic}+$")) {
            setupShortQuery(query, lenient);
        } else {
            setupFullQuery(query, lenient);
        }
    }

    public void setupShortQuery(String query, boolean lenient) {
        final var queryField = FieldValue.of(f -> f.stringValue(query));

        final var prefixMatch = MatchQuery.of(nmb -> nmb
                                .query(queryField)
                                .field("collector.name.prefix")
                                .boost((query.length()) > 3 ? 0.5f : 0.8f));

        finalQueryWithoutTagFilterBuilder = new Query.Builder().functionScore(fs -> fs
                .query(q -> q.bool(b -> {
                    if (lenient) {
                        b.must(prefixOrFullName -> prefixOrFullName.bool(bi -> bi
                                .should(iq -> iq.match(prefixMatch))
                                .should(iq2 -> iq2.match(nmb -> nmb
                                        .query(queryField)
                                        .field("collector.name")
                                        .fuzziness("AUTO")
                                        .prefixLength(2)
                                        .boost(0.2f)))
                        ));
                    } else {
                        b.must(iq -> iq.match(prefixMatch));
                    }
                    b.should(fullMatch -> fullMatch.match(fmb -> fmb
                            .query(queryField)
                            .field("collector.all")
                            .boost((query.length()) > 3 ? 0.4f : 0.1f)));
                    return b;
                }))
                .functions(fvf -> fvf.fieldValueFactor(fvfb -> fvfb
                        .field("importance")
                        .factor(40.0)
                        .missing(0.00001)
                ))
                .functions(demotePoi -> demotePoi
                        .weight(0.2)
                        .filter(fbool -> fbool.bool(c -> c
                                .mustNot(fp -> fp.term(tp -> tp
                                        .field(Constants.OBJECT_TYPE)
                                        .value(FieldValue.of("other"))))))
                )
                .scoreMode(FunctionScoreMode.Sum)
                .boostMode(FunctionBoostMode.Sum)
        );
    }

    public void setupFullQuery(String query, boolean lenient) {
        final var queryField = FieldValue.of(f -> f.stringValue(query));
        final boolean isAlphabetic = query.matches("[\\p{IsAlphabetic} ]+");
        finalQueryWithoutTagFilterBuilder = new Query.Builder().functionScore(fs -> fs
                .query(coreQuery -> coreQuery.bool(coreBuilder -> {
                    coreBuilder.must(fullMatch -> fullMatch.match(fmb -> {
                        fmb.query(queryField);
                        fmb.field("collector.all.ngram");
                        fmb.boost(0.1f);

                        if (lenient) {
                            fmb.minimumShouldMatch("2<-1 6<-2");
                            fmb.fuzziness("AUTO");
                            fmb.prefixLength(2);
                        } else {
                            fmb.operator(Operator.And);
                        }
                        return fmb;
                    }));
                    coreBuilder.must(outer -> outer.disMax(outerb -> outerb
                            .boost(0.2f)
                            .queries(builder -> builder.match(q -> q
                                    .query(queryField)
                                    .field("collector.name")
                                    .fuzziness(lenient ? "AUTO" : "0")
                                    .prefixLength(2)
                                    .boost(isAlphabetic ? 1.5f : 1.0f)
                            ))
                            .queries(builder -> builder.bool(b -> b
                                    .must(hnrWeighted -> hnrWeighted
                                            .functionScore(hnrFS -> hnrFS
                                                    .boostMode(FunctionBoostMode.Multiply)
                                                    .scoreMode(FunctionScoreMode.Sum)
                                                    .query(hnr -> hnr
                                                            .match(m1 -> m1
                                                                    .query(queryField)
                                                                    .boost(0.6f)
                                                                    .field("housenumber")
                                                            ))
                                                    .functions(fullHnr -> fullHnr
                                                            .weight(1.0)
                                                            .filter(hmrExact -> hmrExact
                                                                    .terms(t -> t
                                                                            .field("housenumber.full")
                                                                            .boost(2f)
                                                                            .terms(tf -> tf.value(
                                                                                    Arrays.stream(query.toLowerCase().split("[ ,;]+"))
                                                                                            .map(s -> FieldValue.of(fv -> fv.stringValue(s)))
                                                                                            .collect(Collectors.toList())
                                                                            ))
                                                                    )
                                                            ))
                                                    .functions(constFact -> constFact.weight(1.0))
                                            ))
                                    .must(parent -> parent
                                            .match(m2 -> m2
                                                    .query(queryField)
                                                    .field("collector.parent")
                                                    .fuzziness(lenient ? "AUTO" : "0")
                                                    .prefixLength(2)
                                            ))
                            ))
                    ));
                    coreBuilder.should(fullWord -> fullWord.match(fwm -> fwm
                            .query(queryField)
                            .field("collector.all")
                    ));
                    if (!lenient && query.indexOf(',') < 0) {
                        coreBuilder.should(prefixMatch -> prefixMatch.match(nmb -> nmb
                                .query(queryField)
                                .field("collector.name.prefix")
                                .boost(isAlphabetic ? 0.1f : 0.01f)
                        ));
                    }

                    return coreBuilder;
                }))
                .functions(fvf -> fvf.fieldValueFactor(fvfb -> fvfb
                        .field("importance")
                        .factor(isAlphabetic ? 40.0 : 20.0)
                        .missing(0.00001)
                ))
                .scoreMode(FunctionScoreMode.Sum)
                .boostMode(FunctionBoostMode.Sum)
        );
    }

    public SearchQueryBuilder(StructuredSearchRequest request, boolean lenient)
    {
        var hasSubStateField = request.hasCounty() || request.hasCityOrPostCode() || request.hasDistrict() || request.hasStreet();
        var query4QueryBuilder = new AddressQueryBuilder(lenient)
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
                        .placement(p -> p
                                .origin(JsonData.of(params))
                                .decay(0.8)
                                .offset(JsonData.of(radius / 10 + "km"))
                                .scale(JsonData.of(radius + "km")))))
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