package de.komoot.photon.metrics;

import io.javalin.micrometer.MicrometerPlugin;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch.OpenSearchClient;

@NullMarked
public class MetricsConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    @Nullable private MicrometerPlugin micrometerPlugin;
    @Nullable private PrometheusMeterRegistry registry;
    private final String path = "/metrics";

    private MetricsConfig() {
    }

    private void init(OpenSearchClient client) {
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.config().commonTags("application", "Photon");
        registry.config().meterFilter(new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (id.getName().equals("jetty.server.requests")) {
                    return DistributionStatisticConfig.builder()
                            .percentilesHistogram(true)
                            .build()
                            .merge(config);
                }
                return config;
            }
        });
        registerJvmMetrics(registry);
        registerOpenSearchMetrics(registry, client);
        micrometerPlugin = new MicrometerPlugin(micrometerPluginConfig -> micrometerPluginConfig.registry = registry);
        LOGGER.info("Metrics enabled at " + path);
    }

    private void registerJvmMetrics(MeterRegistry registry) {
        new ClassLoaderMetrics().bindTo(registry);
        new FileDescriptorMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        new UptimeMetrics().bindTo(registry);
    }

    private void registerOpenSearchMetrics(MeterRegistry registry, OpenSearchClient client) {
        new OpenSearchMetrics(client).bindTo(registry);
    }

    public MicrometerPlugin getPlugin() {
        if (micrometerPlugin == null) {
            throw new IllegalStateException("MetricsConfig not initialized.");
        }
        return micrometerPlugin;
    }

    public PrometheusMeterRegistry getRegistry() {
        if (registry == null) {
            throw new IllegalStateException("PrometheusMeterRegistry not initialized.");
        }
        return registry;
    }

    public String getPath() {
        return path;
    }

    public boolean isEnabled() {
        return registry != null && micrometerPlugin != null;
    }

    public static MetricsConfig setupMetrics(String metricsType, OpenSearchClient client) {
        MetricsConfig metricsConfig = new MetricsConfig();
        if ("prometheus".equalsIgnoreCase(metricsType)) {
            metricsConfig.init(client);
        }
        return metricsConfig;
    }
}
