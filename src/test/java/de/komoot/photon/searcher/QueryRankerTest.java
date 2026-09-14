package de.komoot.photon.searcher;

import de.komoot.photon.opensearch.DocFields;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

public class QueryRankerTest {

    @ParameterizedTest
    @ValueSource(strings = {"Munich Main Station", "munich main station", "munich  main station"})
    void testSimpleNameFullMatch(String query) {
        QueryReRankerAssert.assertThat(query, "en")
                .scoresResultEqualTo(1.0,
                        new MockPhotonResult()
                                .putName("default", "München Hbf")
                                .putName("en", "Munich Main Station"));

    }

    @ParameterizedTest
    @ValueSource(strings = {"Bo sjukeheim", "bø sjukeheim", "BO SJUKEHEIM"})
    void testFoldedQueryMatchesDiacriticName(String query) {
        QueryReRankerAssert.assertThat(query, "no")
                .scoresResultEqualTo(1.0, new MockPhotonResult().putName("no", "Bø sjukeheim"));
    }

    @Test
    void testTrailingPeriodIsNotPartOfTheTerm() {
        QueryReRankerAssert.assertThat("st. olavs plass", "no")
                .scoresResultEqualTo(1.0, new MockPhotonResult().putName("no", "St. Olavs plass"));
    }

    @Test
    void testFoldedQueryOutranksUnrelatedName() {
        QueryReRankerAssert.assertThat("bo sjukeheim", "no")
                .ranksResultsInOrder(
                        new MockPhotonResult().putName("no", "Bø sjukeheim"),
                        new MockPhotonResult().putName("no", "Borgen sykehjem"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"en", "default", "alt"})
    void testPartialNameMatchIsLessThanFullMatch(String lang) {
        QueryReRankerAssert.assertThat("munich main", "en")
                .ranksResultsInOrder(
                        new MockPhotonResult().putName(lang, "Munich Main"),
                        new MockPhotonResult().putName(lang, "Munich Main Station")
                );

        QueryReRankerAssert.assertThat("munich main", "en")
                .ranksResultsInOrder(
                        new MockPhotonResult().putName(lang, "Munich Main"),
                        new MockPhotonResult().putName(lang, "Munich Main S"),
                        new MockPhotonResult().putName(lang, "Munich Mains")
                );

        QueryReRankerAssert.assertThat("munich main station", "en")
                .ranksResultsInOrder(
                        new MockPhotonResult().putName(lang, "Munich Main Station"),
                        new MockPhotonResult().putName(lang, "Main Station Munich"),
                        new MockPhotonResult().putName(lang, "Munich Station")
                );
    }

    @Test
    void testLocalizedPreferred() {
        QueryReRankerAssert.assertThat("foobar", "en")
                .ranksResultsInOrder(
                        new MockPhotonResult()
                                .putName("en", "foobar")
                                .putName("default", "fuubaz"),
                        new MockPhotonResult()
                                .putName("default", "foobar")
                                .putName("en", "foobar street"),
                        new MockPhotonResult()
                                .putName("default", "foobar")
                                .putName("en", "fuubaz")
                );
    }

    @ParameterizedTest
    @ValueSource(strings = {"Oslo, Norge", "Oslo Norway", "Oslo, no",
                            "Norge Oslo", "Norway Oslo", "no, oslo"})
    void testScoreCountryBetterThanAddress(String query) {
        QueryReRankerAssert.assertThat(query, "en")
                .ranksResultsInOrder(
                        new MockPhotonResult()
                                .putName("default", "Oslo")
                                .putLocalized(DocFields.COUNTRY, "default", "Norge")
                                .putLocalized(DocFields.COUNTRY, "en", "Norway")
                                .put(DocFields.COUNTRYCODE, "no"),
                        new MockPhotonResult()
                                .putName("default", "Oslo")
                                .putLocalized(DocFields.CITY, "default", "Norge")
                                .putLocalized(DocFields.CITY, "en", "Norway")
                                .putLocalized(DocFields.DISTRICT, "default", "no")
                );
    }

    @ParameterizedTest
    @ValueSource(strings = {"Oslo, Norge, foo", "Oslo Norway foo", "Oslo, no 3",
            "i Norge Oslo", "in Norway Oslo", "23 no, oslo"})
    void testScoreCountryWorseThanAddressInMiddleOfQuery(String query) {
        QueryReRankerAssert.assertThat(query, "en")
                .ranksResultsInOrder(
                        new MockPhotonResult()
                                .putName("default", "Oslo")
                                .putLocalized(DocFields.CITY, "default", "Norge")
                                .putLocalized(DocFields.CITY, "en", "Norway")
                                .putLocalized(DocFields.DISTRICT, "default", "no"),
                        new MockPhotonResult()
                                .putName("default", "Oslo")
                                .putLocalized(DocFields.COUNTRY, "default", "Norge")
                                .putLocalized(DocFields.COUNTRY, "en", "Norway")
                                .put(DocFields.COUNTRYCODE, "no")
                );
    }

    @Test
    void testDoNotMatchCountryOnFullQuery() {
        QueryReRankerAssert.assertThat("liechtenstein", "de")
                .scoresResultEqualTo(0.0,
                        new MockPhotonResult(0.0).putLocalized(DocFields.COUNTRY, "de", "liechtenstein"));
    }

    @Test
    void testDoNotMatchCountryCodeOnFullQuery() {
        QueryReRankerAssert.assertThat("li", "de")
                .scoresResultEqualTo(0.0,
                        new MockPhotonResult(0.0).put(DocFields.COUNTRYCODE, "li"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"en", "default"})
    void testHousenumberRankingPreferStreetMatch(String lang) {
        QueryReRankerAssert.assertThat("12 main", lang)
                .ranksResultsInOrder(
                        new MockPhotonResult()
                                .put(DocFields.HOUSENUMBER, "12")
                                .putLocalized(DocFields.STREET, "default", "Main"),
                        new MockPhotonResult()
                                .put(DocFields.HOUSENUMBER, "12")
                                .putLocalized(DocFields.STREET, "default", "Chruch Street")
                                .putName("default", "Main")
                );

    }

    @Test
    void testHousenumberFullMatchPrefered() {
        QueryReRankerAssert.assertThat("12 main", "default")
                .ranksResultsInOrder(
                        new MockPhotonResult()
                                .put(DocFields.HOUSENUMBER, "12")
                                .putLocalized(DocFields.STREET, "default", "Main"),
                        new MockPhotonResult()
                                .put(DocFields.HOUSENUMBER, "12 A")
                                .putLocalized(DocFields.STREET, "default", "Main")
                        );
    }
}
