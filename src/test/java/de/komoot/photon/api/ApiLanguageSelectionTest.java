package de.komoot.photon.api;

import de.komoot.photon.Importer;
import net.javacrumbs.jsonunit.assertj.JsonMapAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

/**
 * Test selection of returned languages.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApiLanguageSelectionTest extends ApiBaseTester {
    private static final String[] BASE_URLS = {"/api?q=this", "/reverse?lat=2.0&lon=34.0"};

    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws Exception {
        getProperties().setLanguages(new String[]{"en", "de", "it"});
        setUpES(dataDirectory);
        Importer instance = makeImporter();

        instance.add(List.of(
                createDoc(34.0, 2.0, 1000, 23, "place", "city")
                        .names(Map.of(
                                "name", "this",
                                "name:en", "englishName",
                                "name:de", "germanName",
                                "name:it", "italianName"))
        ));

        instance.finish();
        refresh();
    }

    @AfterAll
    public void tearDown() throws IOException {
        shutdownES();
    }

    private JsonMapAssert firstResultProperties(String baseUrl, String langParam, String headerParam) throws Exception {
        var url = baseUrl;
        if (langParam != null) {
            url += "&lang=" + langParam;
        }

        final var connection = connect(url);
        if (headerParam != null) {
            connection.setRequestProperty("Accept-Language", headerParam);
        }

        final var json = new BufferedReader(new InputStreamReader(connection.getInputStream()))
                .lines()
                .collect(Collectors.joining("\n"));

        return assertThatJson(json).isObject()
                .node("features").isArray()
                .element(0).isObject()
                .node("properties").isObject();
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testDefaultLanguageUnconfigured(String baseUrl) throws Exception {
        startAPI();
        firstResultProperties(baseUrl,null, null)
                .containsEntry("name", "this");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testDefaultLanguageConfigured(String baseUrl) throws Exception {
        startAPI("-default-language", "it");
        firstResultProperties(baseUrl,null, null)
                .containsEntry("name", "italianName");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testLanguageByParameter(String baseUrl) throws Exception {
        startAPI();
        firstResultProperties(baseUrl,"de", "en")
                .containsEntry("name", "germanName");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testLanguageByParameterDefault(String baseUrl) throws Exception {
        startAPI("-default-language", "en");
        firstResultProperties(baseUrl,"default", "en")
                .containsEntry("name", "this");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testLanguageByParameterUnsupported(String baseUrl) throws Exception {
        startAPI();
        assertHttpError(baseUrl + "&lang=fr", 400);
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testLanguageByHeaderValid(String baseUrl) throws Exception {
        startAPI();

        firstResultProperties(baseUrl,null, "de")
                .containsEntry("name", "germanName");
        firstResultProperties(baseUrl,null, "de-DE")
                .containsEntry("name", "germanName");
        firstResultProperties(baseUrl,null, "ru,pl;q=0.9,sp,it;q=0.1")
                .containsEntry("name", "italianName");
        firstResultProperties(baseUrl,null, "ru,pl;q=0.9,sp,ch;q=0.1")
                .containsEntry("name", "this");
        firstResultProperties(baseUrl,null, "de;q=0.9,en,it;q=0.1")
                .containsEntry("name", "englishName");
    }

    @ParameterizedTest
    @FieldSource("BASE_URLS")
    void testLanguageByHeaderInvalid(String baseUrl) throws Exception {
        startAPI();

        firstResultProperties(baseUrl,null, "we loves cats")
                .containsEntry("name", "this");
        firstResultProperties(baseUrl,null, "cookies?")
                .containsEntry("name", "this");
        firstResultProperties(baseUrl,null, "Illegal/Header_")
                .containsEntry("name", "this");

    }
}
