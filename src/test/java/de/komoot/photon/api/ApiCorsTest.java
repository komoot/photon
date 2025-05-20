package de.komoot.photon.api;

import de.komoot.photon.App;
import de.komoot.photon.Importer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiCorsTest extends ApiBaseTester {
    private static final String[] BASE_URLS = {"/api/?q=Berlin", "/reverse/?lat=52.54714&lon=13.39026", "/status/"};

    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws Exception {
        setUpES(dataDirectory);
        Importer instance = makeImporter();
        instance.add(List.of(createDoc(13.39026, 52.54714, 1001, 1001, "place", "suburb")
                .importance(0.3)
                .rankAddress(17)));
        instance.finish();
        refresh();
    }

    @AfterEach
    void shutdown() {
        App.shutdown();
    }

    @AfterAll
    @Override
    public void tearDown() {
        shutdownES();
    }

    /**
     * Test that the Access-Control-Allow-Origin header is not set
     */
    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testNoCors(String baseUrl) throws Exception {
        startAPI();

        var connection = connect(baseUrl);
        connection.setRequestProperty("Origin", "http://example.com");
        connection.connect();

        assertThat(connection.getHeaderField("Access-Control-Allow-Origin"))
                .isNull();
    }

    /**
     * Test that the Access-Control-Allow-Origin header is set to *
     */
    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testCorsAny(String baseUrl) throws Exception {
        startAPI("-cors-any");

        var connection = connect(baseUrl);
        connection.setRequestProperty("Origin", "http://example.com");
        connection.connect();

        assertThat(connection.getHeaderField("Access-Control-Allow-Origin"))
                .isEqualTo("*");
    }

    /**
     * Test that the Access-Control-Allow-Origin header is set to a specific domain
     */
    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testCorsOriginIsSetToSpecificDomain(String baseUrl) throws Exception {
        startAPI("-cors-origin", "www.poole.ch");

        for (var prefix : new String[]{"http", "https"}) {
            var connection = connect(baseUrl);
            connection.setRequestProperty("Origin", prefix + "://www.poole.ch");
            connection.connect();

            assertThat(connection.getHeaderField("Access-Control-Allow-Origin"))
                    .isEqualTo(prefix + "://www.poole.ch");
        }
    }

    /*
     * Test that the Access-Control-Allow-Origin header is set to the matching domain
     */
    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testCorsOriginIsSetToMatchingDomain(String baseUrl) throws Exception {
        startAPI("-cors-origin", "www.poole.ch,alt.poole.ch");

        String[] origins = {"http://www.poole.ch", "https://alt.poole.ch"};
        for (String origin : origins) {
            var connection = connect(baseUrl);
            connection.setRequestProperty("Origin", origin);
            connection.connect();

            assertThat(connection.getHeaderField("Access-Control-Allow-Origin"))
                    .isEqualTo(origin);
        }
    }

    /*
     * Test that the Access-Control-Allow-Origin header does not return mismatching origins
     */
    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testMismatchedCorsOriginsAreBlock(String baseUrl) throws Exception {
        startAPI("-cors-origin", "www.poole.ch,alt.poole.ch");

        var connection = connect(baseUrl);
        connection.setRequestProperty("Origin", "http://www.randomsite.com");
        connection.connect();

        assertThat(connection.getHeaderField("Access-Control-Allow-Origin"))
                .isNull();
    }
}
