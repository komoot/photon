package de.komoot.photon;

import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * Class collecting database global properties.
 *
 * The server is responsible for making the data persistent throught the Photon database.
 */
@Slf4j
public class DatabaseProperties {


    private String[] languages = null;

    /**
     * Return the list of languages for which the database is configured.
     * @return
     */
    public String[] getLanguages() {
        if (languages == null) {
            return new String[]{"en", "de", "fr", "it"};
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
}
