package de.komoot.photon.nominatim;

import de.komoot.photon.ReflectionTestUtil;
import de.komoot.photon.nominatim.testdb.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.junit.jupiter.api.Assertions.*;

class NominatimUpdaterDBTest {
    private EmbeddedDatabase db;
    private NominatimUpdater connector;
    private CollectingUpdater updater;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setup() {
        db = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .addScript("/test-schema.sql")
                .build();


        connector = new NominatimUpdater(null, 0, null, null, null, new H2DataAdapter(), false);
        updater = new CollectingUpdater();
        connector.setUpdater(updater);

        jdbc = new JdbcTemplate(db);
        ReflectionTestUtil.setFieldValue(connector, "template", jdbc);
        ReflectionTestUtil.setFieldValue(connector, "exporter", "template", jdbc);
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

        updater.add_existing(place_id, 0);

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

        updater.add_existing(place_id, 0, 1, 2, 3);

        connector.update();
        updater.assertFinishCalled();

        assertEquals(4, updater.numDeleted());
        assertEquals(0, updater.numCreated());

        updater.assertHasDeleted(place_id, 4);
    }

    @Test
    void testUpdateLowerRanks() {
        PlacexTestRow place = new PlacexTestRow("place", "city").name("Town").rankAddress(12).add(jdbc);
        (new PhotonUpdateRow("placex", place.getPlaceId(), "UPDATE")).add(jdbc);

        // Pretending to have two documents for the place in question to check that
        // the algorithm stops at the first.
        // In practise, a rankAddress<30 document cannot have duplicates.
        updater.add_existing(place.getPlaceId(), 0, 1);

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

        updater.add_existing(place.getPlaceId(), 0);

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

        updater.add_existing(place.getPlaceId(), 0);

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

        updater.add_existing(place.getPlaceId(), 0, 1, 2);

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

        updater.add_existing(place.getPlaceId(), 0, 1, 2);

        connector.update();
        updater.assertFinishCalled();

        assertEquals(2, updater.numDeleted());
        assertEquals(1, updater.numCreated());

        updater.assertHasCreated(place.getPlaceId(), "1");
        updater.assertHasDeleted(place.getPlaceId(), 2);
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

        updater.add_existing(osmline.getPlaceId(), 0, 1, 2);

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

        updater.add_existing(osmline.getPlaceId(), 0);

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

        updater.add_existing(osmline.getPlaceId(), 0, 1, 2, 3, 4);

        connector.update();
        updater.assertFinishCalled();

        assertEquals(2, updater.numDeleted());
        assertEquals(3, updater.numCreated());

        updater.assertHasCreated(osmline.getPlaceId(), "6");
        updater.assertHasCreated(osmline.getPlaceId(), "7");
        updater.assertHasCreated(osmline.getPlaceId(), "8");
        updater.assertHasDeleted(osmline.getPlaceId(), 2);
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
}
