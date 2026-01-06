package de.komoot.photon.config;

import com.beust.jcommander.Parameter;

public class PostgresqlConfig {
    public static final String GROUP = "PostgreSQL options";

    @Parameter(names = "-host", category = GROUP, placeholder = "HOST", description = """
            Hostname of the PostgreSQL database
            """)
    private String host = "127.0.0.1";

    @Parameter(names = "-port", category = GROUP, placeholder = "PORT", description = """
            Port of the PostgreSQL database
            """)
    private int port = 5432;

    @Parameter(names = "-database", category = GROUP, placeholder = "NAME", description = """
            Database name of Nominatim database
            """)
    private String database = "nominatim";

    @Parameter(names = "-user", category = GROUP, placeholder = "NAME", description = """
            User for the PostgreSQL database
            """)
    private String user = "nominatim";

    @Parameter(names = "-password", category = GROUP, placeholder = "PASSWORD", description = """
            Password for the PostgreSQL user
            """)
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
