package de.komoot.photon.api;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import de.komoot.photon.App;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for dedupe works correctly
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiDedupeTest extends ApiBaseTester {

    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws Exception {
        setUpES(dataDirectory);
        Importer instance = makeImporter();

        instance.add(
            List.of(
                new PhotonDoc()
                    .placeId("1000")
                    .osmType("W")
                    .osmId(1000)
                    .tagKey("highway")
                    .tagValue("residential")
                    .postcode("1000")
                    .rankAddress(26)
                    .centroid(makePoint(15.94174, 45.80355))
                    .names(makeDocNames("name", "Pfanove"))
            )
        );
        instance.add(
            List.of(
                new PhotonDoc()
                    .placeId("1001")
                    .osmType("W")
                    .osmId(1001)
                    .tagKey("highway")
                    .tagValue("residential")
                    .postcode("1000")
                    .rankAddress(26)
                    .centroid(makePoint(15.94192, 45.802429))
                    .names(makeDocNames("name", "Pfanove"))
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

    @ParameterizedTest
    @ValueSource(
        strings = {
            "/api?q=Pfanove", // basic search
            "/api?q=Pfanove&dedupe=1", // explicitly enabled dedupe
        }
    )
    void testEnabledDedupe(String baseUrl) throws Exception {
        assertThatJson(readURL(baseUrl)).isObject().node("features").isArray().hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/api?q=Pfanove&dedupe=0" })
    void testDisabledDedupe(String baseUrl) throws Exception {
        assertThatJson(readURL(baseUrl)).isObject().node("features").isArray().hasSize(2);
    }
}
