package de.komoot.photon.query;

import spark.Request;
import spark.utils.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * Resolver for the response language for a web request.
 */
public class RequestLanguageResolver {
    static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";

    private final List<String> supportedLanguages;
    private final String defaultLanguage;

    public RequestLanguageResolver(List<String> supportedLanguages, String defaultLanguage) {
        this.supportedLanguages = supportedLanguages;
        this.defaultLanguage = defaultLanguage;
    }

    /**
     * Get the language to use for the response to the given request.
     *
     * @param webRequest Incoming HTTP request.
     * @return Language to be used in the response.
     * @throws BadRequestException The language in the request parameter is unknown.
     *
     * The function first checks for a 'lang' query parameter. If this is not given, it looks for
     * an Accept-Language header and tries to find a supported language there. If that does not
     * work either, the default language is returned
     */
    public String resolveRequestedLanguage(Request webRequest) throws BadRequestException {
        String language = webRequest.queryParams("lang");
        if (StringUtils.isBlank(language)) {
            language = fallbackLanguageFromHeaders(webRequest);
            if (StringUtils.isBlank(language))
                language = defaultLanguage;
        } else {
            checkLanguageSupported(language);
        }

        return language;
    }

    /**
     * Look for a language parameter in the request headers.
     *
     * @param webRequest Incoming HTTP request.
     * @return A suitable language header or null if not could be found.
     */
    private String fallbackLanguageFromHeaders(Request webRequest) {
        String acceptLanguageHeader = webRequest.headers(ACCEPT_LANGUAGE_HEADER);
        if (StringUtils.isBlank(acceptLanguageHeader))
            return null;

        try {
            List<Locale.LanguageRange> languages = Locale.LanguageRange.parse(acceptLanguageHeader);
            return Locale.lookupTag(languages, supportedLanguages);
        } catch (Throwable e) {
        }

        return null;
    }

    /**
     * Check that the language is a supported one.
     *
     * @param lang Language to use.
     * @throws BadRequestException The language is not in the list of supported languages.
     */
    private void checkLanguageSupported(String lang) throws BadRequestException {
        if (!("default".equals(lang) || supportedLanguages.contains((lang)))) {
            throw new BadRequestException(400, "language " + lang + " is not supported, supported languages are: default, " + String.join(", ", supportedLanguages));
        }
    }
}
