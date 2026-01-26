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

import static org.assertj.core.api.Assertions.*;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

class QueryGeometryTest extends ESBaseTester {
    private int testDocId = 10000;

    @BeforeEach
    void setup(@TempDir Path dataDirectory) throws IOException {
        getProperties().setSupportGeometries(true);
        setUpES(dataDirectory);
    }

    private PhotonDoc createDoc(String geometry) {
        ++testDocId;
        return new PhotonDoc()
                .placeId(Integer.toString(testDocId)).osmType("N").osmId(testDocId).tagKey("place").tagValue("city")
                .names(makeDocNames("name", "Muffle Flu"))
                .geometry(makeDocGeometry(geometry))
                .centroid(makePoint(1.0, 2.34));
    }

    private List<PhotonResult> search() {
        final var request = new SimpleSearchRequest();
        request.setQuery("muffle flu");

        return getServer().createSearchHandler(1).search(request);
    }


    @Test
    void testSearchGetPolygon()  {
        Importer instance = makeImporter();
        instance.add(List.of(createDoc("POLYGON ((30 10, 40 40, 20 40, 10 20, 30 10))")));
        instance.finish();
        refresh();

        assertThat(search())
                .element(0)
                .satisfies(p ->
                        assertThatJson(p.getGeometry()).isObject()
                                .containsEntry("type", "Polygon"));
    }

    @Test
    void testSearchGetLineString()  {
        Importer instance = makeImporter();
        instance.add(List.of(createDoc("LINESTRING (30 10, 10 30, 40 40)")));
        instance.finish();
        refresh();

        assertThat(search())
                .element(0)
                .satisfies(p ->
                        assertThatJson(p.getGeometry()).isObject()
                                .containsEntry("type", "LineString"));
    }
}
