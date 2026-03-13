package de.komoot.photon.query;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that the countrycode parameter on the /api endpoint correctly filters results.
 */
class QueryCountryCodeFilterTest extends ESBaseTester {

    @BeforeEach
    void setup(@TempDir Path dataDirectory) throws IOException {
        setUpES(dataDirectory);

        Importer instance = makeImporter();
        instance.add(List.of(
                new PhotonDoc().placeId("1").osmType("N").osmId(1).tagKey("place").tagValue("city")
                        .names(makeDocNames("name", "Paris"))
                        .countryCode("FR"),
                new PhotonDoc().placeId("2").osmType("N").osmId(2).tagKey("place").tagValue("city")
                        .names(makeDocNames("name", "Paris"))
                        .countryCode("US"),
                new PhotonDoc().placeId("3").osmType("N").osmId(3).tagKey("place").tagValue("city")
                        .names(makeDocNames("name", "Paris"))
                        .countryCode("DE")
        ));
        instance.finish();
        refresh();
    }

    private List<PhotonResult> search(String query, String... countryCodes) {
        var request = new SimpleSearchRequest();
        request.setQuery(query);
        request.setCountryCodes(List.of(countryCodes));
        return getServer().createSearchHandler(1).search(request);
    }

    private List<PhotonResult> searchAll(String query) {
        var request = new SimpleSearchRequest();
        request.setQuery(query);
        return getServer().createSearchHandler(1).search(request);
    }

    @Test
    void testCountryCodeFiltersToMatchingCountry() {
        assertThat(search("Paris", "FR")).hasSize(1);
        assertThat(search("Paris", "DE")).hasSize(1);
    }

    @Test
    void testCountryCodeExcludesOtherCountries() {
        var frResults = search("Paris", "FR");
        assertThat(frResults).hasSize(1);
        assertThat(frResults.getFirst().get("countrycode")).isEqualTo("FR");

        var deResults = search("Paris", "DE");
        assertThat(deResults).hasSize(1);
        assertThat(deResults.getFirst().get("countrycode")).isEqualTo("DE");
    }

    @Test
    void testNoCountryCodeReturnsAllCountries() {
        assertThat(searchAll("Paris")).hasSize(3);
    }

    @Test
    void testUnknownCountryCodeReturnsNoResults() {
        assertThat(search("Paris", "GB")).isEmpty();
    }

    @Test
    void testCountryCodeIsCaseInsensitive() {
        var upper = search("Paris", "FR");
        var lower = search("Paris", "fr");

        assertThat(upper).hasSize(1);
        assertThat(lower).hasSize(1);
        assertThat(lower.getFirst().get("countrycode"))
                .isEqualTo(upper.getFirst().get("countrycode"));
    }

    @Test
    void testMultipleCountryCodesReturnResultsFromAllSpecified() {
        assertThat(search("Paris", "FR", "DE")).hasSize(2);
    }

    @Test
    void testMultipleCountryCodesExcludesUnspecified() {
        assertThat(search("Paris", "FR", "GB")).hasSize(1);
    }
}
