package de.komoot.photon.opensearch;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.*;
import static de.komoot.photon.PhotonResultAssert.*;

class ImporterTest extends ESBaseTester {

    @BeforeEach
    public void setUp(@TempDir Path dataDirectory) throws IOException {
        getProperties().setSupportGeometries(true);
        setUpES(dataDirectory);
    }

    @Test
    void testAddSimpleDoc() throws ParseException {
        setupDocs(
                new PhotonDoc("1234", "N", 1000, "place", "city")
                        .geometry(new WKTReader().read("MULTIPOLYGON (((6.111933 51.2659309, 6.1119417 51.2659247, 6.1119554 51.2659249, 6.1119868 51.2659432, 6.111964 51.2659591, 6.1119333 51.2659391, 6.111933 51.2659309)))"))
                        .extraTags(Collections.singletonMap("maxspeed", "100")));

        assertThat(getById(1234)).isNotNull()
                .hasFieldValue(DocFields.OSM_TYPE, "N")
                .hasFieldValue(DocFields.OSM_ID, 1000)
                .hasFieldValue(DocFields.OSM_KEY, "place")
                .hasFieldValue(DocFields.OSM_VALUE, "city")
                .hasNoField(DocFields.EXTRA);
    }

    @Test
    void testAddHousenumberMultiDoc() {
        Importer instance = makeImporter();

        instance.add(List.of(
                new PhotonDoc("4432", "N", 100, "building", "yes").houseNumber("34"),
                new PhotonDoc("4432", "N", 100, "building", "yes").houseNumber("35")));
        instance.finish();

        assertThat(getById("4432")).isNotNull()
                .hasFieldValue(DocFields.OSM_TYPE, "N")
                .hasFieldValue(DocFields.OSM_ID, 100)
                .hasFieldValue(DocFields.OSM_KEY, "building")
                .hasFieldValue(DocFields.OSM_VALUE, "yes")
                .hasFieldValue(DocFields.HOUSENUMBER, "34");

        assertThat(getById("4432.1")).isNotNull()
                .hasFieldValue(DocFields.OSM_TYPE, "N")
                .hasFieldValue(DocFields.OSM_ID, 100)
                .hasFieldValue(DocFields.OSM_KEY, "building")
                .hasFieldValue(DocFields.OSM_VALUE, "yes")
                .hasFieldValue(DocFields.HOUSENUMBER, "35");
    }

    @Test
    void testSelectedExtraTagsCanBeIncluded() {
        getProperties().setExtraTags(List.of("maxspeed", "website"));

        setupDocs(
                new PhotonDoc("1234", "N", 1000, "place", "city")
                        .extraTags(Map.of(
                                "website", "foo",
                                "maxspeed", 100,
                                "source", List.of("survey", "aerial")
                        )),
                new PhotonDoc("1235", "N", 1001, "place", "city")
                        .extraTags(Map.of("wikidata", "100"))
        );

        assertThat(getById(1234)).isNotNull()
                .hasFieldValue(DocFields.EXTRA, Map.of("maxspeed", 100, "website", "foo"));

        assertThat(getById(1235)).isNotNull()
                        .hasNoField(DocFields.EXTRA);
    }

    @Test
    void testUsingPlaceIdsTwice() {
        Importer instance = makeImporter();

        instance.add(List.of(
                new PhotonDoc("113344", "N", 1, "place", "yes")
        ));
        // Photon will bail out on an existing place ID.
        instance.add(List.of(
                new PhotonDoc("113344", "N", 2, "place", "yes")
        ));

        assertThatRuntimeException()
                .isThrownBy(instance::finish);
    }

    @Test
    void testImportWithoutPlaceId() {
        Importer instance = makeImporter();

        for (long i = 0; i < 10; ++i) {
            instance.add(List.of(new PhotonDoc().osmType("XXR").osmId(i).houseNumber(Long.toString(i))));
        }
        instance.finish();

        assertThat(getAll())
                .extracting(p -> Objects.requireNonNull(p.get(DocFields.OSM_ID)))
                .containsExactlyInAnyOrder(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }
}
