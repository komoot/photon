package de.komoot.photon.query;

import de.komoot.photon.PhotonDoc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

public class QueryWithVariantsTest extends BaseTesterQuery {
    @TempDir
    private Path dataDirectory;

    void setupWithVariants(String variants) throws IOException {
        setAnalyzerConfig(variants);
        setUpES(dataDirectory);
    }

    private void assertSearchDocs(String query, PhotonDoc... docs) {
        Object[] ids = Arrays.stream(docs).map(d -> (int) d.getOsmId()).toArray();
        assertThat(search(query))
                .extracting(p -> Objects.requireNonNull(p.get("osm_id")))
                .containsExactlyInAnyOrder(ids);
    }

    @Test
    void testSimpleVariantsAreFoundInName() throws IOException {
        setupWithVariants("""
                normalization:
                  input:
                    - ":: lower"
                variants:
                  - type: lexical
                    rules:
                      - words:
                         - foo => bar
                         - north -> south
                """);

        var fooDoc = createDoc("name", "FOO cafe", "alt_name", "Old foo");
        var northDoc = createDoc("name", "North Road");
        setupDocs(fooDoc, northDoc);

        assertSearchDocs("bar cafe berlin", fooDoc);
        assertThat(search("foo cafe berlin")).isEmpty();
        assertSearchDocs("North Road", northDoc);
        assertSearchDocs("SOUTH ROAD", northDoc);
    }

    @Test
    void testSimpleVariantsAreFoundInAddress() throws IOException {
        setupWithVariants("""
                normalization:
                  input:
                    - ":: lower"
                variants:
                  - type: lexical
                    rules:
                      - words:
                         - boulevard -> bvd
                """);

        var doc = createDoc("name", "Paul's")
                .addAddresses(Map.of(
                        "street", "Logan Boulevard",
                        "city", "Newtown"
                ), getProperties().getLanguages());
        setupDocs(doc);

        assertSearchDocs("pauls restaurant, logan boulevard, newtown", doc);
        assertSearchDocs("pauls restaurant, logan bvd, newtown", doc);
    }

    @Test
    void testLanguageVariantsAreFound() throws IOException {
        getProperties().setLanguages(Set.of("en", "fr"));
        setupWithVariants("""
                normalization:
                  input:
                    - ":: lower"
                variants:
                  - type: lexical
                    rules:
                      - lang: en
                        words:
                         - boulevard -> bvd
                      - lang: fr
                        words:
                         - boulevard -> bld
                """);

        var enDoc = createDoc("name", "Foo Boulevard", "name:en", "Foo Boulevard");
        var frDoc = createDoc("name", "Foo Boulevard", "name:fr", "Foo Boulevard");
        setupDocs(enDoc, frDoc);

        assertSearchDocs("The Foo Boulevard", enDoc, frDoc);
        assertSearchDocs("The Foo Bvd", enDoc);
        assertSearchDocs("The Foo Bld", frDoc);
    }

    @Test
    void testCountryVariantsAreFound() throws IOException {
        getProperties().setLanguages(Set.of("en", "fr"));
        setupWithVariants("""
                normalization:
                  input:
                    - ":: lower"
                variants:
                  - type: lexical
                    rules:
                      - country: us
                        words:
                         - boulevard -> bvd
                      - country: gb
                        words:
                         - boulevard -> bld
                """);

        var usDoc = createDoc("name", "Foo Boulevard").countryCode("us");
        var gbDoc = createDoc("name", "Foo Boulevard").countryCode("gb");
        setupDocs(usDoc, gbDoc);

        assertSearchDocs("The Foo Boulevard", usDoc, gbDoc);
        assertSearchDocs("The Foo Bvd", usDoc);
        assertSearchDocs("The Foo Bld", gbDoc);
    }
}
