package de.komoot.photon.query;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.nominatim.model.AddressType;
import de.komoot.photon.searcher.PhotonResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryFilterLayerTest extends ESBaseTester {
    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws Exception {
        setUpES(dataDirectory);
        Importer instance = makeImporter();

        int id = 0;

        AddressType[] docRanks = {AddressType.STATE, AddressType.CITY, AddressType.CITY, AddressType.LOCALITY};
        for (var rank : docRanks) {
            instance.add(List.of(new PhotonDoc()
                            .placeId(Integer.toString(id)).osmType("W").osmId(++id).tagKey("place").tagValue("value")
                            .names(makeDocNames("name", "berlin"))
                            .centroid(makePoint(10, 10))

                            .addressType(rank)));
        }

        instance.finish();
        refresh();
    }

    @AfterAll
    @Override
    public void tearDown() {
        super.tearDown();
    }

    private List<PhotonResult> searchWithLayers(String... layers) {
        SimpleSearchRequest request = new SimpleSearchRequest();
        request.setQuery("berlin");
        request.addLayerFilters(Arrays.stream(layers).collect(Collectors.toSet()));

        return getServer().createSearchHandler(1).search(request).toList();
    }

    private List<PhotonResult> reverse(String... layers) {
        ReverseRequest request = new ReverseRequest(FACTORY.createPoint(new Coordinate(10, 10)));
        request.setLimit(15, 15);
        request.addLayerFilters(Arrays.stream(layers).collect(Collectors.toSet()));

        return getServer().createReverseHandler(1).search(request).toList();
    }

    @Test
    void testSearchSingleLayer() {
        assertThat(searchWithLayers("city"))
                .hasSize(2)
                .allSatisfy(p -> assertThat(p.get("type")).isEqualTo("city"));
    }

    @Test
    void testSearchMultipleLayers() {
        assertThat(searchWithLayers("city", "locality"))
                .hasSize(3)
                .allSatisfy(p -> assertThat(p.get("type")).isNotEqualTo("state"));
    }

    @Test
    void testReverseSingleLayer() {
        assertThat(reverse("city"))
                .hasSize(2)
                .allSatisfy(p -> assertThat(p.get("type")).isEqualTo("city"));
    }

    @Test
    void testReverseMultipleLayers() {
        assertThat(reverse("city", "locality"))
                .hasSize(3)
                .allSatisfy(p -> assertThat(p.get("type")).isNotEqualTo("state"));
    }
}
