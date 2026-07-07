package de.komoot.photon.metrics;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.opensearch.client.opensearch.OpenSearchClient;

import java.time.Duration;

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

    /**
     * The HTTP request-latency timer that Javalin's MicrometerPlugin registers is named
     * "http.server.requests". The meter filter must enable percentile histograms for it so that
     * Prometheus gets "_bucket" series with an "le" label, which histogram_quantile() (p95/p99)
     * depends on. This guards the name the filter matches against accidental edits; the end-to-end
     * guard against Javalin renaming the emitted meter lives in ApiMetricsTest.
     */
    @Test
    void testHttpServerRequestsTimerExposesHistogramBuckets() {
        OpenSearchClient openSearchClient = new OpenSearchClient(null) {};
        MetricsConfig metricsConfig = MetricsConfig.setupMetrics("prometheus", openSearchClient);
        PrometheusMeterRegistry registry = metricsConfig.getRegistry();

        // Mirror the timer emitted by io.javalin.micrometer.MicrometerPlugin for each request.
        registry.timer("http.server.requests", "method", "GET", "uri", "/api", "status", "200")
                .record(Duration.ofMillis(5));

        String scrape = registry.scrape();

        assertTrue(scrape.contains("http_server_requests_seconds_bucket"),
                "expected histogram buckets for http.server.requests, got:\n" + scrape);
        assertTrue(scrape.contains("le=\""),
                "expected an 'le' label on the histogram buckets, got:\n" + scrape);
        assertTrue(scrape.contains("http_server_requests_seconds_count"),
                "expected _count series to remain present, got:\n" + scrape);
        assertTrue(scrape.contains("http_server_requests_seconds_sum"),
                "expected _sum series to remain present, got:\n" + scrape);
    }

    /**
     * The histogram must stay scoped to the request timer. Enabling percentile histograms globally
     * would add high-cardinality buckets to unrelated timers (JVM GC pauses, OpenSearch client
     * timers, etc.), so an arbitrary timer must not get "_bucket" series.
     */
    @Test
    void testUnrelatedTimerDoesNotExposeHistogramBuckets() {
        OpenSearchClient openSearchClient = new OpenSearchClient(null) {};
        MetricsConfig metricsConfig = MetricsConfig.setupMetrics("prometheus", openSearchClient);
        PrometheusMeterRegistry registry = metricsConfig.getRegistry();

        registry.timer("some.unrelated.timer").record(Duration.ofMillis(5));

        String scrape = registry.scrape();

        assertFalse(scrape.contains("some_unrelated_timer_seconds_bucket"),
                "unrelated timers must not get histogram buckets, got:\n" + scrape);
    }
}
