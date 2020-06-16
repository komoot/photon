package de.komoot.photon.query;

import org.junit.Before;
import org.junit.Test;
import spark.Request;

import java.util.List;

import static de.komoot.photon.query.RequestLanguageResolver.ACCEPT_LANGUAGE_HEADER;
import static java.util.Arrays.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class RequestLanguageTest {
    // maybe could access directly from RequestLanguageResolver, but in case of change... whatever.
    private final List<String> supportedLangs = asList("en", "fr", "de");
    private RequestLanguageResolver languageResolver;

    private static final String DEFAULT_LANGUAGE = "en";

    @Before
    public void setup() {
        languageResolver = new RequestLanguageResolver(supportedLangs, DEFAULT_LANGUAGE);
    }

    @Test
    public void testDefaultLanguageFallback() {
        validateReturnedLanguage(null, null, DEFAULT_LANGUAGE);
    }

    @Test
    public void testValidQueryLangs()
    {
        supportedLangs.forEach(l -> validateReturnedLanguage(l, null, l));
        validateReturnedLanguage("default", null, "default");
    }

    @Test
    public void testLanguageNotSupported() {
        asList("ru", "pl", "xyaasdas").forEach(l -> validateNotSupported(l, null));
    }

    @Test
    public void testPriorityOfQueryParam() {
        validateReturnedLanguage("de", "en", "de");
        validateNotSupported("ko", "en");
    }

    @Test
    public void testFallbackOnQueryNotSetBasicHeader() {
        supportedLangs.forEach(l -> validateReturnedLanguage(null, l, l));
    }

    @Test
    public void testFallbackMatchingHeaderLang() {
        validateReturnedLanguage(null, "ru,pl;q=0.9,sp,fr;q=0.1", "fr");
        validateReturnedLanguage(null, "ru,pl;q=0.7,sp,de", "de");
        validateReturnedLanguage(null, "de-DE", "de");
        validateReturnedLanguage(null, "ru,pl;q=0.9,sp", DEFAULT_LANGUAGE);
    }

    @Test
    public void validateIgnoreInvalidAcceptLangHeader() {
        validateReturnedLanguage(null, "we loves cats", DEFAULT_LANGUAGE);
        validateReturnedLanguage(null, "cookies?", DEFAULT_LANGUAGE);
        validateReturnedLanguage(null, "Illegal/Header_", DEFAULT_LANGUAGE);
    }


    private Request buildRequest(String queryLang, String acceptLangHeader) {
        Request request = mock(Request.class);
        when(request.queryParams("lang")).thenReturn(queryLang);
        when(request.headers(ACCEPT_LANGUAGE_HEADER)).thenReturn(acceptLangHeader);
        return request;
    }

    private void validateReturnedLanguage(String queryLang, String acceptHeader, String expected) {
        Request req = buildRequest(queryLang, acceptHeader);
        try {
            String actual = languageResolver.resolveRequestedLanguage(req);
            assertEquals(expected, actual);
        } catch (BadRequestException e) {
            fail(e.getMessage());
        }
    }

    private void validateNotSupported(String queryLang, String acceptHeader) {
        Request req = buildRequest(queryLang, acceptHeader);
        try {
            languageResolver.resolveRequestedLanguage(req);
            fail("Language " + queryLang + " is not supported");
        } catch (BadRequestException ignored) {
        }
    }
}
