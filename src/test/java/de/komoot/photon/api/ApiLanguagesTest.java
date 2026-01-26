package de.komoot.photon.api;

import de.komoot.photon.App;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.*;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

class ApiLanguagesTest extends ApiBaseTester {
    @TempDir
    private Path dataDirectory;

    @AfterEach
    void shutdown() {
        App.shutdown();
    }

    protected PhotonDoc createDoc(int id, String value, String... names) {
        return new PhotonDoc()
                .placeId(Integer.toString(id)).osmType("N").osmId(id).tagKey("place").tagValue(value)
                .centroid(makePoint(1.0, 2.34))
                .names(makeDocNames(names));
    }

    private void importPlaces(String... languages) throws Exception {
        getProperties().setLanguages(languages);
        setUpES(dataDirectory);
        Importer instance = makeImporter();
        instance.add(List.of(createDoc(1000, "city",
                "name:en", "thething", "name:fr", "letruc", "name:ch", "dasding")));
        instance.add(List.of(createDoc(1001, "town",
                "name:ch", "thething", "name:fr", "letruc", "name:en", "dasding")));
        instance.finish();
        refresh();
    }

    @Test
    void testOnlyImportSelectedLanguages() throws Exception {
        importPlaces("en");
        startAPI();

        assertThatJson(readURL("/api?q=thething")).isObject()
                .node("features").isArray()
                .hasSize(1)
                .element(0).isObject()
                .node("properties").isObject()
                .containsEntry("osm_id", 1000);

        assertThatJson(readURL("/api?q=letruc")).isObject()
                .node("features").isArray()
                .hasSize(0);
    }

    @Test
    void testUseImportLanguagesWhenNoOtherIsGiven() throws Exception {
        importPlaces("en", "fr", "ch");
        startAPI();

        assertThatJson(readURL("/api?q=thething")).isObject()
                .node("features").isArray()
                .hasSize(2);
    }
}
