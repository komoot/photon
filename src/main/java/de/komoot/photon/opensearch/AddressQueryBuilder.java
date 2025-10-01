package de.komoot.photon.opensearch;

import de.komoot.photon.Constants;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.*;
import org.opensearch.common.unit.Fuzziness;

import java.util.Objects;

public class AddressQueryBuilder {
    private static final float STATE_BOOST = 0.1f; // state is unreliable - some locations have e.g. "NY", some "New York".
    private static final float COUNTY_BOOST = 4.0f;
    private static final float CITY_BOOST = 3.0f;
    private static final float POSTAL_CODE_BOOST = 7.0f;
    private static final float DISTRICT_BOOST = 2.0f;
    private static final float STREET_BOOST = 5.0f; // we filter streets in the wrong city / district / ... so we can use a high boost value
    private static final float HOUSE_NUMBER_BOOST = 10.0f;

    private BoolQuery.Builder query = QueryBuilders.bool();

    private BoolQuery.Builder cityFilter;

    private boolean lenient;

    public AddressQueryBuilder(boolean lenient) {
        this.lenient = lenient;
    }

    public Query getQuery() {
        return query.build().toQuery();
    }

    public AddressQueryBuilder addCountryCode(String countryCode, boolean hasMoreDetails) {
        if (countryCode == null) return this;

        query.filter(QueryBuilders.term().field(Constants.COUNTRYCODE).value(FieldValue.of(countryCode.toUpperCase())).build().toQuery());
        if(!hasMoreDetails)
        {
            query.filter(QueryBuilders.term()
                    .field(Constants.OBJECT_TYPE)
                    .value(FieldValue.of("country"))
                    .build()
                    .toQuery());
        }

        return this;
    }

    public AddressQueryBuilder addState(String state, boolean hasMoreDetails) {
        if (state == null) return this;

        var stateQuery = getNameOrFieldQuery(Constants.STATE, state, STATE_BOOST, "state", hasMoreDetails);
        query.should(stateQuery);
        return this;
    }

    public AddressQueryBuilder addCounty(String county, boolean hasMoreDetails) {
        if (county == null) return this;

        addNameOrFieldQuery(Constants.COUNTY, county, COUNTY_BOOST, "county", hasMoreDetails);
        return this;
    }

    public AddressQueryBuilder addCity(String city, boolean hasDistrict, boolean hasStreet, boolean hasPostCode) {
        if (city == null) return this;

        Query combinedQuery;
        Query nameQuery = getFuzzyNameQueryBuilder(city, "city").boost(CITY_BOOST)
                .build()
                .toQuery();
        Query fieldQuery = getFuzzyQuery(Constants.CITY, city, CITY_BOOST);

        if (!hasDistrict) {
            var districtNameQuery = getFuzzyNameQueryBuilder(city, "district").boost(0.95f * CITY_BOOST)
                    .build()
                    .toQuery();
            nameQuery = QueryBuilders.bool()
                    .should(nameQuery)
                    .should(districtNameQuery)
                    .minimumShouldMatch("1")
                    .build()
                    .toQuery();

            var districtFieldQuery = getFuzzyQuery(Constants.DISTRICT, city, 0.95f * CITY_BOOST);
            fieldQuery = QueryBuilders.bool()
                    .should(fieldQuery)
                    .should(districtFieldQuery)
                    .minimumShouldMatch("1")
                    .build()
                    .toQuery();
        }

        if (!hasStreet && !hasDistrict) {
            // match only name
            if (hasPostCode) {
                // post code can implicitly specify a district that has the city in the address field (instead of the name)
                combinedQuery = QueryBuilders.bool()
                        .should(nameQuery)
                        .should(fieldQuery)
                        .build()
                        .toQuery();
            }
            else {
                combinedQuery = nameQuery;
            }
        } else {
            // match only address field
            combinedQuery = fieldQuery;
        }

        addToCityFilter(combinedQuery);
        query.must(combinedQuery);

        return this;
    }

    private void addToCityFilter(Query query) {
        if (cityFilter == null) {
            cityFilter = QueryBuilders.bool();
        }

        cityFilter.should(query);
    }

    public AddressQueryBuilder addPostalCode(String postalCode) {
        if (postalCode == null) return this;

        Fuzziness fuzziness = lenient ? Fuzziness.AUTO : Fuzziness.ZERO;

        Query query;
        if (postalCode.contains(" ")) {
            query = QueryBuilders.match()
                    .field(Constants.POSTCODE)
                    .query(FieldValue.of(postalCode))
                    .fuzziness(fuzziness.asString())
                    .boost(POSTAL_CODE_BOOST)
                    .build()
                    .toQuery();
        } else {
            query = QueryBuilders.fuzzy()
                    .field(Constants.POSTCODE)
                    .value(FieldValue.of(postalCode))
                    .fuzziness(fuzziness.asString())
                    .boost(POSTAL_CODE_BOOST)
                    .build()
                    .toQuery();
        }

        addToCityFilter(query);
        this.query.must(query);

        return this;
    }

