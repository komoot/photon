package de.komoot.photon.json;

import de.komoot.photon.DatabaseProperties;
import de.komoot.photon.ReflectionTestUtil;
import de.komoot.photon.nominatim.ImportThread;
import de.komoot.photon.nominatim.NominatimConnector;
import de.komoot.photon.nominatim.NominatimImporter;
import de.komoot.photon.nominatim.model.NameMap;
import de.komoot.photon.nominatim.testdb.H2DataAdapter;
import de.komoot.photon.nominatim.testdb.OsmlineTestRow;
import de.komoot.photon.nominatim.testdb.PlacexTestRow;
import net.javacrumbs.jsonunit.assertj.JsonMapAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_FIELDS;
import static org.assertj.core.api.Assertions.*;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

class JsonDumperTest {
    private final PrintStream stdout = System.out;
    private JdbcTemplate jdbc;
    private TransactionTemplate txTemplate;

    private String[] configLanguages = new String[]{"en", "de"};
    private List<String> configExtraTags = List.of();
    private String[] configCountries = null;
    private boolean configGeometryColumn = false;

    @BeforeEach
    void setup() {
        EmbeddedDatabase db = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .addScript("/test-schema.sql")
                .build();

        jdbc = new JdbcTemplate(db);
        txTemplate = new TransactionTemplate(new DataSourceTransactionManager(db));
    }

