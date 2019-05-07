package de.komoot.photon.query;

import lombok.AllArgsConstructor;
import spark.Request;
import spark.utils.StringUtils;

import java.util.List;
import java.util.Locale;

@AllArgsConstructor
class RequestLanguageResolver {
    static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";
    static final String DEFAULT_LANGUAGE = "en";

    private final LanguageChecker languageChecker;

    String resolverRequestedLanguage(Request webRequest) throws BadRequestException {
        String language = webRequest.queryParams("lang");
        if (StringUtils.isBlank(language))
            language = fallbackLanguageFromHeaders(webRequest);
        if (StringUtils.isBlank(language))
            language = DEFAULT_LANGUAGE;
        languageChecker.apply(language);
        return language;
    }

    private String fallbackLanguageFromHeaders(Request webRequest) {
        String acceptLanguageHeader = webRequest.headers(ACCEPT_LANGUAGE_HEADER);
        if (StringUtils.isBlank(acceptLanguageHeader))
            return null;
        try {
            List<Locale.LanguageRange> languages = Locale.LanguageRange.parse(acceptLanguageHeader);
            for (Locale.LanguageRange lang : languages)
                if (languageChecker.isLanguageSupported(lang.getRange()))
                    return lang.getRange();
        } catch (Throwable e) {
            return null;
        }
        return null;
    }
}
