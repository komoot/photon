package de.komoot.photon.opensearch;

import de.komoot.photon.ReflectionTestUtil;
import de.komoot.photon.config.PhotonDBConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class OpenSearchTransportBuilderTest {

    private static String truststorePath;
    private static final String TRUSTSTORE_PASSWORD = "testpass";

    @BeforeAll
    static void createTestTruststore(@TempDir Path tempDir) throws Exception {
        truststorePath = tempDir.resolve("test-truststore.p12").toString();

        final var ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        try (var out = new FileOutputStream(truststorePath)) {
            ks.store(out, TRUSTSTORE_PASSWORD.toCharArray());
        }
    }

    private PhotonDBConfig createConfig(List<String> addresses, boolean ssl,
                                        String truststore, String truststorePassword) {
        final var config = new PhotonDBConfig(".", "photon", addresses);
        if (ssl) {
            ReflectionTestUtil.setFieldValue(config, PhotonDBConfig.class, "opensearchSsl", true);
        }
        if (truststore != null) {
            ReflectionTestUtil.setFieldValue(config, PhotonDBConfig.class, "opensearchTruststore", truststore);
        }
        if (truststorePassword != null) {
            ReflectionTestUtil.setFieldValue(config, PhotonDBConfig.class, "opensearchTruststorePassword", truststorePassword);
        }
        return config;
    }

    @Test
    void testBuildHostsParsingAndScheme() {
        final var httpConfig = new PhotonDBConfig(".", "photon", List.of("node1:9200", "node2"));
        final var httpHosts = new OpenSearchTransportBuilder(httpConfig, new JacksonJsonpMapper()).buildHosts();

        assertThat(httpHosts).hasSize(2);
        assertThat(httpHosts[0].getSchemeName()).isEqualTo("http");
        assertThat(httpHosts[0].getHostName()).isEqualTo("node1");
        assertThat(httpHosts[0].getPort()).isEqualTo(9200);
        assertThat(httpHosts[1].getPort()).isEqualTo(9201); // default port

        final var sslConfig = createConfig(List.of("secure.example.com:9200"), true, null, null);
        final var sslHosts = new OpenSearchTransportBuilder(sslConfig, new JacksonJsonpMapper()).buildHosts();

        assertThat(sslHosts[0].getSchemeName()).isEqualTo("https");
    }

    @Test
    void testBuildSslContextWithTruststore() throws Exception {
        final var config = createConfig(List.of("localhost:9200"), true,
                truststorePath, TRUSTSTORE_PASSWORD);
        final var builder = new OpenSearchTransportBuilder(config, new JacksonJsonpMapper());

        final var sslContext = builder.buildSslContext();

        assertThat(sslContext).isNotNull();
        assertThat(sslContext.getProtocol()).isEqualTo("TLS");
    }

    @Test
    void testBuildWithAuthAndSsl() throws Exception {
        final var config = createConfig(List.of("localhost:9200"), true,
                truststorePath, TRUSTSTORE_PASSWORD);
        ReflectionTestUtil.setFieldValue(config, PhotonDBConfig.class, "opensearchUser", "admin");
        ReflectionTestUtil.setFieldValue(config, PhotonDBConfig.class, "opensearchPassword", "secret");
        final var builder = new OpenSearchTransportBuilder(config, new JacksonJsonpMapper());

        final var transport = builder.build();

        assertThat(transport).isNotNull();
        transport.close();
    }
}
