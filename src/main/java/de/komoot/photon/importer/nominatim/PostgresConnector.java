package de.komoot.photon.importer.nominatim;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connector for Postgres
 * @author felix
 */
public class PostgresConnector {


    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PostgresConnector.class);

    /**
     * get a Postgres Connection
     * @param host       database host
     * @param port       database port
     * @param database   database name
     * @param username   db username
     * @param password   db username's password
     */
    public Connection connect(String host, int port, String database, String username, String password) {
        LOGGER.info(String.format("connecting to database [%s] host [%s] port [%d]", database, host, port));
        Connection connection = null;
        try {

            Class.forName("org.postgresql.Driver");

        } catch (ClassNotFoundException e) {

            LOGGER.error("Where is your PostgreSQL JDBC Driver?");
            e.printStackTrace();
            return connection;

        }

        LOGGER.info("PostgreSQL JDBC Driver Registered!");

        try {

            connection = DriverManager.getConnection(String.format("jdbc:postgresql://%s:%d/%s", host, port, database), username, password);

        } catch (SQLException e) {

            LOGGER.error("Connection Failed! Check output console");
            e.printStackTrace();
            return connection;

        }

        if (connection != null) {
            LOGGER.info(String.format("Successfully connected to %s/%s", host, database));
        } else {
            LOGGER.error("Failed to make connection!");
        }


        return connection;
    }


}
