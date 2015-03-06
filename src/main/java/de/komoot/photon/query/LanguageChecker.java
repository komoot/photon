package de.komoot.photon.query;

import com.google.common.base.Joiner;
import de.komoot.photon.utils.Function;

import java.util.Set;

/**
 * Checks if a the requested language is supported by photon.
 * Created by Sachin Dole on 2/20/2015.
 */
public class LanguageChecker implements Function<String,Boolean,BadRequestException> {
    private final Set<String> supportedLanguages;

    public LanguageChecker(Set<String> supportedLanguages) {
        this.supportedLanguages = supportedLanguages;
    }

    @Override
    public Boolean apply(String lang) throws BadRequestException {
        if(lang == null) lang = "en";
        if(!supportedLanguages.contains(lang)) {
            throw new BadRequestException(400, "language " + lang + " is not supported, supported languages are: " + Joiner.on(", ").join(supportedLanguages));
        }
        return true;
    }
}
