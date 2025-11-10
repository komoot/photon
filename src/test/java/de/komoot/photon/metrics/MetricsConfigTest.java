package de.komoot.photon.metrics;

import de.komoot.photon.CommandLineArgs;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;

import static org.junit.jupiter.api.Assertions.*;

class MetricsConfigTest {
    @Test
    void testInit() {
        CommandLineArgs args = new CommandLineArgs() {
            @Override
            public String getMetricsEnable() {
                return "prometheus";
            }
        };
        OpenSearchClient openSearchClient = new OpenSearchClient(null) {};

        MetricsConfig metricsConfig = MetricsConfig.setupMetrics(args, openSearchClient);
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
        OpenSearchClient openSearchClient = new OpenSearchClient(null) {};

        MetricsConfig metricsConfig = MetricsConfig.setupMetrics(args, openSearchClient);
        assertThrows(IllegalStateException.class, metricsConfig::getRegistry);
        assertThrows(IllegalStateException.class, metricsConfig::getPlugin);
        assertFalse(metricsConfig.isEnabled());
    }

}