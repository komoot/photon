package de.komoot.photon.opensearch;

import de.komoot.photon.Constants;
import de.komoot.photon.query.StructuredSearchRequest;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.*;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@NullMarked
public class SearchQueryBuilder extends BaseQueryBuilder {
    public FunctionScoreQuery.Builder innerQuery = new FunctionScoreQuery.Builder();
    double importance_factor = 40.0;

    public SearchQueryBuilder(@Nullable String query, boolean lenient, boolean suggestAddresses) {
        if (query == null) {
            // Empty query is used for category-only searches (e.g. /api?include=osm.place.city),
            // see SimpleSearchRequestFactory.create
            innerQuery.query(q -> q.matchAll(ma -> ma));
        } else if (!suggestAddresses && (query.length() < 4 || query.matches("^\\p{IsAlphabetic}+$"))) {
            importance_factor = setupShortQuery(query, lenient);
        } else {
            importance_factor = setupFullQuery(query, lenient, suggestAddresses);
        }

        innerQuery.functions(fvf -> fvf.fieldValueFactor(fvfb -> fvfb
                        .field("importance")
                        .factor((float) importance_factor)
                        .missing(0.00001)))
                .scoreMode(FunctionScoreMode.Sum)
                .boostMode(FunctionBoostMode.Sum);
    }

    public double setupShortQuery(String query, boolean lenient) {
        final int qlen = query.length();
        final var queryField = FieldValue.of(f -> f.stringValue(query));

        innerQuery.query(q -> q.bool(b -> {
            b.should(prefixMatch -> prefixMatch.match(nmb -> nmb
                    .query(queryField)
                    .field("collector.name.prefix")));

            b.should(fullMatch -> fullMatch.match(fzq -> fzq
                    .field("collector.field.name.full")
                    .query(queryField)
                    .fuzziness(qlen < 4 ? "0" : "AUTO")
                    .prefixLength(qlen <= 6 ? 1 : 2)
            ));

            if (lenient) {
                b.should(iq2 -> iq2.match(nmb -> nmb
                        .query(queryField)
                        .field("collector.name")
                        .fuzziness("AUTO")
                        .prefixLength(2)
                        .boost(0.2f)));
            }

            return b;
        }));

        innerQuery.functions(demotePoi -> demotePoi
                .weight(0.4f)
                .filter(fbool -> fbool.bool(c -> c
                        .mustNot(fp -> fp.term(tp -> tp
                                .field(Constants.OBJECT_TYPE)
                                .value(FieldValue.of("other"))))))
        );

        return 40.0;
    }

    public double setupFullQuery(String query, boolean lenient, boolean suggestAddresses) {
        final var queryField = FieldValue.of(f -> f.stringValue(query));
        final boolean isAlphabetic = query.matches("[\\p{IsAlphabetic} ]+");

        innerQuery.query(coreQuery -> coreQuery.bool(coreBuilder -> {
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
                    .queries(builder -> builder.bool(b -> {
                                var hnrQuery = QueryBuilders
                                        .functionScore()
                                        .boostMode(FunctionBoostMode.Multiply)
                                        .scoreMode(FunctionScoreMode.Sum)
                                        .query(hnr -> hnr
                                                .match(m1 -> m1
                                                        .query(queryField)
                                                        .boost(0.6f)
                                                        .field("housenumber")
                                                ))
                                        .functions(fullHnr -> fullHnr
                                                .weight(1.0f)
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
                                        .functions(constFact -> constFact.weight(1.0f))
                                        .build().toQuery();

                                if (suggestAddresses) {
                                    b.should(hnrQuery);
                                    b.must(m -> m.exists(e -> e.field(Constants.HOUSENUMBER)));
                                    b.mustNot(m -> m.exists(e -> e.field(Constants.NAME)));
                                } else {
                                    b.must(hnrQuery);
                                }
                                return b.must(parent -> parent
                                        .match(m2 -> m2
                                                .query(queryField)
                                                .field("collector.parent")
                                                .fuzziness(lenient ? "AUTO" : "0")
                                                .prefixLength(2)
                                        ));
                            }
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
        }));

        return isAlphabetic ? 40.0 : 20.0;
    }

    public SearchQueryBuilder(StructuredSearchRequest request, boolean lenient) {
        var hasSubStateField = request.hasCounty() || request.hasCityOrPostCode() || request.hasDistrict() || request.hasStreet();

        innerQuery.query(new AddressQueryBuilder(lenient)
                .addCountryCode(request.getCountryCode(), request.hasState() || hasSubStateField)
                .addState(request.getState(), hasSubStateField)
                .addCounty(request.getCounty(), request.hasCityOrPostCode() || request.hasDistrict() || request.hasStreet())
                .addCity(request.getCity(), request.hasDistrict(), request.hasStreet(), request.hasPostCode())
                .addPostalCode(request.getPostCode())
                .addDistrict(request.getDistrict(), request.hasStreet())
                .addStreetAndHouseNumber(request.getStreet(), request.getHouseNumber())
                .getQuery());

        var hasHouseNumberQuery = QueryBuilders.exists().field(Constants.HOUSENUMBER).build().toQuery();
        var isHouseQuery = QueryBuilders.term().field(Constants.OBJECT_TYPE).value(FieldValue.of("house")).build().toQuery();
        var typeOtherQuery = QueryBuilders.term().field(Constants.OBJECT_TYPE).value(FieldValue.of("other")).build().toQuery();

        if (!request.hasHouseNumber()) {
            outerQuery.filter(fn -> fn.bool(b -> b
                    .mustNot(hasHouseNumberQuery)
                    .mustNot(isHouseQuery)
                    .mustNot(typeOtherQuery)
            ));
        } else {
            var noHouseOrHouseNumber = QueryBuilders.bool().should(hasHouseNumberQuery)
                    .should(QueryBuilders.bool().mustNot(isHouseQuery).build().toQuery())
                    .build()
                    .toQuery();

            outerQuery.filter(fn -> fn.bool(b -> b
                    .must(noHouseOrHouseNumber)
                    .mustNot(typeOtherQuery)
            ));
        }
    }

    public void addLocationBias(@Nullable Point point, double scale, int zoom) {
        if (point == null || zoom < 4) return;

        if (zoom > 18) {
            zoom = 18;
        }
        double radius = (1 << (18 - zoom)) * 0.25;
        final double fnscale = Double.min(1.0, Double.max(0.0, scale));

        innerQuery.functions(fn1 -> fn1
                .weight((float) (38.0 * (1.0 - fnscale)))
                .exp(ex -> ex
                        .field("coordinate")
                        .placement(p -> p
                                .origin(JsonData.of(Map.of("lon", point.getX(), "lat", point.getY())))
                                .decay(0.8)
                                .offset(JsonData.of(radius / 10 + "km"))
                                .scale(JsonData.of(radius + "km")))));
    }

    public void addBoundingBox(@Nullable Envelope bbox) {
        if (bbox != null) {
            outerQuery.filter(q -> q.geoBoundingBox(bb -> bb
                    .field("coordinate")
                    .boundingBox(b -> b.coords(c -> c
                            .top(bbox.getMaxY())
                            .bottom(bbox.getMinY())
                            .left(bbox.getMinX())
                            .right(bbox.getMaxX())))
            ));
        }
    }

    public Query build() {
        return outerQuery
                .must(innerQuery.build().toQuery())
                .build().toQuery();
    }
}
