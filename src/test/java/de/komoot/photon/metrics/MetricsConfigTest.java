package de.komoot.photon.metrics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.opensearch.client.opensearch.OpenSearchClient;

import static org.junit.jupiter.api.Assertions.*;

class MetricsConfigTest {
    @Test
    void testInit() {
        OpenSearchClient openSearchClient = new OpenSearchClient(null) {};

        MetricsConfig metricsConfig = MetricsConfig.setupMetrics("prometheus", openSearchClient);
        assertNotNull(metricsConfig.getRegistry());
        assertNotNull(metricsConfig.getPlugin());
        assertTrue(metricsConfig.isEnabled());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testNoInit(String metricsType) {
        OpenSearchClient openSearchClient = new OpenSearchClient(null) {};

        MetricsConfig metricsConfig = MetricsConfig.setupMetrics(metricsType, openSearchClient);
        assertThrows(IllegalStateException.class, metricsConfig::getRegistry);
        assertThrows(IllegalStateException.class, metricsConfig::getPlugin);
        assertFalse(metricsConfig.isEnabled());
    }

}