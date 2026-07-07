package de.komoot.photon.api;

import de.komoot.photon.App;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiMetricsTest extends ApiBaseTester {

    @BeforeAll
    void setUp(@TempDir Path dataDirectory) throws Exception {
        setUpES(dataDirectory);
        refresh();
    }

    @AfterEach
    void shutdown() {
        App.shutdown();
    }

    @AfterAll
    @Override
    public void tearDown() {
        shutdownES();
    }

    @Test
    void testMetricsEndpointReturnsOpenSearchMetrics() throws Exception {
        startAPI("-metrics-enable", "prometheus");

        String metrics = readURL("/metrics");

        // Check for OpenSearch index metrics
        assertThat(metrics).contains("opensearch_documents_count{");
        assertThat(metrics).contains("opensearch_index_size_bytes{");
        assertThat(metrics).contains("opensearch_search{");
        assertThat(metrics).contains("opensearch_search_time_millis_milliseconds{");
        assertThat(metrics).contains("opensearch_indexing{");
        assertThat(metrics).contains("opensearch_indexing_time_millis_milliseconds{");

        // Check for OpenSearch cluster metrics
        assertThat(metrics).contains("opensearch_cluster_shards_active{");
        assertThat(metrics).contains("opensearch_cluster_shards_relocating{");
        assertThat(metrics).contains("opensearch_cluster_shards_unassigned{");
        assertThat(metrics).contains("opensearch_cluster_health_status{");

        // Check for index tag on index metrics
        assertThat(metrics).contains("index=\"photon\"");

        // Check for JVM metrics
        assertThat(metrics).contains("jvm_memory");
        assertThat(metrics).contains("jvm_gc");
        assertThat(metrics).contains("jvm_threads");
    }

    @Test
    void testMetricsEndpointReturns404WhenDisabled() throws Exception {
        startAPI();

        var conn = connect("/metrics");
        assertThat(conn.getResponseCode()).isEqualTo(404);
    }

    /**
     * Regression test for the missing HTTP request-latency histogram buckets: Javalin's
     * MicrometerPlugin records request timings under the "http.server.requests" meter, and the
     * metrics filter must enable percentile histograms for it. Without the buckets (and their "le"
     * label) any Grafana panel using histogram_quantile() for p95/p99 latency returns nothing.
     * A future rename of the emitted meter makes this fail loudly instead of silently dropping
     * the buckets.
     */
    @Test
    void testMetricsEndpointExposesHttpRequestLatencyHistogram() throws Exception {
        startAPI("-metrics-enable", "prometheus");

        // Drive real requests so the http.server.requests timer records observations before scraping.
        readURL("/api?q=berlin");
        readURL("/reverse?lat=52.54714&lon=13.39026");

        String metrics = readURL("/metrics");

        // A bucket line carrying an "le" label is what histogram_quantile() consumes.
        assertThat(metrics).containsPattern("http_server_requests_seconds_bucket\\{[^}]*le=\"");

        // The summary series must still be exposed alongside the buckets.
        assertThat(metrics).contains("http_server_requests_seconds_count");
        assertThat(metrics).contains("http_server_requests_seconds_sum");
    }
}
