package de.komoot.photon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;

import static de.komoot.photon.Server.DATABASE_VERSION;

/**
 * Class collecting database global properties.
 * <p>
 * This class is marshalled and unmarshalled into OS table properties using Jackson.
 */
@NullMarked
public class DatabaseProperties {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String[] DEFAULT_LANGAUGES = new String[]{"en", "de", "fr", "it"};

    private String[] languages = DEFAULT_LANGAUGES;
    @Nullable private Date importDate;
    private boolean supportStructuredQueries = true;
    private boolean supportGeometries = false;
    private boolean synonymsInstalled = false;
    private ConfigExtraTags extraTags = new ConfigExtraTags();

    @SuppressWarnings("unused")
    public void setDatabaseVersion(String version) {
        if (!DATABASE_VERSION.equals(version)) {
            LOGGER.error("Database has incompatible version '{}'. Expected: {}",
                    version, DATABASE_VERSION);
            throw new UsageException("Incompatible database.");
        }
    }

    @SuppressWarnings("unused")
    public String getDatabaseVersion() {
        return DATABASE_VERSION;
    }

    /**
     * Return the list of languages for which the database is configured.
     * If no list was set, then the default is returned.
     *
     * @return Array of supported languages.
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
    public DatabaseProperties setLanguages(String @Nullable[] languages) {
        this.languages = (languages == null) ? DEFAULT_LANGAUGES : languages;
        return this;
    }

    /**
     * Set language list to the intersection between the existing list and the given list.
     * <p>
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

    @Nullable
    public Date getImportDate() {
        return this.importDate;
    }

    public void setImportDate(@Nullable Date importDate) {
        this.importDate = importDate;
    }

    public boolean getSupportGeometries() {
        return supportGeometries;
    }

    public void setSupportGeometries(boolean supportGeometries) {
        this.supportGeometries = supportGeometries;
    }

    // needed for backwards compatibility
    @SuppressWarnings("unused")
    public void setSupportStructuredQueries(boolean supportStructuredQueries) {
        this.supportStructuredQueries = supportStructuredQueries;
    }

    public void setExtraTags(List<String> extraTags) {
        this.extraTags = new ConfigExtraTags(extraTags);
    }

    @SuppressWarnings("unused")
    public List<String> getExtraTags() {
        return extraTags.asConfigParam();
    }

    public void setSynonymsInstalled(boolean synonymsInstalled) {
        this.synonymsInstalled = synonymsInstalled;
    }

    public boolean getSynonymsInstalled() {
        return synonymsInstalled;
    }

    public ConfigExtraTags configExtraTags() {
        return extraTags;
    }

    public void putConfigExtraTags(ConfigExtraTags extraTags) {
        this.extraTags = extraTags;
    }

    @Override
    public String toString() {
        return "DatabaseProperties{" +
                "languages=" + Arrays.toString(languages) +
                ", importDate=" + importDate +
                ", supportStructuredQueries=" + supportStructuredQueries +
                ", supportGeometries=" + supportGeometries +
                ", synonymsInstalled=" + synonymsInstalled +
                ", extraTags=" + extraTags +
                '}';
    }
}
