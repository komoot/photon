package de.komoot.photon.config;

import com.beust.jcommander.Parameter;

public class PostgresqlConfig {
    @Parameter(names = "-host", description = "Hostname of the PostgreSQL database.")
    private String host = "127.0.0.1";

    @Parameter(names = "-port", description = "Port of the PostgreSQL database.")
    private int port = 5432;

    @Parameter(names = "-database", description = "Database name of the PostgreSQL database.")
    private String database = "nominatim";

    @Parameter(names = "-user", description = "Username in the PostgreSQL database.")
    private String user = "nominatim";

    @Parameter(names = "-password", description = "Password for the PostgreSQL database.")
    private String password = null;

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getDatabase() {
        return this.database;
    }

    public String getUser() {
        return this.user;
    }

    public String getPassword() {
        return this.password;
    }

    @Override
    public String toString() {
        return String.format("database %s at %s:%d (user: %s)", database, host, port, user);
    }
}
