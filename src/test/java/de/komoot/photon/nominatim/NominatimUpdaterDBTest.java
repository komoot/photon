package de.komoot.photon.nominatim;

import de.komoot.photon.DatabaseProperties;
import de.komoot.photon.ReflectionTestUtil;
import de.komoot.photon.nominatim.model.AddressType;
import de.komoot.photon.nominatim.testdb.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class NominatimUpdaterDBTest {
    private NominatimUpdater connector;
    private CollectingUpdater updater;
    private JdbcTemplate jdbc;

    private static final Map<String, String> COUNTRY_NAMES = Map.of("default", "USA", "en", "United States");

    @BeforeEach
    void setup() {
        EmbeddedDatabase db = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .addScript("/test-schema.sql")
                .build();


        connector = new NominatimUpdater(null, 0, null, null, null, new H2DataAdapter(), new DatabaseProperties());
        updater = new CollectingUpdater();
        connector.setUpdater(updater);

        jdbc = new JdbcTemplate(db);
        TransactionTemplate txTemplate = new TransactionTemplate(new DataSourceTransactionManager(db));
        ReflectionTestUtil.setFieldValue(connector, NominatimConnector.class, "template", jdbc);
        ReflectionTestUtil.setFieldValue(connector, NominatimConnector.class, "txTemplate", txTemplate);
    }

    @Test
    void testSimpleInsert() {
        PlacexTestRow place = new PlacexTestRow("place", "city").name("Town").add(jdbc);
        new PhotonUpdateRow("placex", place.getPlaceId(), "UPDATE").add(jdbc);

        connector.update();
        assertThat(updater.getFinishCalled()).isEqualTo(1);

        updater.assertThatDeleted().isEmpty();
        updater.assertThatCreated().singleElement().satisfies(place::assertEquals);
    }

    @ParameterizedTest
    @ValueSource(strings = {"placex", "location_property_osmline"})
    void testSimpleDelete(String table) {
        final long place_id = 47836;
        new PhotonUpdateRow(table, place_id, "DELETE").add(jdbc);

        connector.update();

        updater.assertThatCreated().isEmpty();
        updater.assertThatDeleted().containsExactly(place_id);
    }

    @ParameterizedTest
    @ValueSource(strings = {"placex", "location_property_osmline"})
    void testInsertNonExisting(String table) {
        new PhotonUpdateRow(table, 12345L, "UPDATE").add(jdbc);

        connector.update();
        assertThat(updater.getFinishCalled()).isEqualTo(1);

        updater.assertThatDeleted().isEmpty();
        updater.assertThatCreated().isEmpty();
    }

    @Test
    void testUpdateLowerRanksWithAddress() {
        PlacexTestRow place = PlacexTestRow.make_street("Burg").add(jdbc);

        place.addAddresslines(jdbc,
                new PlacexTestRow("place", "neighbourhood").name("Le Coin").ranks(24).add(jdbc),
                new PlacexTestRow("place", "suburb").name("Crampton").ranks(20).add(jdbc),
                new PlacexTestRow("place", "city").name("Grand Junction").ranks(16).add(jdbc),
                new PlacexTestRow("place", "county").name("Lost County").ranks(12).add(jdbc),
                new PlacexTestRow("place", "state").name("Le Havre").ranks(8).add(jdbc));


        new PhotonUpdateRow("placex", place.getPlaceId(), "UPDATE").add(jdbc);

        connector.update();

        updater.assertThatDeleted().isEmpty();
        updater.assertThatCreated().singleElement()
                .satisfies(place::assertEquals)
                .hasFieldOrPropertyWithValue("addressParts", Map.of(
                        AddressType.LOCALITY, Map.of("default", "Le Coin"),
                        AddressType.DISTRICT, Map.of("default", "Crampton"),
                        AddressType.CITY, Map.of("default", "Grand Junction"),
                        AddressType.COUNTY, Map.of("default", "Lost County"),
                        AddressType.STATE, Map.of("default", "Le Havre"),
                        AddressType.COUNTRY, COUNTRY_NAMES));
    }

    @Test
    void testUpdatePlaceAddressAddressRank0() {
        PlacexTestRow place = new PlacexTestRow("natural", "water").name("Lake Tee").rankAddress(0).rankSearch(20).add(jdbc);

        place.addAddresslines(jdbc,
                new PlacexTestRow("place", "county").name("Lost County").ranks(12).add(jdbc),
                new PlacexTestRow("place", "state").name("Le Havre").ranks(8).add(jdbc));

        new PhotonUpdateRow("placex", place.getPlaceId(), "UPDATE").add(jdbc);

        connector.update();

        updater.assertThatDeleted().isEmpty();
        updater.assertThatCreated().singleElement()
                .satisfies(place::assertEquals)
                .hasFieldOrPropertyWithValue("addressParts", Map.of(
                        AddressType.COUNTY, Map.of("default", "Lost County"),
                        AddressType.STATE, Map.of("default", "Le Havre"),
                        AddressType.COUNTRY, COUNTRY_NAMES));
    }


    @Test
    void testUpdateSimpleRank30() {
        PlacexTestRow parent = PlacexTestRow.make_street("Burg").add(jdbc);

        parent.addAddresslines(jdbc,
                new PlacexTestRow("place", "city").name("Grand Junction").ranks(16).add(jdbc));

        PlacexTestRow place = new PlacexTestRow("place", "house").name("House").parent(parent).add(jdbc);

        new PhotonUpdateRow("placex", place.getPlaceId(), "UPDATE").add(jdbc);

        connector.update();

        updater.assertThatDeleted().isEmpty();
        updater.assertThatCreated().singleElement()
                .satisfies(place::assertEquals)
                .hasFieldOrPropertyWithValue("addressParts", Map.of(
                        AddressType.STREET, Map.of("default", "Burg"),
                        AddressType.CITY, Map.of("default", "Grand Junction"),
                        AddressType.COUNTRY, COUNTRY_NAMES));

    }

    @Test
    void testUpdateRank30MoreHousenumbers() {
        PlacexTestRow parent = PlacexTestRow.make_street("Main St").add(jdbc);
        PlacexTestRow place = new PlacexTestRow("building", "yes").addr("housenumber", "1;2a;3").parent(parent).add(jdbc);

        new PhotonUpdateRow("placex", place.getPlaceId(), "UPDATE").add(jdbc);

        connector.update();

        updater.assertThatDeleted().isEmpty();
        updater.assertThatCreated()
                .allSatisfy(place::assertEquals)
                .allSatisfy(d -> assertThat(d.getAddressParts())
                        .containsEntry(AddressType.STREET, Map.of("default", "Main St")))
                .extracting("houseNumber")
                .containsExactlyInAnyOrder("1", "2a", "3");

    }

    @Test
    void testInsertInterpolation() {
        PlacexTestRow street = PlacexTestRow.make_street("La strada").add(jdbc);

        var osmline = new OsmlineTestRow().number(6, 8, 1).parent(street).geom("LINESTRING(0 0, 0 1)").add(jdbc);

        new PhotonUpdateRow("location_property_osmline", osmline.getPlaceId(), "UPDATE").add(jdbc);

        connector.update();

        updater.assertThatDeleted().isEmpty();
        updater.assertThatCreated()
                .allSatisfy(osmline::assertEquals)
                .extracting("houseNumber")
                .containsExactlyInAnyOrder("6", "7", "8");
    }


    @Test
    void testUpdateWithDuplicatesDeleteLast() throws InterruptedException {
        PlacexTestRow place1 = new PlacexTestRow("place", "city").name("Town").rankAddress(12).add(jdbc);
        PlacexTestRow place2 = new PlacexTestRow("building", "yes").housenumber(23).rankAddress(30).add(jdbc);

        (new PhotonUpdateRow("placex", place2.getPlaceId(), "UPDATE")).add(jdbc);
        (new PhotonUpdateRow("placex", place1.getPlaceId(), "UPDATE")).add(jdbc);
        Thread.sleep(2L);
        (new PhotonUpdateRow("placex", place2.getPlaceId(), "UPDATE")).add(jdbc);
        Thread.sleep(2L);
        (new PhotonUpdateRow("placex", place1.getPlaceId(), "UPDATE")).add(jdbc);
        (new PhotonUpdateRow("placex", place2.getPlaceId(), "DELETE")).add(jdbc);

        connector.update();

        updater.assertThatDeleted().containsExactly(place2.getPlaceId());
        updater.assertThatCreated().singleElement().satisfies(place1::assertEquals);
    }
}
