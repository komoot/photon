package de.komoot.photon.opensearch;

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
    public static final float IMPORTANCE_FACTOR = 30f;

    public FunctionScoreQuery.Builder innerQuery = new FunctionScoreQuery.Builder();

    public SearchQueryBuilder(@Nullable String query, boolean lenient, boolean suggestAddresses) {
        if (query == null) {
            // Empty query is used for category-only searches (e.g. /api?include=osm.place.city),
            // see SimpleSearchRequestFactory.create
            innerQuery.query(q -> q.matchAll(ma -> ma));
        } else if (!suggestAddresses && (query.length() < 4 || query.matches("^\\p{IsAlphabetic}+$"))) {
            setupShortQuery(query, lenient);
        } else {
            setupFullQuery(query, lenient, suggestAddresses);
        }

        innerQuery.functions(fvf -> fvf.fieldValueFactor(fvfb -> fvfb
                        .field(DocFields.IMPORTANCE)
                        .factor(IMPORTANCE_FACTOR)
                        .missing(0.00001)))
                .scoreMode(FunctionScoreMode.Sum)
                .boostMode(FunctionBoostMode.Sum);
    }

    public void setupShortQuery(String query, boolean lenient) {
        final int qlen = query.length();
        final var queryField = FieldValue.of(f -> f.stringValue(query));

        innerQuery.query(q -> q.bool(b -> {
            b.should(prefixMatch -> prefixMatch.match(nmb -> nmb
                    .query(queryField)
                    .field(DocFields.COLLECTOR + ".name.prefix")));

            b.should(fullMatch -> fullMatch.match(fzq -> fzq
                    .field(DocFields.COLLECTOR + ".field.name.full")
                    .query(queryField)
                    .fuzziness(qlen < 4 ? "0" : "AUTO")
                    .prefixLength(qlen <= 6 ? 1 : 2)
            ));

            if (lenient) {
                b.should(iq2 -> iq2.match(nmb -> nmb
                        .query(queryField)
                        .field(DocFields.COLLECTOR + ".name")
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
                                .field(DocFields.OBJECT_TYPE)
                                .value(FieldValue.of("other"))))))
        );
    }

    public void setupFullQuery(String query, boolean lenient, boolean suggestAddresses) {
        final var queryField = FieldValue.of(f -> f.stringValue(query));
        final boolean isAlphabetic = query.matches("[\\p{IsAlphabetic} ]+");

        innerQuery.query(coreQuery -> coreQuery.bool(coreBuilder -> {
            coreBuilder.must(fullMatch -> fullMatch.match(fmb -> {
                fmb.query(queryField);
                fmb.field(DocFields.COLLECTOR + ".all.ngram");
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
                            .field(DocFields.COLLECTOR + ".name")
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
                                                        .field(DocFields.HOUSENUMBER)
                                                ))
                                        .functions(fullHnr -> fullHnr
                                                .weight(1.0f)
                                                .filter(hmrExact -> hmrExact
                                                        .terms(t -> t
                                                                .field(DocFields.HOUSENUMBER + ".full")
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
                                    b.must(m -> m.exists(e -> e.field(DocFields.HOUSENUMBER)));
                                    b.mustNot(m -> m.exists(e -> e.field(DocFields.NAME)));
                                } else {
                                    b.must(hnrQuery);
                                }
                                return b.must(parent -> parent
                                        .match(m2 -> m2
                                                .query(queryField)
                                                .field(DocFields.COLLECTOR + ".parent")
                                                .fuzziness(lenient ? "AUTO" : "0")
                                                .prefixLength(2)
                                        ));
                            }
                    ))
            ));
            coreBuilder.should(fullWord -> fullWord.match(fwm -> fwm
                    .query(queryField)
                    .field(DocFields.COLLECTOR + ".all")
            ));
            if (!lenient && query.indexOf(',') < 0) {
                coreBuilder.should(prefixMatch -> prefixMatch.match(nmb -> nmb
                        .query(queryField)
                        .field(DocFields.COLLECTOR + ".name.prefix")
                        .boost(isAlphabetic ? 0.1f : 0.01f)
                ));
            }

            return coreBuilder;
        }));
    }

    public SearchQueryBuilder(StructuredSearchRequest request, boolean lenient) {
        var hasSubStateField = request.hasCounty() || request.hasCityOrPostCode() || request.hasDistrict() || request.hasStreet();
        var postcodeIsSubField = request.hasHouseNumber() || request.hasStreet() || request.hasCity() || request.hasDistrict() || request.hasCounty() || request.hasState();

        innerQuery.query(new AddressQueryBuilder(lenient)
                .addCountryCode(request.getCountryCode(), request.hasState() || hasSubStateField)
                .addState(request.getState(), hasSubStateField)
                .addCounty(request.getCounty(), request.hasCityOrPostCode() || request.hasDistrict() || request.hasStreet())
                .addCity(request.getCity(), request.hasDistrict(), request.hasStreet(), request.hasPostCode())
                .addPostalCode(request.getPostCode(), postcodeIsSubField)
                .addDistrict(request.getDistrict(), request.hasStreet())
                .addStreetAndHouseNumber(request.getStreet(), request.getHouseNumber())
                .getQuery());

        var hasHouseNumberQuery = QueryBuilders.exists().field(DocFields.HOUSENUMBER).build().toQuery();
        var isHouseQuery = QueryBuilders.term().field(DocFields.OBJECT_TYPE).value(FieldValue.of("house")).build().toQuery();
        var typeOtherQuery = QueryBuilders.term().field(DocFields.OBJECT_TYPE).value(FieldValue.of("other")).build().toQuery();

        if (postcodeIsSubField) {
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
    }

    public void addLocationBias(Point point, float weight, double radius, double decayRadius) {
        innerQuery.functions(fn1 -> fn1
                .weight(weight)
                .exp(ex -> ex
                        .field(DocFields.COORDINATE)
                        .placement(p -> p
                                .origin(JsonData.of(Map.of("lon", point.getX(), "lat", point.getY())))
                                .decay(0.5)
                                .offset(JsonData.of(radius + "km"))
                                .scale(JsonData.of(decayRadius + "km")))));
    }

    public void addBoundingBox(@Nullable Envelope bbox) {
        if (bbox != null) {
            outerQuery.filter(q -> q.geoBoundingBox(bb -> bb
                    .field(DocFields.COORDINATE)
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
