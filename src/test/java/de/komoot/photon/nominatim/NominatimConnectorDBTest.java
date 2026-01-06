package de.komoot.photon.nominatim;

import de.komoot.photon.DatabaseProperties;
import de.komoot.photon.config.PostgresqlConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import de.komoot.photon.ReflectionTestUtil;
import de.komoot.photon.nominatim.model.AddressType;
import de.komoot.photon.nominatim.testdb.CollectingImporter;
import de.komoot.photon.nominatim.testdb.H2DataAdapter;
import de.komoot.photon.nominatim.testdb.OsmlineTestRow;
import de.komoot.photon.nominatim.testdb.PlacexTestRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.*;

class NominatimConnectorDBTest {
    private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
    private NominatimImporter connector;
    private CollectingImporter importer;
    private JdbcTemplate jdbc;

    private boolean supportGeometries = false;

    private static final Map<String, String> COUNTRY_NAMES = Map.of("default", "USA", "en", "United States");

    @BeforeEach
    void setup() {
        EmbeddedDatabase db = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .addScript("/test-schema.sql")
                .build();

        jdbc = new JdbcTemplate(db);
    }

    protected Point makePoint(double x, double y) {
        return FACTORY.createPoint(new Coordinate(x, y));
    }

    private void setupImporter() {
        DatabaseProperties dbProperties = new DatabaseProperties();
        dbProperties.setSupportGeometries(supportGeometries);
        dbProperties.setLanguages(new String[]{"en", "de", "fr"});
        connector = new NominatimImporter(new PostgresqlConfig(), new H2DataAdapter(), dbProperties);
        importer = new CollectingImporter();

        assert(jdbc.getDataSource() != null);
        TransactionTemplate txTemplate = new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource()));
        ReflectionTestUtil.setFieldValue(connector, NominatimConnector.class, "template", jdbc);
        ReflectionTestUtil.setFieldValue(connector, NominatimConnector.class, "txTemplate", txTemplate);
    }

    private void readEntireDatabase() {
        setupImporter();

        ImportThread importThread = new ImportThread(importer);
        try {
            for (var country: connector.getCountriesFromDatabase()) {
                connector.readCountry(country, importThread);
            }
        } finally {
            importThread.finish();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"cafe", "45", "good_cafe", "3-a-b"})
    void testSimpleNodeImport(String value) {
        PlacexTestRow place = new PlacexTestRow("amenity", value).name("Spot").add(jdbc);
        readEntireDatabase();

        assertThat(importer).singleElement()
                .satisfies(place::assertEquals)
                .hasFieldOrPropertyWithValue("geometry", null)
                .hasFieldOrPropertyWithValue("categories", Set.of("osm.amenity." + value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a b", "er:45", "@#", ""})
    void testImportBadTagValue(String value) {
        new PlacexTestRow("amenity", value).name("Spot").add(jdbc);
        readEntireDatabase();

        assertThat(importer).singleElement()
                .hasFieldOrPropertyWithValue("tagKey", "amenity")
                .hasFieldOrPropertyWithValue("tagValue", "yes")
                .hasFieldOrPropertyWithValue("categories", Set.of("osm.amenity.yes"));
    }

    @Test
    void testImportForSelectedCountries() {
        PlacexTestRow place = new PlacexTestRow("amenity", "cafe").name("SpotHU").country("hu").add(jdbc);
        new PlacexTestRow("amenity", "cafe").name("SpotDE").country("de").add(jdbc);
        new PlacexTestRow("amenity", "cafe").name("SpotUS").country("us").add(jdbc);

        setupImporter();

        ImportThread importThread = new ImportThread(importer);
        try {
            connector.readCountry("uk", importThread);
            connector.readCountry("hu", importThread);
            connector.readCountry("nl", importThread);
        } finally {
            importThread.finish();
        }

        assertThat(importer.getFinishCalled()).isEqualTo(1);
        assertThat(importer).singleElement().satisfies(place::assertEquals);
    }

    @Test
    void testImportance() {
        PlacexTestRow place1 = new PlacexTestRow("amenity", "cafe").name("Spot").rankSearch(10).add(jdbc);
        PlacexTestRow place2 = new PlacexTestRow("amenity", "cafe").name("Spot").importance(0.3).add(jdbc);

        readEntireDatabase();

        importer.assertThatByRow(place1).hasFieldOrPropertyWithValue("importance", 0.5);
        importer.assertThatByRow(place2).hasFieldOrPropertyWithValue("importance", 0.3);
    }

    @Test
    void testPlaceAddress() {
        PlacexTestRow place = PlacexTestRow.make_street("Burg").add(jdbc);

        place.addAddresslines(jdbc,
                new PlacexTestRow("place", "neighbourhood").name("Le Coin").ranks(24).add(jdbc),
                new PlacexTestRow("place", "suburb").name("Crampton").ranks(20).add(jdbc),
                new PlacexTestRow("place", "city").name("Grand Junction").ranks(16).add(jdbc),
                new PlacexTestRow("place", "county").name("Lost County").ranks(12).add(jdbc),
                new PlacexTestRow("place", "state").name("Le Havre").ranks(8).add(jdbc));

        readEntireDatabase();

        importer.assertThatByRow(place)
                .hasFieldOrPropertyWithValue("addressParts", Map.of(
                        AddressType.LOCALITY, Map.of("default", "Le Coin"),
                        AddressType.DISTRICT, Map.of("default", "Crampton"),
                        AddressType.CITY, Map.of("default", "Grand Junction"),
                        AddressType.COUNTY, Map.of("default", "Lost County"),
                        AddressType.STATE, Map.of("default", "Le Havre"),
                        AddressType.COUNTRY, COUNTRY_NAMES));
    }

    @Test
    void testPlaceAddressAddressRank0() {
        PlacexTestRow place = new PlacexTestRow("natural", "water").name("Lake Tee").rankAddress(0).rankSearch(20).add(jdbc);

        place.addAddresslines(jdbc,
                new PlacexTestRow("place", "county").name("Lost County").ranks(12).add(jdbc),
                new PlacexTestRow("place", "state").name("Le Havre").ranks(8).add(jdbc));

        readEntireDatabase();

        importer.assertThatByRow(place)
                .hasFieldOrPropertyWithValue("addressParts", Map.of(
                        AddressType.COUNTY, Map.of("default", "Lost County"),
                        AddressType.STATE, Map.of("default", "Le Havre"),
                        AddressType.COUNTRY, COUNTRY_NAMES));
    }

    @Test
    void testPlaceAddressLineWithNoUsableName() {
        PlacexTestRow place = new PlacexTestRow("natural", "water").name("Lake Tee").rankAddress(0).rankSearch(20).add(jdbc);

        place.addAddresslines(jdbc,
                new PlacexTestRow("place", "county").name("name:zh", "Lost County").ranks(12).add(jdbc),
                new PlacexTestRow("place", "state").name("Le Havre").ranks(8).add(jdbc));

        readEntireDatabase();

        importer.assertThatByRow(place)
                .hasFieldOrPropertyWithValue("addressParts", Map.of(
                        AddressType.STATE, Map.of("default", "Le Havre"),
                        AddressType.COUNTRY, COUNTRY_NAMES));
    }

    @Test
    void testPoiAddress() {
        PlacexTestRow parent = PlacexTestRow.make_street("Burg").add(jdbc);

        parent.addAddresslines(jdbc,
                new PlacexTestRow("place", "city").name("Grand Junction").ranks(16).add(jdbc));

        PlacexTestRow place = new PlacexTestRow("place", "house").name("House").parent(parent).add(jdbc);

        readEntireDatabase();

        importer.assertThatByRow(place)
                .hasFieldOrPropertyWithValue("addressParts", Map.of(
                        AddressType.STREET, Map.of("default", "Burg"),
                        AddressType.CITY, Map.of("default", "Grand Junction"),
                        AddressType.COUNTRY, COUNTRY_NAMES));
    }

    @Test
    void testInterpolationPoint() {
        PlacexTestRow street = PlacexTestRow.make_street("La strada").add(jdbc);

        OsmlineTestRow osmline =
                new OsmlineTestRow().number(45, 45, 1).parent(street).geom("POINT(45 23)").add(jdbc);

        readEntireDatabase();

        importer.assertThatAllByRow(osmline).singleElement()
                .hasFieldOrPropertyWithValue("houseNumber", "45")
                .hasFieldOrPropertyWithValue("centroid", makePoint(45.0, 23.0));
    }

    @Test
    void testInterpolationAny() {
        PlacexTestRow street = PlacexTestRow.make_street("La strada").add(jdbc);

        OsmlineTestRow osmline =
                new OsmlineTestRow().number(1, 11, 1).parent(street).geom("LINESTRING(0 0, 0 1)").add(jdbc);

        readEntireDatabase();

        var expect = importer.assertThatAllByRow(osmline);

        for (int i = 1; i <= 11; ++i) {
            final var idx = i;
            expect.withFailMessage("Entry for housenumber %s failed", idx)
                    .satisfies(d -> {
                        assertThatObject(d.getCentroid()).isEqualTo(makePoint(0, (idx - 1) * 0.1));
                        assertThat(d.getHouseNumber()).isEqualTo(String.valueOf(idx));
                    },
                    atIndex(idx - 1));
        }
    }

    @Test
    void testInterpolationWithSteps() {
        PlacexTestRow street = PlacexTestRow.make_street("La strada").add(jdbc);

        OsmlineTestRow osmline =
                new OsmlineTestRow().number(10, 20, 2).parent(street).geom("LINESTRING(0 0, 0 1)").add(jdbc);

        readEntireDatabase();

        var expect = importer.assertThatAllByRow(osmline);

        for (int i = 0; i < 6; ++i) {
            final var idx = i;
            expect.satisfies(d -> {
                        assertThatObject(d.getCentroid()).isEqualTo(makePoint(0, idx * 0.2));
                        assertThat(d.getHouseNumber()).isEqualTo(String.valueOf(10 + idx * 2));
                    },
                    atIndex(i));
        }
    }

    /**
     * When the address contains multiple address parts that map to 'city', then only the one with the
     * highest address rank is used. The others are moved to context.
     */
    @Test
    void testAddressMappingDuplicate() {
        PlacexTestRow place = PlacexTestRow.make_street("Main Street").add(jdbc);
        PlacexTestRow munip = new PlacexTestRow("place", "municipality").name("Gemeinde").ranks(14).add(jdbc);

        place.addAddresslines(jdbc,
                munip,
                new PlacexTestRow("place", "village").name("Dorf").ranks(16).add(jdbc));

        readEntireDatabase();

        importer.assertThatByRow(place)
                .hasFieldOrPropertyWithValue("addressParts", Map.of(
                        AddressType.CITY, Map.of("default", "Dorf"),
                        AddressType.COUNTRY, COUNTRY_NAMES
                ))
                .hasFieldOrPropertyWithValue("context", Map.of(
                        "default", Set.of("Gemeinde")
                ));
    }

    /**
     * When a city item has address parts that map to 'city' then these parts are moved to context.
     */
    @Test
    void testAddressMappingAvoidSameTypeAsPlace() {
        PlacexTestRow village = new PlacexTestRow("place", "village").name("Dorf").ranks(16).add(jdbc);
        PlacexTestRow munip = new PlacexTestRow("place", "municipality").name("Gemeinde").ranks(14).add(jdbc);

        village.addAddresslines(jdbc, munip);

        readEntireDatabase();

        importer.assertThatByRow(village)
                .hasFieldOrPropertyWithValue("addressParts", Map.of(AddressType.COUNTRY, COUNTRY_NAMES))
                .hasFieldOrPropertyWithValue("context", Map.of(
                        "default", Set.of("Gemeinde")
                ));
    }

    /**
     * Unnamed objects are imported when they have a housenumber.
     */
    @Test
    void testUnnamedObjectWithHousenumberAndStreetAddress() {
        PlacexTestRow parent = PlacexTestRow.make_street("Main St").add(jdbc);
        PlacexTestRow place = new PlacexTestRow("building", "yes")
                .addr("housenumber", "123")
                .addr("street", "North Main St")
                .parent(parent).add(jdbc);

        readEntireDatabase();

        importer.assertThatByRow(place)
                .hasFieldOrPropertyWithValue("houseNumber", "123")
                .hasFieldOrPropertyWithValue("addressParts", Map.of(
                        AddressType.STREET, Map.of("default", "North Main St"),
                        AddressType.COUNTRY, COUNTRY_NAMES
                ));
    }

    /**
     * Semicolon-separated housenumber lists result in multiple iport objects.
     */
    @Test
    void testObjectWithHousenumberList() {
        PlacexTestRow parent = PlacexTestRow.make_street("Main St").add(jdbc);
        PlacexTestRow place = new PlacexTestRow("building", "yes").addr("housenumber", "1;2a;3").parent(parent).add(jdbc);

        readEntireDatabase();

        importer.assertThatAllByRow(place)
                        .allSatisfy(d -> assertThat(d.getAddressParts())
                                .containsEntry(AddressType.STREET, Map.of("default", "Main St")))
                .extracting("houseNumber")
                .containsExactlyInAnyOrder("1", "2a", "3");
    }

    /**
     * streetnumbers and conscription numbers are recognised.
     */
    @Test
    void testObjectWithConscriptionNumber() {
        PlacexTestRow parent = PlacexTestRow.make_street("Main St").add(jdbc);
        PlacexTestRow place = new PlacexTestRow("building", "yes")
                .addr("streetnumber", "34")
                .addr("conscriptionnumber", "99521")
                .addr("place", "Village")
                .addr("street", "Main St")
                .parent(parent).add(jdbc);

        place.addAddresslines(jdbc,
                parent,
                new PlacexTestRow("place", "city").name("Grand Central").ranks(16).add(jdbc));

        readEntireDatabase();

        importer.assertThatAllByRow(place)
                .anySatisfy(d -> assertThat(d)
                        .hasFieldOrPropertyWithValue("houseNumber", "34")
                        .hasFieldOrPropertyWithValue("addressParts", Map.of(
                                AddressType.STREET, Map.of("default", "Main St"),
                                AddressType.CITY, Map.of("default", "Grand Central"),
                                AddressType.COUNTRY, COUNTRY_NAMES
                        )))
                .anySatisfy(d -> assertThat(d)
                        .hasFieldOrPropertyWithValue("houseNumber", "99521")
                        .hasFieldOrPropertyWithValue("addressParts", Map.of(
                                AddressType.STREET, Map.of("default", "Village"),
                                AddressType.CITY, Map.of("default", "Grand Central"),
                                AddressType.COUNTRY, COUNTRY_NAMES
                        )));
    }
    /**
     * Unnamed objects are ignored when they do not have a housenumber.
     */
    @Test
    void testUnnamedObjectWithOutHousenumber() {
        var parent = PlacexTestRow.make_street("Main St").add(jdbc);
        var place = new PlacexTestRow("building", "yes").parent(parent).add(jdbc);

        readEntireDatabase();

        assertThat(importer).allSatisfy(d -> assertThat(d.getPlaceId()).isNotEqualTo(place.getPlaceId()));
    }

    /**
     * Places without a country can be imported.
     */
    @Test
    void testNoCountry() {
        PlacexTestRow place = new PlacexTestRow("building", "yes").name("Building").country(null).add(jdbc);

        readEntireDatabase();

        importer.assertThatByRow(place)
                        .hasFieldOrPropertyWithValue("countryCode", null);
    }

    @Test
    void testGeometry() {
        supportGeometries = true;
        PlacexTestRow place = new PlacexTestRow("building", "yes").name("Oosterbroek Zuivel").country("de").add(jdbc);
        readEntireDatabase();

        importer.assertThatByRow(place)
                .extracting("geometry").isNotNull();
    }

    @Test
    void testUsePostcodeFromPlacex() {
        supportGeometries = true;
        PlacexTestRow parent = PlacexTestRow.make_street("Main St").add(jdbc);
        PlacexTestRow place = new PlacexTestRow("building", "yes")
                .addr("housenumber", "34")
                .postcode("AA 44XH")
                .parent(parent).add(jdbc);

        readEntireDatabase();

        importer.assertThatByRow(place)
                .hasFieldOrPropertyWithValue("postcode", "AA 44XH");
    }

    @Test
    void testPreferPostcodeFromPostcodeRelations() {
        PlacexTestRow parent = PlacexTestRow.make_street("Main St").add(jdbc);
        PlacexTestRow place = new PlacexTestRow("building", "yes")
                .addr("housenumber", "34")
                .postcode("XXX")
                .parent(parent).add(jdbc);
        PlacexTestRow postcode = new PlacexTestRow("boundary", "postal_code")
                .name("ref", "1234XZ").ranks(11).add(jdbc);

        parent.addAddresslines(jdbc, postcode);

        readEntireDatabase();

        importer.assertThatByRow(place)
                .hasFieldOrPropertyWithValue("postcode", "1234XZ");
    }

    @Test
    void testPreferPostcodeFromAddress() {
        PlacexTestRow parent = PlacexTestRow.make_street("Main St").add(jdbc);
        PlacexTestRow place = new PlacexTestRow("building", "yes")
                .addr("housenumber", "34")
                .addr("postcode", "45-234")
                .postcode("XXX")
                .parent(parent).add(jdbc);
        PlacexTestRow postcode = new PlacexTestRow("boundary", "postal_code")
                .name("ref", "1234XZ").ranks(11).add(jdbc);

        parent.addAddresslines(jdbc, postcode);

        readEntireDatabase();

        importer.assertThatByRow(place)
                .hasFieldOrPropertyWithValue("postcode", "45-234");
    }

    @Test
    void testGetImportDate() {
        setupImporter();

        assertThat(connector.getLastImportDate()).isNull();

        var importDate = new Date();
        jdbc.update("INSERT INTO import_status(lastimportdate, indexed) VALUES(?, ?)", importDate, true);
        assertThat(connector.getLastImportDate()).hasSameTimeAs(importDate);
    }
}