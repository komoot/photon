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
    private static final Set<String> DEFAULT_LANGUAGES = Set.of("en", "de", "fr", "it");

    private Set<String> languages = DEFAULT_LANGUAGES;
    @Nullable private Date importDate;
    private boolean supportStructuredQueries = true;
    private boolean supportGeometries = false;
    private boolean synonymsInstalled = false;
    private ConfigExtraTags extraTags = new ConfigExtraTags();
    private boolean reverseOnly = false;
    private boolean streetHousenumberFull = false;

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
     * @return Set of supported languages.
     */
    public Set<String> getLanguages() {
        return languages;
    }

    /**
     * Replace the language list with the given list.
     *
     * @param languages Array of two-letter language codes.
     *
     * @return This object for function chaining.
     */
    public DatabaseProperties setLanguages(Set<String> languages) {
        this.languages = languages;
        return this;
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
                "languages=" + Arrays.toString(languages.toArray()) +
                ", importDate=" + importDate +
                ", supportStructuredQueries=" + supportStructuredQueries +
                ", supportGeometries=" + supportGeometries +
                ", synonymsInstalled=" + synonymsInstalled +
                ", extraTags=" + extraTags +
                ", streetHousenumberFull=" + streetHousenumberFull +
                '}';
    }

    public void setReverseOnly(boolean reverseOnly) {
        this.reverseOnly = reverseOnly;
    }

    public boolean getReverseOnly() {
        return reverseOnly;
    }

    /**
     * Whether street-based addresses that carry a separate street number should be indexed with
     * the full combined house number (the {@code housenumber} value, e.g. the Czech/Slovak
     * conscription/orientation form {@code 2531/80}) instead of the plain street number.
     *
     * @return {@code true} if the full combined house number is used, {@code false} otherwise.
     */
    public boolean getStreetHousenumberFull() {
        return streetHousenumberFull;
    }

    public void setStreetHousenumberFull(boolean streetHousenumberFull) {
        this.streetHousenumberFull = streetHousenumberFull;
    }
}
