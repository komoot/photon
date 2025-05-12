package de.komoot.photon;

import org.slf4j.Logger;

import java.util.*;

import static de.komoot.photon.Server.DATABASE_VERSION;

/**
 * Class collecting database global properties.
 *
 * The server is responsible for making the data persistent in the Photon database.
 */
public class DatabaseProperties {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DatabaseProperties.class);
    private static final String[] DEFAULT_LANGAUGES = new String[]{"en", "de", "fr", "it"};

    private String[] languages = DEFAULT_LANGAUGES;
    private Date importDate;
    private boolean supportStructuredQueries = false;
    private boolean supportGeometries = false;
    private ConfigExtraTags extraTags = new ConfigExtraTags();

    public void setVersion(String version) {
        if (!DATABASE_VERSION.equals(version)) {
            LOGGER.error("Database has incompatible version '{}'. Expected: {}",
                    version, DATABASE_VERSION);
            throw new UsageException("Incompatible database.");
        }
    }

    public String getVersion() {
        return DATABASE_VERSION;
    }

    /**
     * Return the list of languages for which the database is configured.
     * If no list was set, then the default is returned.
     *
     * @return List of supported languages.
     */
    public String[] getLanguages() {
        return languages;
    }

    /**
     * Replace the language list with the given list.
     *
     * @param languages Array of two-letter language codes.
     *
     * @return This object for function chaining.
     */
    public DatabaseProperties setLanguages(String[] languages) {
        this.languages = languages == null ? DEFAULT_LANGAUGES : languages;
        return this;
    }

    /**
     * Set language list to the intersection between the existing list and the given list.
     *
     * The final list will use the same order as the given list.
     *
     * @param languageList Comma-separated list of two-letter language codes.
     */
    public void restrictLanguages(String[] languageList) {
        if (languages == DEFAULT_LANGAUGES) {
            // Special case for versions that did not yet have a language list set
            // in the database: Use the given list as is.
            languages = languageList;
        } else {
            Set<String> currentLanguageSet = new HashSet<>(Arrays.asList(languages));
            List<String> newLanguageList = new ArrayList<>();

            for (String lang : languageList) {
                if (currentLanguageSet.contains(lang)) {
                    newLanguageList.add(lang);
                }
            }

            if (newLanguageList.isEmpty()) {
                throw new UsageException("Language list '" + Arrays.toString(languageList) +
                        "' not compatible with languages in database(" + Arrays.toString(languages) + ")");
            }

            languages = newLanguageList.toArray(new String[]{});
        }
    }

    public Date getImportDate() {
        return this.importDate;
    }

    public DatabaseProperties setImportDate(Date importDate) {
        this.importDate = importDate;
        return this;
    }

    public boolean getSupportStructuredQueries() {
        return supportStructuredQueries;
    }

    public DatabaseProperties setSupportStructuredQueries(boolean supportStructuredQueries) {
        this.supportStructuredQueries = supportStructuredQueries;
        return this;
    }

    public boolean getSupportGeometries() {
        return supportGeometries;
    }

    public DatabaseProperties setSupportGeometries(boolean supportGeometries) {
        this.supportGeometries = supportGeometries;
        return this;
    }

    public void setExtraTags(List<String> extraTags) {
        this.extraTags = new ConfigExtraTags(extraTags);
    }

    public List<String> getExtraTags() {
        return extraTags.asConfigParam();
    }

    public ConfigExtraTags configExtraTags() {
        return extraTags;
    }

    @Override
    public String toString() {
        return "DatabaseProperties{" +
                "languages=" + Arrays.toString(languages) +
                ", importDate=" + importDate +
                ", supportStructuredQueries=" + supportStructuredQueries +
                ", supportGeometries=" + supportGeometries +
                '}';
    }
}
