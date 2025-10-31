package de.komoot.photon.metrics;

import de.komoot.photon.CommandLineArgs;
import org.junit.jupiter.api.Test;

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

        MetricsConfig metricsConfig = MetricsConfig.setupMetrics(args);
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

        MetricsConfig metricsConfig = MetricsConfig.setupMetrics(args);
        assertThrows(IllegalStateException.class, metricsConfig::getRegistry);
        assertThrows(IllegalStateException.class, metricsConfig::getPlugin);
        assertFalse(metricsConfig.isEnabled());
    }

}