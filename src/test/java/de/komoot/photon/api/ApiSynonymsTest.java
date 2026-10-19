package de.komoot.photon.api;

import de.komoot.photon.App;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.nominatim.model.AddressType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.core.JsonEncoding;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.*;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApiSynonymsTest extends ApiBaseTester {

    private static final Date TEST_DATE = new Date();

    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws Exception {
        getProperties().setSupportGeometries(true);
        getProperties().setExtraTags(List.of("ALL"));
        getProperties().setImportDate(TEST_DATE);
        setUpES(dataDirectory);
        Importer instance = makeImporter();
        instance.add(List.of(new PhotonDoc()
                .placeId("1000").osmType("N").osmId(1000).tagKey("railway").tagValue("station")
                .importance(0.2).addressType(AddressType.HOUSE)
                .categories(List.of("osm.railway.station"))
                .centroid(makePoint(13.38886, 52.51704))
                .geometry(makeDocGeometry("POINT(13.38886 52.51704)"))
                .names(makeDocNames("name", "Hauptbahnhof Berlin"))
        ));
        instance.add(List.of(new PhotonDoc()
                .placeId("2000").osmType("N").osmId(1000).tagKey("place").tagValue("city")
                .categories(List.of("osm.place.city"))
                .importance(0.6).addressType(AddressType.CITY)
                .centroid(makePoint(13.38886, 52.51704))
                .geometry(makeDocGeometry("POINT(13.38886 52.51704)"))
                .names(makeDocNames("name", "Berlin"))
        ));

        instance.finish();
        refresh();
    }

    @AfterAll
    @Override
    public void tearDown() {
        shutdownES();
    }

    private void cfgSynonyms(Path synFile, String... synonyms) {
        try (var writer = JsonMapper.shared().createGenerator(synFile.toFile(), JsonEncoding.UTF8)) {
            writer.writeStartObject();
            writer.writePOJOProperty("search_synonyms", synonyms);
            writer.writeEndObject();
        }
    }

    private void cfgCategories(Path synFile, String... terms) {
        try (var writer = JsonMapper.shared().createGenerator(synFile.toFile(), JsonEncoding.UTF8)) {
            writer.writeStartObject();
            writer.writeArrayPropertyStart("classification_terms");
            for (var term: terms) {
                var parts = term.split(":");
                writer.writeStartObject();
                writer.writeStringProperty("key", parts[0]);
                writer.writeStringProperty("value", parts[1]);
                writer.writePOJOProperty("terms", parts[2].split(","));
                writer.writeEndObject();
            }
            writer.writeEndArray();
            writer.writeEndObject();
        }
    }

    @Test
    void testSimpleSynonym(@TempDir Path dataDir) throws Exception {
        var synonymFile = dataDir.resolve("synonyms.json");
        cfgSynonyms(synonymFile, "hauptbahnhof,hbf");

        startAPI("-synonym-file", synonymFile.toString());

        assertThatJson(readURL("/api?q=berlin hbf")).isObject()
                .node("features").isArray().hasSize(1);

        App.shutdown();
    }

    @Test
    void testSynonymAreNotUsedInOneWordQueries(@TempDir Path dataDir) throws Exception {
        var synonymFile = dataDir.resolve("synonyms.json");
        cfgSynonyms(synonymFile, "hauptbahnhof,hbf");

        startAPI("-synonym-file", synonymFile.toString());

        assertThatJson(readURL("/api?q=Hauptbahnhof")).isObject()
                .node("features").isArray().hasSize(1);

        assertThatJson(readURL("/api?q=Hbf")).isObject()
                .node("features").isArray().hasSize(0);

        App.shutdown();
    }

    @Test
    void testDisallowSpacesInSynonyms(@TempDir Path dataDir) {
        var synonymFile = dataDir.resolve("synonyms.json");
        cfgSynonyms(synonymFile, "berlin,haupt stadt");

        assertThatException()
                .isThrownBy(() -> startAPI("-synonym-file", synonymFile.toString()))
                .withMessageContaining("Terms must not contain spaces");
    }

    @Test
    void testSimpleCategory(@TempDir Path dataDir) throws Exception {
        var synonymFile = dataDir.resolve("synonyms.json");
        cfgCategories(synonymFile, "railway:station:station");

        startAPI("-synonym-file", synonymFile.toString());

        assertThatJson(readURL("/api?q=Berlin Station")).isObject()
                .node("features").isArray().hasSize(1)
                .element(0).isObject()
                .node("properties").isObject()
                .containsEntry("osm_key", "railway");

        App.shutdown();
    }

    @Test
    void testCategoriesAreNotUsedInSingleWordQueries(@TempDir Path dataDir) throws Exception {
        var synonymFile = dataDir.resolve("synonyms.json");
        cfgCategories(synonymFile, "railway:station:station");

        startAPI("-synonym-file", synonymFile.toString());

        assertThatJson(readURL("/api?q=Station")).isObject()
                .node("features").isArray().hasSize(0);

        App.shutdown();
    }
}
