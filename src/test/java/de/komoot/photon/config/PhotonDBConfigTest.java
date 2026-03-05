package de.komoot.photon.config;

import com.beust.jcommander.JCommander;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PhotonDBConfigTest {

    @Test
    void testParseAllOpenSearchAuthParameters() {
        final var config = new PhotonDBConfig();

        JCommander.newBuilder().addObject(config).build().parse(
                "-opensearch-user", "myuser",
                "-opensearch-password", "mypass",
                "-opensearch-ssl",
                "-opensearch-truststore", "/path/to/truststore.p12",
                "-opensearch-truststore-password", "truststorepass",
                "-opensearch-keystore", "/path/to/keystore.p12",
                "-opensearch-keystore-password", "keystorepass"
        );

        assertThat(config.getOpensearchUser()).isEqualTo("myuser");
        assertThat(config.getOpensearchPassword()).isEqualTo("mypass");
        assertThat(config.isOpensearchSsl()).isTrue();
        assertThat(config.getOpensearchTruststore()).isEqualTo("/path/to/truststore.p12");
        assertThat(config.getOpensearchTruststorePassword()).isEqualTo("truststorepass");
        assertThat(config.getOpensearchKeystore()).isEqualTo("/path/to/keystore.p12");
        assertThat(config.getOpensearchKeystorePassword()).isEqualTo("keystorepass");
        assertThat(config.hasAuthentication()).isTrue();
    }

    @Test
    void testDefaultsWhenNoAuthParametersProvided() {
        final var config = new PhotonDBConfig();

        JCommander.newBuilder().addObject(config).build().parse();

        assertThat(config.getOpensearchUser()).isNull();
        assertThat(config.getOpensearchPassword()).isNull();
        assertThat(config.isOpensearchSsl()).isFalse();
        assertThat(config.getOpensearchTruststore()).isNull();
        assertThat(config.getOpensearchTruststorePassword()).isNull();
        assertThat(config.getOpensearchKeystore()).isNull();
        assertThat(config.getOpensearchKeystorePassword()).isNull();
        assertThat(config.hasAuthentication()).isFalse();
    }
}
