package de.komoot.photon.nominatim;

import de.komoot.photon.AssertUtil;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.ReflectionTestUtil;
import de.komoot.photon.nominatim.model.AddressType;
import de.komoot.photon.nominatim.testdb.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

class NominatimUpdaterDBTest {
    private NominatimUpdater connector;
    private CollectingUpdater updater;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setup() {
        EmbeddedDatabase db = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .addScript("/test-schema.sql")
                .build();


        connector = new NominatimUpdater(null, 0, null, null, null, new H2DataAdapter(), false);
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
        (new PhotonUpdateRow("placex", place.getPlaceId(), "UPDATE")).add(jdbc);

        connector.update();
        updater.assertFinishCalled();

        assertEquals(0, updater.numDeleted());
        assertEquals(1, updater.numCreated());

        updater.assertHasCreated(place.getPlaceId());
    }

    @Test
    void testSimpleDelete() {
        final long place_id = 47836;
        (new PhotonUpdateRow("placex", place_id, "DELETE")).add(jdbc);

        connector.update();
        updater.assertFinishCalled();

        assertEquals(1, updater.numDeleted());
        assertEquals(0, updater.numCreated());

        updater.assertHasDeleted(place_id);
    }

    @Test
    void testSimpleDeleteNonExisting() {
        final long place_id = 28119;
        (new PhotonUpdateRow("placex", place_id, "DELETE")).add(jdbc);

        connector.update();
        updater.assertFinishCalled();

        assertEquals(1, updater.numDeleted());
        assertEquals(0, updater.numCreated());

        updater.assertHasDeleted(place_id);
    }

    @Test
    void testDeleteMultiDocument() {
        final long place_id = 887;
        (new PhotonUpdateRow("placex", place_id, "DELETE")).add(jdbc);

        connector.update();
        updater.assertFinishCalled();

        assertEquals(1, updater.numDeleted());
        assertEquals(0, updater.numCreated());

        updater.assertHasDeleted(place_id);
    }

    @Test
    void testUpdateLowerRanks() {
        PlacexTestRow place = new PlacexTestRow("place", "city").name("Town").rankAddress(12).add(jdbc);
        (new PhotonUpdateRow("placex", place.getPlaceId(), "UPDATE")).add(jdbc);

        connector.update();
        updater.assertFinishCalled();

        assertEquals(0, updater.numDeleted());
        assertEquals(1, updater.numCreated());

        updater.assertHasCreated(place.getPlaceId());
    }

    @Test
    void testUpdateSimpleRank30() {
        PlacexTestRow place = new PlacexTestRow("building", "yes").housenumber(23).rankAddress(30).add(jdbc);
        (new PhotonUpdateRow("placex", place.getPlaceId(), "UPDATE")).add(jdbc);

        connector.update();
        updater.assertFinishCalled();

        assertEquals(0, updater.numDeleted());
        assertEquals(1, updater.numCreated());

        updater.assertHasCreated(place.getPlaceId());
    }

    @Test
    void testUpdateRank30MoreHousenumbers() {
        PlacexTestRow place = new PlacexTestRow("building", "yes").addr("housenumber", "1;2a;3").rankAddress(30).add(jdbc);
        (new PhotonUpdateRow("placex", place.getPlaceId(), "UPDATE")).add(jdbc);

        connector.update();
        updater.assertFinishCalled();

        assertEquals(0, updater.numDeleted());
        assertEquals(3, updater.numCreated());

        updater.assertHasCreated(place.getPlaceId(), "1");
        updater.assertHasCreated(place.getPlaceId(), "2a");
        updater.assertHasCreated(place.getPlaceId(), "3");
    }

    @Test
    void testUpdateRank30SameHousenumbers() {
        PlacexTestRow place = new PlacexTestRow("building", "yes").addr("housenumber", "1;2a;3").rankAddress(30).add(jdbc);
        (new PhotonUpdateRow("placex", place.getPlaceId(), "UPDATE")).add(jdbc);

        connector.update();
        updater.assertFinishCalled();

        assertEquals(0, updater.numDeleted());
        assertEquals(3, updater.numCreated());

        updater.assertHasCreated(place.getPlaceId(), "1");
        updater.assertHasCreated(place.getPlaceId(), "2a");
        updater.assertHasCreated(place.getPlaceId(), "3");
    }

    @Test
    void testUpdateRank30LessHousenumbers() {
        PlacexTestRow place = new PlacexTestRow("building", "yes").addr("housenumber", "1").rankAddress(30).add(jdbc);
        (new PhotonUpdateRow("placex", place.getPlaceId(), "UPDATE")).add(jdbc);

        connector.update();
        updater.assertFinishCalled();

        assertEquals(0, updater.numDeleted());
        assertEquals(1, updater.numCreated());

        updater.assertHasCreated(place.getPlaceId(), "1");
    }

