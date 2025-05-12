package de.komoot.photon.query;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.searcher.TagFilter;
import org.locationtech.jts.io.ParseException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryReverseFilterTagValueTest extends ESBaseTester {
    private static final String[] TAGS = new String[]{"tourism", "attraction",
                                                      "tourism", "hotel",
                                                      "tourism", "museum",
                                                      "tourism", "information",
                                                      "amenity", "parking",
                                                      "amenity", "restaurant",
                                                      "amenity", "information",
                                                      "food", "information",
                                                      "railway", "station"};

    @BeforeAll
    void setup(@TempDir Path dataDirectory) throws IOException, ParseException {
        setUpES(dataDirectory);
        Importer instance = makeImporter();
        double lon = 13.38886;
        double lat = 52.51704;
        for (int i = 0; i < TAGS.length; i++) {
            String key = TAGS[i];
            String value = TAGS[++i];
            PhotonDoc doc = this.createDoc(lon, lat, i, i, key, value);
            instance.add(List.of(doc));
            lon += 0.00004;
            lat += 0.00006;
            doc = this.createDoc(lon, lat, i + 1, i + 1, key, value);
            instance.add(List.of(doc));
            lon += 0.00004;
            lat += 0.00006;
        }
        instance.finish();
        refresh();
    }

    @AfterAll
    @Override
    public void tearDown() throws IOException {
        super.tearDown();
    }

    private List<PhotonResult> reverseWithTags(String[] params) {
        Point pt = FACTORY.createPoint(new Coordinate(13.38886, 52.51704));
        ReverseRequest request = new ReverseRequest(pt, "en", 1.0, "", 50, true, new HashSet<>(), false, false);
        for (String param : params) {
            request.addOsmTagFilter(TagFilter.buildOsmTagFilter(param));
        }
        return getServer().createReverseHandler(1).reverse(request);
    }

    @ParameterizedTest
    @MethodSource("simpleTagFilterProvider")
    void testSingleTagFilter(String filter, int expectedResults) {
        assertEquals(expectedResults, reverseWithTags(new String[]{filter}).size());
    }

    static Stream<Arguments> simpleTagFilterProvider() {
        return Stream.of(
                arguments("tourism:attraction", 2),
                arguments(":attraction", 2),
                arguments(":information", 6),
                arguments("tourism", 8),
                arguments("!tourism:attraction", 16),
                arguments(":!information", 12),
                arguments("!tourism", 10),
                arguments("tourism:!information", 6)
        );
    }

    @ParameterizedTest
    @MethodSource("combinedTagFilterProvider")
    void testCombinedTagFilter(String[] filters, int expectedResults) {
        assertEquals(expectedResults, reverseWithTags(filters).size());
    }

    static Stream<Arguments> combinedTagFilterProvider() {
        return Stream.of(
                arguments(new String[]{"food", "amenity"}, 8),
                arguments(new String[]{":parking", ":museum"}, 4),
                arguments(new String[]{"food", ":information"}, 6),
                arguments(new String[]{"!tourism", "!amenity"}, 4),
                arguments(new String[]{"tourism", "!amenity"}, 8),
                arguments(new String[]{":information", "!amenity"}, 4),
                arguments(new String[]{"tourism:!information", "food"}, 8),
                arguments(new String[]{"tourism:!information", "tourism:!hotel"}, 8),
                arguments(new String[]{"tourism", "!:information", "food"}, 6)
        );
    }
}
