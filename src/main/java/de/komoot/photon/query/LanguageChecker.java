package de.komoot.photon.query;

import com.google.common.base.Joiner;

import java.util.Set;

/**
 * Created by Sachin Dole on 2/20/2015.
 */
public class LanguageChecker {
    private final Set<String> supportedLanguages;

    public LanguageChecker(Set<String> supportedLanguages) {
        this.supportedLanguages = supportedLanguages;
    }

    public void check(String lang) throws BadRequestException {
        if(lang == null) lang = "en";
        if(!supportedLanguages.contains(lang)) {
            throw new BadRequestException(400, "language " + lang + " is not supported, supported languages are: " + Joiner.on(", ").join(supportedLanguages));
        }
    }
}
