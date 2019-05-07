package de.komoot.photon.query;

import com.google.common.base.Joiner;

import java.util.Set;

/**
 * Checks if a the requested language is supported by photon.
 * Created by Sachin Dole on 2/20/2015.
 */
public class LanguageChecker {
    private final Set<String> supportedLanguages;

    public LanguageChecker(Set<String> supportedLanguages) {
        this.supportedLanguages = supportedLanguages;
    }

    public boolean apply(String lang) throws BadRequestException {
        if (lang == null) lang = "en";
        if (!isLanguageSupported(lang)) {
            throw new BadRequestException(400, "language " + lang + " is not supported, supported languages are: " + Joiner.on(", ").join(supportedLanguages));
        }
        return true;
    }

    public boolean isLanguageSupported(String lang) {
        return this.supportedLanguages.contains(lang);
    }
}
