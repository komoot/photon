package de.komoot.photon.api;

import de.komoot.photon.App;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

/**
 * API integration tests for the country code parameter on the /api endpoint
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiCountryCodeFilterTest extends ApiBaseTester {

    private static final String BASE_URL = "/api?q=paris";

    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws Exception {
        setUpES(dataDirectory);
        Importer instance = makeImporter();

        instance.add(
            List.of(
                new PhotonDoc()
                    .placeId("1000")
                    .osmType("N")
                    .osmId(1000)
                    .tagKey("place")
                    .tagValue("city")
                    .names(makeDocNames("name", "Paris"))
                    .countryCode("FR")
            )
        );
        instance.add(
            List.of(
                new PhotonDoc()
                    .placeId("1001")
                    .osmType("N")
                    .osmId(1001)
                    .tagKey("place")
                    .tagValue("city")
                    .names(makeDocNames("name", "Paris"))
                    .countryCode("US")
            )
        );
        instance.add(
            List.of(
                new PhotonDoc()
                        .placeId("1002")
                        .osmType("N")
                        .osmId(1002)
                        .tagKey("place")
                        .tagValue("city")
                        .names(makeDocNames("name", "Paris"))
                        .countryCode("DE")
            )
        );
        instance.finish();
        refresh();
        startAPI();
    }

    @AfterAll
    public void tearDown() {
        App.shutdown();
        shutdownES();
    }

    private String buildApiUrl(String... countryCodes) {
        StringBuilder sb = new StringBuilder(BASE_URL);
        for (String cc : countryCodes) {
            sb.append("&countrycode=").append(cc);
        }
        return sb.toString();
    }

    @Test
    void testNoCountryCodeReturnsAllPlaces() throws Exception {
        assertThatJson(readURL(BASE_URL)).isObject()
                .node("features").isArray().hasSize(3);
    }

    @Test
    void testSingleCountryCodeFiltersResults() throws Exception {
        assertThatJson(readURL(buildApiUrl("FR"))).isObject()
                .node("features").isArray().hasSize(1);

        assertThatJson(readURL(buildApiUrl("US"))).isObject()
                .node("features").isArray().hasSize(1);

                assertThatJson(readURL(buildApiUrl("GB"))).isObject()
                .node("features").isArray().hasSize(0);
    }

    @Test
    void testSingleCountryCodeReturnsCorrectCountry() throws Exception {
        assertThatJson(readURL(buildApiUrl("FR"))).isObject()
                .node("features").isArray()
                .element(0).isObject()
                .node("properties").isObject()
                .containsEntry("countrycode", "FR");

        assertThatJson(readURL(buildApiUrl("DE"))).isObject()
                .node("features").isArray()
                .element(0).isObject()
                .node("properties").isObject()
                .containsEntry("countrycode", "DE");
    }

    @Test
    void testUnknownCountryCodeReturnsNoResults() throws Exception {
        assertThatJson(readURL(buildApiUrl("GB"))).isObject()
                .node("features").isArray().isEmpty();
    }

    @Test
    void testCountryCodeIsCaseInsensitive() throws Exception {
        assertThatJson(readURL(buildApiUrl("FR"))).isObject()
                .node("features").isArray().hasSize(1);

        assertThatJson(readURL(buildApiUrl("fr"))).isObject()
                .node("features").isArray().hasSize(1);
    }

    @Test
    void testMultipleCountryCodesReturnResultsFromAllSpecified() throws Exception {
        assertThatJson(readURL(buildApiUrl("FR", "US"))).isObject()
                .node("features").isArray().hasSize(2);
    }

    @Test
    void testMultipleCountryCodesExcludeUnknownCountry() throws Exception {
        assertThatJson(readURL(buildApiUrl("FR", "GB"))).isObject()
                .node("features").isArray()
                .element(0).isObject()
                .node("properties").isObject()
                .containsEntry("countrycode", "FR");
    }

    @Test
    void testMultipleCountryCodesAllPresent() throws Exception {
        assertThatJson(readURL(buildApiUrl("FR", "US", "DE"))).isObject()
                .node("features").isArray().hasSize(3);
    }
}
