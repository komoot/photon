package de.komoot.photon.api;

import de.komoot.photon.App;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

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

    protected PhotonDoc createDoc(int id, String key, String value, String... names) {
        Point location = FACTORY.createPoint(new Coordinate(1.0, 2.34));
        PhotonDoc doc = new PhotonDoc(id, "W", id, key, value).centroid(location);

        Map<String, String> nameMap = new HashMap<>();

        for (int i = 0; i < names.length - 1; i += 2) {
            nameMap.put(names[i], names[i + 1]);
        }

        doc.names(nameMap);

        return doc;
    }

    private void importPlaces(String... languages) throws Exception {
        getProperties().setLanguages(languages);
        setUpES(dataDirectory);
        Importer instance = makeImporter();
        instance.add(List.of(createDoc(1000, "place", "city",
                "name:en", "thething", "name:fr", "letruc", "name:ch", "dasding")));
        instance.add(List.of(createDoc(1001, "place", "town",
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

    @Test
    void testUseCommandLineLanguages() throws Exception {
        importPlaces("en", "fr", "ch");
        startAPI("-languages", "en,fr");

        assertThatJson(readURL("/api?q=thething")).isObject()
                .node("features").isArray()
                .hasSize(1);
    }
}
