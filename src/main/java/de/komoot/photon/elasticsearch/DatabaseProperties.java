package de.komoot.photon.elasticsearch;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.util.*;

/**
 * Class responsible for loading and saving database global properties.
 *
 * The properties are saved in a separate document in the Photon index that does not contain
 * any indexed data.
 */
@Slf4j
public class DatabaseProperties {
    /**
     * Database version created by new imports with the current code.
     *
     * Format must be: major.minor.patch-dev
     *
     * Increase to next to be released version when the database layout
     * changes in an incompatible way. If it is alredy at the next released
     * version, increase the dev version.
     */
    private static final String DATABASE_VERSION = "0.3.4-0";
    public static final String PROPERTY_DOCUMENT_ID = "DATABASE_PROPERTIES";

    private static final String BASE_FIELD = "document_properties";
    private static final String FIELD_VERSION = "database_version";
    private static final String FIELD_LANGUAGES = "indexed_languages";

    private String[] languages = null;

    /**
     * Return the list of languages for which the database is configured.
     * @return
     */
    public String[] getLanguages() {
        if (languages == null) {
            return new String[]{"en", "de", "fr", "it", "ja"};
        }

        return languages;
    }

    /**
     * Replace the language list with the given list.
     *
     * @param languages Array of two-letter language codes.
     *
     * @return This object for chaining.
     */
    public DatabaseProperties setLanguages(String[] languages) {
        this.languages = languages;
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
        if (languages == null) {
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
                throw new RuntimeException("Language list '" + languageList.toString() +
                        "not compatible with languages in database(" + languages.toString() + ")");
            }

            languages = newLanguageList.toArray(new String[]{});
        }
    }

    /**
     * Save the global properties to the database.
     *
     * The function saved properties available as members and the database version
     * as currently defined in DATABASE_VERSION.
     */
    public void saveToDatabase(Client client) throws IOException  {
        final XContentBuilder builder = XContentFactory.jsonBuilder().startObject().startObject(BASE_FIELD)
                        .field(FIELD_VERSION, DATABASE_VERSION)
                        .field(FIELD_LANGUAGES, String.join(",", languages))
                        .endObject().endObject();

        client.prepareIndex(PhotonIndex.NAME, PhotonIndex.TYPE).
                    setSource(builder).setId(PROPERTY_DOCUMENT_ID).execute().actionGet();
    }

    /**
     * Load the global properties from the database.
     *
     * The function first loads the database version and throws an exception if it does not correspond
     * to the version as defined in DATABASE_VERSION.
     *
     * Currently does nothing when the property entry is missing. Later versions with a higher
     * database version will then fail.
     */
    public void loadFromDatabase(Client client) {
        GetResponse response = client.prepareGet(PhotonIndex.NAME, PhotonIndex.TYPE, PROPERTY_DOCUMENT_ID).execute().actionGet();

        // We are currently at the database version where versioning was introduced.
        if (!response.isExists()) {
            return;
        }

        Map<String, String> properties = (Map<String, String>) response.getSource().get(BASE_FIELD);

        if (properties == null) {
            throw new RuntimeException("Found database properties but no '" + BASE_FIELD +"' field. Database corrupt?");
        }

        String version = properties.getOrDefault(FIELD_VERSION, "");
        if (!DATABASE_VERSION.equals(version)) {
            log.error("Database has incompatible version '" + version + "'. Expected: " + DATABASE_VERSION);
            throw new RuntimeException("Incompatible database.");
        }

        String langString = properties.get(FIELD_LANGUAGES);
        if (langString == null) {
            languages = null;
        } else {
            languages = langString.split(",");
        }
    }
}
