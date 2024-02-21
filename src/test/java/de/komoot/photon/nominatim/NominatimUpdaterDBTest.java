package de.komoot.photon.nominatim;

import com.vividsolutions.jts.io.ParseException;
import de.komoot.photon.ReflectionTestUtil;
import de.komoot.photon.nominatim.testdb.CollectingUpdater;
import de.komoot.photon.nominatim.testdb.H2DataAdapter;
import de.komoot.photon.nominatim.testdb.PhotonUpdateRow;
import de.komoot.photon.nominatim.testdb.PlacexTestRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

public class NominatimUpdaterDBTest {
    private EmbeddedDatabase db;
    private NominatimUpdater connector;
    private CollectingUpdater updater;
    private JdbcTemplate jdbc;

    @BeforeEach
    public void setup() {
        db = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .addScript("/test-schema.sql")
                .build();


        connector = new NominatimUpdater(null, 0, null, null, null, new H2DataAdapter());
        updater = new CollectingUpdater();
        connector.setUpdater(updater);

        jdbc = new JdbcTemplate(db);
        ReflectionTestUtil.setFieldValue(connector, "template", jdbc);
        ReflectionTestUtil.setFieldValue(connector, "exporter", "template", jdbc);
    }

    @Test
    public void testSimpleUpdate() {
        PlacexTestRow place = new PlacexTestRow("place", "city").name("Town").add(jdbc);
        (new PhotonUpdateRow("placex", place.getPlaceId(), "UPDATE")).add(jdbc);

        connector.update();

        updater.assertFinishCalled();
        updater.assertDeleted(place.getPlaceId());
        updater.assertCreatedPlaceIds(place.getPlaceId());
    }

    @Test
    public void testSimpleDelete() {
        PlacexTestRow place = new PlacexTestRow("place", "city").name("Town").add(jdbc);
        (new PhotonUpdateRow("placex", place.getPlaceId(), "DELETE")).add(jdbc);

        connector.update();

        updater.assertFinishCalled();
        updater.assertDeleted(place.getPlaceId());
        updater.assertCreatedPlaceIds();
    }
}
