package de.komoot.photon.metrics;

import de.komoot.photon.CommandLineArgs;
import io.javalin.micrometer.MicrometerPlugin;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class MetricsConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    private MicrometerPlugin micrometerPlugin;
    private PrometheusMeterRegistry registry;
    private final String path = "/metrics";

    private MetricsConfig() {
    }

    private void init() {
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.config().commonTags("application", "Photon");
        registerJvmMetrics(registry);
        micrometerPlugin = new MicrometerPlugin(micrometerPluginConfig -> micrometerPluginConfig.registry = registry);
        LOGGER.info("Metrics enabled at " + path);
    }

    private void registerJvmMetrics(MeterRegistry registry) {
        new ClassLoaderMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
    }

    @NotNull
    public MicrometerPlugin getPlugin() {
        if (micrometerPlugin == null) {
            throw new IllegalStateException("MetricsConfig not initialized.");
        }
        return micrometerPlugin;
    }

    @NotNull
    public PrometheusMeterRegistry getRegistry() {
        if (registry == null) {
            throw new IllegalStateException("PrometheusMeterRegistry not initialized.");
        }
        return registry;
    }

    @NotNull
    public String getPath() {
        return path;
    }

    public boolean isEnabled() {
        return registry != null && micrometerPlugin != null;
    }

    @NotNull
    public static MetricsConfig setupMetrics(CommandLineArgs args) {
        MetricsConfig metricsConfig = new MetricsConfig();
        if (args.getMetricsEnable() != null && args.getMetricsEnable().equalsIgnoreCase("prometheus")) {
            metricsConfig.init();
        }
        return metricsConfig;
    }
}
