package de.komoot.photon.opensearch;

import de.komoot.photon.config.PhotonDBConfig;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.jspecify.annotations.NullMarked;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5Transport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

/**
 * Builds a configured {@link ApacheHttpClient5Transport} for connecting to an
 * external OpenSearch cluster with optional basic authentication and SSL/TLS.
 */
@NullMarked
public class OpenSearchTransportBuilder {
    private final PhotonDBConfig config;
    private final JacksonJsonpMapper mapper;

    public OpenSearchTransportBuilder(PhotonDBConfig config, JacksonJsonpMapper mapper) {
        this.config = config;
        this.mapper = mapper;
    }

    /**
     * Build a transport configured with the authentication and SSL settings
     * from the photon database configuration.
     */
    public ApacheHttpClient5Transport build() throws GeneralSecurityException, IOException {
        final var hosts = buildHosts();
        final var builder = ApacheHttpClient5TransportBuilder.builder(hosts).setMapper(mapper);

        if (config.isOpensearchSsl() || config.hasAuthentication()) {
            final var sslContext = config.isOpensearchSsl() ? buildSslContext() : null;

            builder.setHttpClientConfigCallback(httpClientBuilder -> {
                if (config.hasAuthentication()) {
                    final var credentialsProvider = new BasicCredentialsProvider();
                    for (var host : hosts) {
                        credentialsProvider.setCredentials(
                                new AuthScope(host),
                                new UsernamePasswordCredentials(
                                        config.getOpensearchUser(),
                                        config.getOpensearchPassword().toCharArray()));
                    }
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                }

                if (sslContext != null) {
                    final var tlsStrategy = ClientTlsStrategyBuilder.create()
                            .setSslContext(sslContext)
                            .buildAsync();
                    final var cm = PoolingAsyncClientConnectionManagerBuilder.create()
                            .setTlsStrategy(tlsStrategy).build();
                    httpClientBuilder.setConnectionManager(cm);
                }

                return httpClientBuilder;
            });
        }

        return builder.build();
    }

    HttpHost[] buildHosts() {
        final var scheme = config.isOpensearchSsl() ? "https" : "http";
        return config.getTransportAddresses().stream()
                .map(addr -> addr.split(":", 2))
                .map(parts -> new HttpHost(scheme, parts[0],
                        parts.length > 1 ? Integer.parseInt(parts[1]) : 9201))
                .toArray(HttpHost[]::new);
    }

    SSLContext buildSslContext() throws GeneralSecurityException, IOException {
        final var builder = SSLContextBuilder.create();

        if (config.getOpensearchTruststore() != null) {
            final var truststore = KeyStore.getInstance("PKCS12");
            try (var in = new FileInputStream(config.getOpensearchTruststore())) {
                final var pw = config.getOpensearchTruststorePassword();
                truststore.load(in, pw != null ? pw.toCharArray() : null);
            }
            builder.loadTrustMaterial(truststore, null);
        }

        if (config.getOpensearchKeystore() != null) {
            final var keystore = KeyStore.getInstance("PKCS12");
            try (var in = new FileInputStream(config.getOpensearchKeystore())) {
                final var pw = config.getOpensearchKeystorePassword();
                keystore.load(in, pw != null ? pw.toCharArray() : null);
            }
            final var pw = config.getOpensearchKeystorePassword();
            builder.loadKeyMaterial(keystore, pw != null ? pw.toCharArray() : null);
        }

        return builder.build();
    }
}
