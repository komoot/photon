package de.komoot.photon.query;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QueryCategoryFilterTest extends ESBaseTester {

    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws Exception {
        setUpES(dataDirectory);
        Importer instance = makeImporter();

        instance.add(makePlace(1,
                "osm.tourism.hotel",
                "accomodation.guest_house"
        ));
        instance.add(makePlace(2,
                "osm.tourism.hotel",
                "accomodation.hostel"
        ));
        instance.add(makePlace(3,
                "osm.tourism.camping",
                "accomodation.tent",
                "accomodation.lodge"
        ));
        instance.add(makePlace(4,
                "osm.amenity.playground",
                "grade.A1"
        ));

        instance.finish();
        refresh();
    }

    @AfterAll
    @Override
    public void tearDown() {
        super.tearDown();
    }


    private List<PhotonDoc> makePlace(int id, String... categories) {
        return List.of(new PhotonDoc()
                .osmType("N").osmId(100 + id)
                .names(makeDocNames("name", "Foobar"))
                .houseNumber(Integer.toString(id))
                .categories(Arrays.stream(categories).collect(Collectors.toList()))
                .centroid(makePoint(0.1, 45.45))
        );
    }

    @ParameterizedTest
    @MethodSource("filterProvider")
    void testCategoryFilters(List<String> filters, List<Integer> ids) {
        SimpleSearchRequest request = new SimpleSearchRequest();
        request.setQuery("foobar");
        request.addIncludeCategories(filters.stream()
                .filter(s -> s.startsWith("+"))
                .map(s -> s.substring(1))
                .collect(Collectors.toList())
        );
        request.addExcludeCategories(filters.stream()
                .filter(s -> s.startsWith("-"))
                .map(s -> s.substring(1))
                .collect(Collectors.toList())
        );
        request.setLimit(100, 100);

        final var results =  getServer().createSearchHandler(1).search(request);

        assertThat(results)
                .extracting(p -> p.get("housenumber"))
                .containsExactlyInAnyOrder(ids.stream()
                        .map(i -> Integer.toString(i))
                        .toArray()
                );
    }

    static Stream<Arguments> filterProvider() {
        return Stream.of(
                arguments(List.of(),
                        List.of(1, 2, 3, 4)),
                arguments(List.of("+osm.tourism"),
                        List.of(1, 2, 3)),
                arguments(List.of("+osm.tourism.camping"),
                        List.of(3)),
                arguments(List.of("+accomodation.hostel,osm.amenity.playground"),
                        List.of(2, 4)),
                arguments(List.of("+grade.A1"),
                        List.of(4)),
                arguments(List.of("+grade.a1"),
                        List.of()),
                arguments(List.of("+osm.tourism", "+accomodation.tent,accomodation.guest_house"),
                        List.of(1, 3)),
                arguments(List.of("-osm.tourism"),
                        List.of(4)),
                arguments(List.of("-osm.tourism,accomodation.tent"),
                        List.of(1, 2, 4)),
                arguments(List.of("-osm.amenity.playground", "-accomodation.guest_house"),
                        List.of(2, 3)),
                arguments(List.of("+osm.tourism", "-osm.tourism.hotel"),
                        List.of(3))
        );
    }


}
