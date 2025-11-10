package de.komoot.photon.metrics;

import de.komoot.photon.opensearch.PhotonIndex;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OpenSearchMetricsTest {

    @Test
    void testMetricsRegistration() {
        var client = mock(OpenSearchClient.class);
        var registry = new SimpleMeterRegistry();

        new OpenSearchMetrics(client).bindTo(registry);

        assertThat(registry.getMeters()).hasSize(10);
        assertThat(registry.get("opensearch.documents.count").tag("index", PhotonIndex.NAME).gauge()).isNotNull();
        assertThat(registry.get("opensearch.index.size.bytes").tag("index", PhotonIndex.NAME).gauge()).isNotNull();
        assertThat(registry.get("opensearch.search.total").tag("index", PhotonIndex.NAME).gauge()).isNotNull();
        assertThat(registry.get("opensearch.search.time.millis").tag("index", PhotonIndex.NAME).gauge()).isNotNull();
        assertThat(registry.get("opensearch.indexing.total").tag("index", PhotonIndex.NAME).gauge()).isNotNull();
        assertThat(registry.get("opensearch.indexing.time.millis").tag("index", PhotonIndex.NAME).gauge()).isNotNull();
        assertThat(registry.get("opensearch.cluster.shards.active").gauge()).isNotNull();
        assertThat(registry.get("opensearch.cluster.shards.relocating").gauge()).isNotNull();
        assertThat(registry.get("opensearch.cluster.shards.unassigned").gauge()).isNotNull();
        assertThat(registry.get("opensearch.cluster.health.status").gauge()).isNotNull();
    }

    @Test
    void testMetricsDoNotThrowWhenClientFails() {
        var client = mock(OpenSearchClient.class);
        var registry = new SimpleMeterRegistry();

        new OpenSearchMetrics(client).bindTo(registry);

        assertThat(registry.get("opensearch.documents.count").gauge().value()).isEqualTo(0.0);
    }
}