    private List<String> readEntireDatabase() throws IOException {
        final var dbProps = new DatabaseProperties();
        dbProps.setLanguages(configLanguages);
        dbProps.setExtraTags(configExtraTags);
        dbProps.setSupportGeometries(configGeometryColumn);

        NominatimImporter connector = new NominatimImporter(null, 0, null, null, null, new H2DataAdapter(), dbProps);
        ReflectionTestUtil.setFieldValue(connector, NominatimConnector.class, "template", jdbc);
        ReflectionTestUtil.setFieldValue(connector, NominatimConnector.class, "txTemplate", txTemplate);

        final var outCapture = new ByteArrayOutputStream();

        System.setOut(new PrintStream(outCapture));
        try {
            JsonDumper dumper = new JsonDumper("-", dbProps);
            final var importThread = new ImportThread(dumper);
            try {
                for (var country: configCountries == null ? connector.getCountriesFromDatabase() : configCountries) {
                    connector.readCountry(country, importThread);
                }
            } finally {
                importThread.finish();
            }
        } finally {
            System.setOut(stdout);
        }

        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(outCapture.toByteArray())))
                        .lines().collect(Collectors.toList());
    }

    private String filterByPlaceId(List<String> results, long placeID) {
        return results.stream().filter(s -> s.contains(String.format("\"place_id\":%d,", placeID))).findFirst().orElse(null);
    }

    private JsonMapAssert assertThatPlaceDocument(List<String> results, long placeID) {
        return assertThatJson(filterByPlaceId(results, placeID))
                .withTolerance(0.000001)
                .isObject()
                .containsEntry("type", NominatimPlaceDocument.DOCUMENT_TYPE)
                .node("content").isArray().hasSize(1).element(0).isObject();
    }


    @Test
    void testSimpleObjectDump() throws IOException {
        PlacexTestRow place = new PlacexTestRow("highway", "residential")
                .osm("W", 3452765)
                .rankSearch(27)
                .rankAddress(26)
                .importance(0.123)
                .country("hu")
                .name("Spot")
                .name("ref", "34")
                .name("name:fi", "Spott")
                .name("name:de", "Sport")
                .name("odd_name", "Dot")
                .add(jdbc);

        var results = readEntireDatabase();

        assertThat(results).hasSize(1);

        assertThatPlaceDocument(results, place.getPlaceId())
                .containsEntry("place_id", place.getPlaceId())
                .containsEntry("object_type", "W")
                .containsEntry("object_id", 3452765)
                .containsEntry("categories", List.of("osm.highway.residential"))
                .containsEntry("rank_address", 26)
                .containsEntry("importance", 0.123)
                .containsEntry("country_code", "hu")
                .doesNotContainKeys("parent_place_id", "postcode", "extra")
                .containsEntry("name", Map.of(
                        "name", "Spot",
                        "name:de", "Sport"));
    }

    @Test
    void testPlaceAddress() throws IOException {
        PlacexTestRow place = PlacexTestRow.make_street("Burg").postcode("55771").add(jdbc);

        place.addAddresslines(jdbc,
                new PlacexTestRow("place", "neighbourhood").name("Le Coin").ranks(24).add(jdbc),
                new PlacexTestRow("place", "suburb").name("Crampton").ranks(20).add(jdbc),
                new PlacexTestRow("place", "city").name("Grand Junction").ranks(16).add(jdbc),
                new PlacexTestRow("place", "county").name("Lost County").ranks(12).add(jdbc),
                new PlacexTestRow("place", "state").name("Le Havre").ranks(8).add(jdbc));

        var results = readEntireDatabase();

        assertThat(results).hasSize(6);

        assertThatPlaceDocument(results, place.getPlaceId())
                .containsEntry("name", Map.of("name", "Burg"))
                .containsEntry("postcode", "55771")
                .doesNotContainKeys("admin_level")
                .containsEntry("address", Map.of(
                        "state", "Le Havre",
                        "county", "Lost County",
                        "city", "Grand Junction",
                        "suburb", "Crampton",
                        "neighbourhood", "Le Coin"
                ));

    }

    @Test
    void testInterpolationExport() throws IOException {
        PlacexTestRow street = PlacexTestRow.make_street("La strada").add(jdbc);

        OsmlineTestRow osmline =
                new OsmlineTestRow().number(45, 46, 1).parent(street).geom("LINESTRING(0 0, 0 1)").add(jdbc);

        var results = readEntireDatabase();

        assertThat(results).hasSize(2);

        assertThatJson(filterByPlaceId(results, osmline.getPlaceId()))
                .when(IGNORING_EXTRA_FIELDS)
                .isObject()
                .containsEntry("type", NominatimPlaceDocument.DOCUMENT_TYPE)
                .node("content")
                .isEqualTo(List.of(
                        Map.of("place_id", osmline.getPlaceId(), "housenumber", "45"),
                        Map.of("place_id", osmline.getPlaceId(), "housenumber", "46")
                ));
    }

    @Test
    void testExportOfContext() throws IOException{
        PlacexTestRow place = PlacexTestRow.make_street("Main Street").add(jdbc);

        place.addAddresslines(jdbc,
                new PlacexTestRow("place", "municipality")
                        .name("Gemeinde")
                        .name("name:en", "Ville")
                        .name("name:es", "Lugar")
                        .ranks(14).add(jdbc),
                new PlacexTestRow("place", "village").name("Dorf").ranks(16).add(jdbc)
        );

        var results = readEntireDatabase();

        assertThat(results).hasSize(3);

        assertThatPlaceDocument(results, place.getPlaceId())
                .containsEntry("address", Map.of(
                        "city", "Dorf",
                        "other1", "Gemeinde",
                        "other1:en", "Ville"
                ));
    }

    @Test
    void testExportForSelectedCountries() throws IOException {
        PlacexTestRow place = new PlacexTestRow("amenity", "cafe").name("SpotHU").country("hu").add(jdbc);
        new PlacexTestRow("amenity", "cafe").name("SpotDE").country("de").add(jdbc);
        new PlacexTestRow("amenity", "cafe").name("SpotUS").country("us").add(jdbc);

        configCountries = new String[]{"uk", "hu", "nl"};
        var results = readEntireDatabase();

        assertThat(results).hasSize(1);

        assertThatPlaceDocument(results, place.getPlaceId())
                .containsEntry("place_id", place.getPlaceId());
    }


    @Test
    void testGeometryAsCentroid() throws IOException {
        var place = new PlacexTestRow("highway", "primary")
                .name("North Road")
                .centroid(10.1, -3.45)
                .geometry("LINESTRING(10.1 -3.4, 10.1 -3.5)")
                .add(jdbc);

        var results = readEntireDatabase();

        assertThat(results).hasSize(1);

        assertThatPlaceDocument(results, place.getPlaceId())
                .doesNotContainKey("geometry")
                .node("centroid").isEqualTo("[10.1, -3.45]");

        assertThatPlaceDocument(results, place.getPlaceId())
                .node("bbox").isEqualTo("[10.1,-3.4,10.1,-3.5]");
    }

    @Test
    void testGeometryAsGeometry() throws IOException {
        var place = new PlacexTestRow("highway", "primary")
                .name("North Road")
                .centroid(10.1, -3.45)
                .geometry("LINESTRING(10.1 -3.4, 10.1 -3.5)")
                .add(jdbc);

        configGeometryColumn = true;
        var results = readEntireDatabase();

        assertThat(results).hasSize(1);

        assertThatPlaceDocument(results, place.getPlaceId())
                .node("centroid").isEqualTo("[10.1, -3.45]");

        assertThatPlaceDocument(results, place.getPlaceId())
                .node("bbox").isEqualTo("[10.1,-3.4,10.1,-3.5]");

        assertThatPlaceDocument(results, place.getPlaceId())
                .node("geometry").isEqualTo("{\"type\":\"LineString\",\"coordinates\":[[10.1,-3.4],[10.1,-3.5]]}");
    }

    @Test
    void testExtraTagsDisabled() throws IOException {
        var place = new PlacexTestRow("highway", "primary")
                .name("North Road")
                .extraTag("access", "private")
                .extraTag("maxspeed", "120")
                .add(jdbc);

        var results = readEntireDatabase();

        assertThat(results).hasSize(1);

        assertThatPlaceDocument(results, place.getPlaceId()).doesNotContainKey("extra");
    }

    @Test
    void testExtraTagsAll() throws IOException {
        var place = new PlacexTestRow("highway", "primary")
                .name("North Road")
                .extraTag("access", "private")
                .extraTag("maxspeed", "120")
                .add(jdbc);

        configExtraTags = List.of("ALL");
        var results = readEntireDatabase();

        assertThat(results).hasSize(1);

        assertThatPlaceDocument(results, place.getPlaceId())
                .containsEntry("extra", Map.of("access", "private", "maxspeed", "120"));
    }

    @Test
    void testExtraTagsSome() throws IOException {
        var place = new PlacexTestRow("highway", "primary")
                .name("North Road")
                .extraTag("access", "private")
                .extraTag("maxspeed", "120")
                .add(jdbc);

        configExtraTags = List.of("maxspeed", "surface");
        var results = readEntireDatabase();

        assertThat(results).hasSize(1);

        assertThatPlaceDocument(results, place.getPlaceId())
                .containsEntry("extra", Map.of("maxspeed", "120"));
    }

    @Test
    void testHeaderOutput() throws IOException {
        final var outCapture = new ByteArrayOutputStream();

        System.setOut(new PrintStream(outCapture));
        try {
            JsonDumper dumper = new JsonDumper("-", new DatabaseProperties());
            dumper.writeHeader(Map.of(
                            "ch", NameMap.makeForPlace(Map.of(
                                    "name", "Schweiz/Suisse",
                                    "name:de", "Schweiz",
                                    "name:fr", "Suisse"), new String[]{"fr"}),
                            "", new NameMap()));
            dumper.finish();
        } finally {
            System.setOut(stdout);
        }

        var lines = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(outCapture.toByteArray())))
                .lines().collect(Collectors.toList());

        assertThat(lines).hasSize(2);

        assertThatJson(lines.get(0)).isObject()
                .containsEntry("type", "NominatimDumpFile")
                .node("content").isObject()
                .containsEntry("version", NominatimDumpHeader.EXPECTED_VERSION)
                .containsKeys("generator", "database_version", "data_timestamp", "features");

        assertThatJson(lines.get(1)).isObject()
                .containsEntry("type", "CountryInfo")
                .node("content").isArray().singleElement().isObject()
                .containsEntry("country_code", "ch")
                .containsEntry("name", Map.of("name", "Schweiz/Suisse", "name:fr", "Suisse"));

    }
}