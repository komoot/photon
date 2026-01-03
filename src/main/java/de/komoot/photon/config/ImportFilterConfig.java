package de.komoot.photon.config;

import com.beust.jcommander.Parameter;
import de.komoot.photon.ConfigExtraTags;
import de.komoot.photon.DatabaseProperties;

import java.util.ArrayList;
import java.util.List;

public class ImportFilterConfig {
    @Parameter(names = "-languages", description = "Comma-separated list of languages to use. On import sets the name translations to use (default: de,en,fr,it). When running, the languages to be searched may be further restricted.")
    private List<String> languages = List.of("en", "de", "fr", "it");

    @Parameter(names = "-country-codes", description = "[import-only] Comma-separated list of country codes for countries the importer should import, comma separated. An empty list means the full database is imported.")
    private List<String> countryCodes = new ArrayList<>();

    @Parameter(names = "-extra-tags", description = "Comma-separated list of additional tags to save for each place.")
    private List<String> extraTags = null;

    @Parameter(names = "-import-geometry-column", description = "[import-only] Add the 'geometry' column from Nominatim on import (i.e. add Polygons/Linestrings/Multipolygons etc. for cities, countries etc.). WARNING: This will increase the Elasticsearch Index size! (~575GB for Planet)")
    private boolean importGeometryColumn = false;

    public String[] getLanguages() {
        return languages.toArray(String[]::new);
    }

    public String[] getCountryCodes() {
        return this.countryCodes.toArray(new String[0]);
    }

    public ConfigExtraTags getExtraTags() {
        return new ConfigExtraTags(extraTags == null? List.of() : extraTags);
    }

    public boolean isExtraTagsSet() { return this.extraTags == null; }

    public boolean getImportGeometryColumn() {
        return importGeometryColumn;
    }

    public DatabaseProperties getDatabaseProperties() {
        final var dbProps = new DatabaseProperties();
        if (!languages.isEmpty()) {
            dbProps.setLanguages(languages.toArray(new String[0]));
        }
        dbProps.setSupportGeometries(importGeometryColumn);

        if (extraTags != null) {
            dbProps.setExtraTags(extraTags);
        }

        return dbProps;
    }
}
