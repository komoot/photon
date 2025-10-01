package de.komoot.photon.query;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.searcher.PhotonResult;
import de.komoot.photon.searcher.TagFilter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryFilterTagValueTest extends ESBaseTester {
    private static final List<Map.Entry<String, String>> TAGS = List.of(
            Map.entry("tourism", "attraction"),
            Map.entry("tourism", "hotel"),
            Map.entry("tourism", "museum"),
            Map.entry("tourism", "information"),
            Map.entry("amenity", "parking"),
            Map.entry("amenity", "restaurant"),
            Map.entry("amenity", "information"),
            Map.entry("food", "information"),
            Map.entry("railway", "station"));

    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws Exception {
        setUpES(dataDirectory);
        Importer instance = makeImporter();
        double lon = 13.38886;
        double lat = 52.51704;
        int i = 0;
        for (var entry : TAGS) {
            for (int j = 0; j < 2; ++j) {
                instance.add(List.of(new PhotonDoc()
                        .placeId(i).osmType("N").osmId(i).tagKey(entry.getKey()).tagValue(entry.getValue())
                        .centroid(makePoint(lon, lat))
                        .names(makeDocNames("name", "myPlace"))));
                lon += 0.00004;
                lat += 0.00006;
                ++i;
            }
        }
        instance.finish();
        refresh();
    }

    @AfterAll
    @Override
    public void tearDown() {
        super.tearDown();
    }

    private List<PhotonResult> searchWithTags(String[] params) {
        SimpleSearchRequest request = new SimpleSearchRequest();
        request.setQuery("myplace");
        request.setLimit(50, 50);
        for (String param : params) {
            request.addOsmTagFilter(TagFilter.buildOsmTagFilter(param));
        }

        return getServer().createSearchHandler(1).search(request);
    }

    private List<PhotonResult> reverseWithTags(String[] params) {
        ReverseRequest request = new ReverseRequest();
        request.setLocation(FACTORY.createPoint(new Coordinate(13.38886, 52.51704)));
        request.setLimit(50, 50);

        for (String param : params) {
            request.addOsmTagFilter(TagFilter.buildOsmTagFilter(param));
        }
        return getServer().createReverseHandler(1).search(request);
    }

    @ParameterizedTest
    @MethodSource("simpleTagFilterProvider")
    void testSearchSingleTagFilter(String filter, int expectedResults) {
        assertThat(searchWithTags(new String[]{filter})).hasSize(expectedResults);
    }

    @ParameterizedTest
    @MethodSource("simpleTagFilterProvider")
    void testReverseSingleTagFilter(String filter, int expectedResults) {
        assertThat(reverseWithTags(new String[]{filter})).hasSize(expectedResults);
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
    void testSearchCombinedTagFilter(String[] filters, int expectedResults) {
        assertThat(searchWithTags(filters)).hasSize(expectedResults);
    }

    @ParameterizedTest
    @MethodSource("combinedTagFilterProvider")
    void testReverseCombinedTagFilter(String[] filters, int expectedResults) {
        assertThat(reverseWithTags(filters)).hasSize(expectedResults);
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
