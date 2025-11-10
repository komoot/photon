package de.komoot.photon.metrics;

import de.komoot.photon.CommandLineArgs;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class MetricsConfigTest {
    @Test
    void testInit() {
        CommandLineArgs args = new CommandLineArgs() {
            @Override
            public String getMetricsEnable() {
                return "prometheus";
            }
        };
        var client = mock(OpenSearchClient.class);

        MetricsConfig metricsConfig = MetricsConfig.setupMetrics(args, client);
        assertNotNull(metricsConfig.getRegistry());
        assertNotNull(metricsConfig.getPlugin());
        assertTrue(metricsConfig.isEnabled());
    }

    @Test
    void testNoInit() {
        CommandLineArgs args = new CommandLineArgs() {
            @Override
            public String getMetricsEnable() {
                return "";
            }
        };
        var client = mock(OpenSearchClient.class);

        MetricsConfig metricsConfig = MetricsConfig.setupMetrics(args, client);
        assertThrows(IllegalStateException.class, metricsConfig::getRegistry);
        assertThrows(IllegalStateException.class, metricsConfig::getPlugin);
        assertFalse(metricsConfig.isEnabled());
    }

}