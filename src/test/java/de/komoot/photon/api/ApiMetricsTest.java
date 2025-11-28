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
}
