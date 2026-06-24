package de.komoot.photon.opensearch;

import de.komoot.photon.PhotonDoc;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the bulk-flush byte estimate. The estimate is what keeps a -full-geometry
 * import from building a bulk request larger than OpenSearch's http.max_content_length, so the
 * key property is that it grows with the number of geometry points.
 */
class ImporterEstimateSizeTest {

    private static PhotonDoc doc() {
        return new PhotonDoc("1", "N", 1, "place", "city");
    }

    @Test
    void geometrylessDocIsEstimatedAtTheFlatBase() {
        assertThat(Importer.estimateSize(doc())).isEqualTo(Importer.BASE_DOC_BYTES);
    }

    @Test
    void pointGeometryCountsAsOnePoint() throws ParseException {
        PhotonDoc doc = doc().geometry(new WKTReader().read("POINT (6.1 51.2)"));

        assertThat(Importer.estimateSize(doc))
                .isEqualTo(Importer.BASE_DOC_BYTES + Importer.BYTES_PER_GEOMETRY_POINT);
    }

    @Test
    void estimateGrowsWithGeometryPointCount() throws ParseException {
        // A closed triangle ring has 4 points (the first is repeated as the last).
        PhotonDoc doc = doc().geometry(new WKTReader().read(
                "POLYGON ((6.1 51.2, 6.2 51.2, 6.2 51.3, 6.1 51.2))"));

        assertThat(Importer.estimateSize(doc))
                .isEqualTo(Importer.BASE_DOC_BYTES + 4L * Importer.BYTES_PER_GEOMETRY_POINT);
    }
}