    public AddressQueryBuilder addDistrict(String district, boolean hasMoreDetails) {
        if (district == null) return this;

        addNameOrFieldQuery(Constants.DISTRICT, district, DISTRICT_BOOST, "district", hasMoreDetails);
        return this;
    }

    public AddressQueryBuilder addStreetAndHouseNumber(String street, String houseNumber) {
        if (street == null) {
            if (houseNumber != null) {
                // some hamlets have no street name and only number the buildings
                var houseNumberQuery = QueryBuilders.bool()
                        .mustNot(QueryBuilders.exists().field(Constants.STREET).build().toQuery())
                        .must(QueryBuilders.matchPhrase()
                                .field(Constants.HOUSENUMBER)
                                .query(houseNumber)
                                .build()
                                .toQuery());

                query.must(houseNumberQuery.build().toQuery());
            }

            return this;
        }

        Query streetQuery;

        if (lenient) {
            var nameFieldQuery = getFuzzyNameQueryBuilder(street, "street");
            if (houseNumber == null) {
                streetQuery = nameFieldQuery.boost(STREET_BOOST).build().toQuery();
            } else {
                streetQuery = QueryBuilders.bool()
                        .should(getFuzzyQuery(Constants.STREET, street))
                        .should(nameFieldQuery.build().toQuery())
                        .minimumShouldMatch("1")
                        .boost(STREET_BOOST).build().toQuery();
            }
        } else {
            streetQuery = getFuzzyQuery(Constants.STREET, street, STREET_BOOST);
        }

        if (houseNumber != null) {
            var houseNumberMatchQuery = QueryBuilders.bool().must(QueryBuilders.matchPhrase()
                    .field(Constants.HOUSENUMBER)
                    .query(houseNumber)
                    .build()
                    .toQuery());

            houseNumberMatchQuery.filter(getFuzzyQuery(Constants.STREET, street));
            if (cityFilter != null) {
                houseNumberMatchQuery.filter(cityFilter.build().toQuery());
            }

            BoolQuery.Builder houseNumberQuery = QueryBuilders.bool()
                    .should(houseNumberMatchQuery.build().toQuery())
                    .should(QueryBuilders.bool().mustNot(QueryBuilders.exists().field(Constants.HOUSENUMBER).build().toQuery()).build().toQuery())
                    .boost(HOUSE_NUMBER_BOOST);

            query.must(houseNumberQuery.build().toQuery());
        }

        query.must(streetQuery);

        return this;
    }

    private Query getFuzzyQuery(String name, String value) {
        return getFuzzyQuery(name, value, 1.0f);
    }

    private Query getFuzzyQuery(String name, String value, float boost) {
        final var field = "collector.field." + name;

        if (lenient) {
            return QueryBuilders.match()
                    .field(field)
                    .query(FieldValue.of(value))
                    .fuzziness(Fuzziness.AUTO.asString())
                    .boost(boost)
                    .build()
                    .toQuery();
        }

        return QueryBuilders.matchPhrase()
                .field(field)
                .query(value)
                .boost(boost)
                .build()
                .toQuery();
    }

    private BoolQuery.Builder getFuzzyNameQueryBuilder(String value, String objectType) {
        var or = QueryBuilders.bool();
        or.should(getFuzzyQuery("name", value));

        return or.minimumShouldMatch("1")
                .filter(QueryBuilders.term()
                        .field(Constants.OBJECT_TYPE)
                        .value(FieldValue.of(objectType))
                        .build()
                        .toQuery());
    }

    private static boolean isCityRelatedField(String name) {
        return Objects.equals(name, Constants.POSTCODE) || Objects.equals(name, Constants.CITY) || Objects.equals(name, Constants.DISTRICT);
    }

    private void addNameOrFieldQuery(String field, String value, float boost, String objectType, boolean hasMoreDetails) {
        var query = getNameOrFieldQuery(field, value, boost, objectType, hasMoreDetails);
        if (isCityRelatedField(field)) {
            addToCityFilter(query);
        }

        this.query.must(query);
    }

    private Query getNameOrFieldQuery(String field, String value, float boost, String objectType, boolean hasMoreDetails) {
        if (hasMoreDetails) {
            return getFuzzyQuery(field, value);
        }

        return getFuzzyNameQueryBuilder(value, objectType)
                .boost(boost)
                .build()
                .toQuery();
    }
}
