package de.komoot.photon;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppLanguageConfigTest {

    @Test
    void acceptsImportedFallbackLanguage() {
        var dbProperties = new DatabaseProperties().setLanguages(Set.of("en", "nl"));

        assertThatNoException()
                .isThrownBy(() -> App.validateFallbackLanguage(dbProperties, "en"));
    }

    @Test
    void acceptsDefaultAsFallbackLanguage() {
        var dbProperties = new DatabaseProperties().setLanguages(Set.of("en", "nl"));

        assertThatNoException()
                .isThrownBy(() -> App.validateFallbackLanguage(dbProperties, "default"));
    }

    @Test
    void rejectsFallbackLanguageThatWasNotImported() {
        var dbProperties = new DatabaseProperties().setLanguages(Set.of("en", "nl"));

        assertThatThrownBy(() -> App.validateFallbackLanguage(dbProperties, "fr"))
                .isInstanceOf(UsageException.class)
                .hasMessageContaining("Fallback language is not supported");
    }
}
