package de.komoot.photon.opensearch;

import de.komoot.photon.DatabaseProperties;

import java.util.Date;

public class DBPropertyEntry {
    public String databaseVersion;
    public Date importDate;
    public String[] languages;
    public boolean supportStructuredQueries;

    public DBPropertyEntry() {}

    public DBPropertyEntry(DatabaseProperties props, String databaseVersion) {
        this.databaseVersion = databaseVersion;
        importDate = props.getImportDate();
        languages = props.getLanguages();
        supportStructuredQueries = props.getSupportStructuredQueries();
    }
}