    @Test
    void testInsertInterpolation() {
        PlacexTestRow street = PlacexTestRow.make_street("La strada").add(jdbc);

        OsmlineTestRow osmline =
                new OsmlineTestRow().number(6, 8, 1).parent(street).geom("LINESTRING(0 0, 0 1)").add(jdbc);
        (new PhotonUpdateRow("location_property_osmline", osmline.getPlaceId(), "UPDATE")).add(jdbc);

        connector.update();
        updater.assertFinishCalled();

        assertEquals(0, updater.numDeleted());
        assertEquals(3, updater.numCreated());

        updater.assertHasCreated(osmline.getPlaceId(), "6");
        updater.assertHasCreated(osmline.getPlaceId(), "7");
        updater.assertHasCreated(osmline.getPlaceId(), "8");
    }

    @Test
    void testUpdateInterpolationSameLength() {
        PlacexTestRow street = PlacexTestRow.make_street("La strada").add(jdbc);

        OsmlineTestRow osmline =
                new OsmlineTestRow().number(6, 8, 1).parent(street).geom("LINESTRING(0 0, 0 1)").add(jdbc);
        (new PhotonUpdateRow("location_property_osmline", osmline.getPlaceId(), "UPDATE")).add(jdbc);

        connector.update();
        updater.assertFinishCalled();

        assertEquals(0, updater.numDeleted());
        assertEquals(3, updater.numCreated());

        updater.assertHasCreated(osmline.getPlaceId(), "6");
        updater.assertHasCreated(osmline.getPlaceId(), "7");
        updater.assertHasCreated(osmline.getPlaceId(), "8");
    }

    @Test
    void testUpdateInterpolationLonger() {
        PlacexTestRow street = PlacexTestRow.make_street("La strada").add(jdbc);

        OsmlineTestRow osmline =
                new OsmlineTestRow().number(6, 8, 1).parent(street).geom("LINESTRING(0 0, 0 1)").add(jdbc);
        (new PhotonUpdateRow("location_property_osmline", osmline.getPlaceId(), "UPDATE")).add(jdbc);

        connector.update();
        updater.assertFinishCalled();

        assertEquals(0, updater.numDeleted());
        assertEquals(3, updater.numCreated());

        updater.assertHasCreated(osmline.getPlaceId(), "6");
        updater.assertHasCreated(osmline.getPlaceId(), "7");
        updater.assertHasCreated(osmline.getPlaceId(), "8");
    }

    @Test
    void testUpdateInterpolationShorter() {
        PlacexTestRow street = PlacexTestRow.make_street("La strada").add(jdbc);

        OsmlineTestRow osmline =
                new OsmlineTestRow().number(6, 8, 1).parent(street).geom("LINESTRING(0 0, 0 1)").add(jdbc);
        (new PhotonUpdateRow("location_property_osmline", osmline.getPlaceId(), "UPDATE")).add(jdbc);

        connector.update();
        updater.assertFinishCalled();

        assertEquals(0, updater.numDeleted());
        assertEquals(3, updater.numCreated());

        updater.assertHasCreated(osmline.getPlaceId(), "6");
        updater.assertHasCreated(osmline.getPlaceId(), "7");
        updater.assertHasCreated(osmline.getPlaceId(), "8");
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
        updater.assertFinishCalled();

        assertEquals(1, updater.numDeleted());
        assertEquals(1, updater.numCreated());

        updater.assertHasCreated(place1.getPlaceId());
        updater.assertHasDeleted(place2.getPlaceId());
    }

    @Test
    void testUpdateWithAddressLowerRanks() {
        PlacexTestRow place = PlacexTestRow.make_street("Burg").add(jdbc);
        place.addAddresslines(jdbc,
                new PlacexTestRow("place", "neighbourhood").name("Le Coin").ranks(24).add(jdbc),
                new PlacexTestRow("place", "suburb").name("Crampton").ranks(20).add(jdbc),
                new PlacexTestRow("place", "city").name("Grand Junction").ranks(16).add(jdbc),
                new PlacexTestRow("place", "county").name("Lost County").ranks(12).add(jdbc),
                new PlacexTestRow("place", "state").name("Le Havre").ranks(8).add(jdbc));
        (new PhotonUpdateRow("placex", place.getPlaceId(), "UPDATE")).add(jdbc);
        connector.update();
        updater.assertFinishCalled();
        assertEquals(0, updater.numDeleted());
        assertEquals(1, updater.numCreated());
        updater.assertHasCreated(place.getPlaceId());
        PhotonDoc doc = updater.getCreated(place.getPlaceId(), 0);
        AssertUtil.assertAddressName("Le Coin", doc, AddressType.LOCALITY);
        AssertUtil.assertAddressName("Crampton", doc, AddressType.DISTRICT);
        AssertUtil.assertAddressName("Grand Junction", doc, AddressType.CITY);
        AssertUtil.assertAddressName("Lost County", doc, AddressType.COUNTY);
        AssertUtil.assertAddressName("Le Havre", doc, AddressType.STATE);
    }
}
