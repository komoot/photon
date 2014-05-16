package de.komoot.photon.importer.nominatim;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Export nominatim data
 * @author felix
 */
public class Exporter {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Exporter.class);

    private Connection pgConnection;
    private int fetchSize = 10000;

    public void export() {

        long counter = 0;
        long time = System.currentTimeMillis();

        try {
            Statement query = this.pgConnection.createStatement();

            query.setFetchSize(this.fetchSize);

            ResultSet results = query.executeQuery("SELECT placex.place_id as place_id," +
                    "name, housenumber, st_y(centroid) as lat, st_x(centroid) as lon, admin_level from placex limit 100;");
            while (results.next()) {
                if(counter % 10000 == 0 && counter > 0) {
                    LOGGER.info(String.format("progress: %10d entries [%.1f / second]", counter, 10000000. / (1. * System.currentTimeMillis() - time)));
                    time = System.currentTimeMillis();
                }

                Statement detailQuery = this.pgConnection.createStatement();
                ResultSet detailResults = detailQuery.executeQuery("SELECT place_id, osm_type, osm_id, name->'name' as name, name->'ref' as name_ref, name->'place_name' as place_name, name->'short_name' as short_name, name->'official_name' as official_name, class," +
                        " type, admin_level, rank_address FROM get_addressdata("+results.getString("place_id")+") WHERE isaddress ORDER BY rank_address DESC");
                while(detailResults.next())
                {
                    LOGGER.info(String.format("%s - %s - %s", detailResults.getString("name"), detailResults.getString("admin_level"), detailResults.getString("rank_address")));
                }

                LOGGER.info(results.getString(1));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }



    /**
     * @param host       database host
     * @param port       database port
     * @param database   database name
     * @param username   db username
     * @param password   db username's password
     */
    public Exporter(String host, int port, String database, String username, String password){
        this.pgConnection = new PostgresConnector().connect(host,port,database,username,password);
        try {
            this.pgConnection.setAutoCommit(false);
        } catch (SQLException e) {
            LOGGER.error("cant disable autcommitting");
        }
    }

}
