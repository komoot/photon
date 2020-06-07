package de.komoot.photon.query;

import com.google.common.base.Joiner;
import lombok.AllArgsConstructor;
import spark.Request;
import spark.utils.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@AllArgsConstructor
public class RequestLanguageResolver {
    static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";
    static final String DEFAULT_LANGUAGE = "en";

    private final Set<String> supportedLanguages;

    public String resolveRequestedLanguage(Request webRequest) throws BadRequestException {
        String language = webRequest.queryParams("lang");
        if (StringUtils.isBlank(language)) {
            language = fallbackLanguageFromHeaders(webRequest);
            if (StringUtils.isBlank(language))
                language = DEFAULT_LANGUAGE;
        } else {
            isLanguageSupported(language);
        }

        return language;
    }

    private String fallbackLanguageFromHeaders(Request webRequest) {
        String acceptLanguageHeader = webRequest.headers(ACCEPT_LANGUAGE_HEADER);
        if (StringUtils.isBlank(acceptLanguageHeader))
            return null;
        try {
            List<Locale.LanguageRange> languages = Locale.LanguageRange.parse(acceptLanguageHeader);
            for (Locale.LanguageRange lang : languages)
                if (supportedLanguages.contains(lang.getRange()))
                    return lang.getRange();
        } catch (Throwable e) {
            return null;
        }
        return null;
    }

    private void isLanguageSupported(String lang) throws BadRequestException {
        if (!supportedLanguages.contains((lang))) {
            throw new BadRequestException(400, "language " + lang + " is not supported, supported languages are: " + Joiner.on(", ").join(supportedLanguages));
        }
    }
}
