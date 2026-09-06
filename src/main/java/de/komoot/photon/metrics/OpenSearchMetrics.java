package de.komoot.photon.metrics;

import de.komoot.photon.opensearch.PhotonIndex;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.HealthStatus;

@NullMarked
public class OpenSearchMetrics implements MeterBinder {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final long CACHE_TTL_MS = 30_000;

    private final OpenSearchClient client;
    @Nullable private volatile CachedStats cache;

    public OpenSearchMetrics(OpenSearchClient client) {
        this.client = client;
    }

    private record CachedStats(
            long timestamp,
            double documentCount,
            double indexSizeBytes,
            double searchTotal,
            double searchTimeMillis,
            double indexingTotal,
            double indexingTimeMillis,
            double activeShards,
            double relocatingShards,
            double unassignedShards,
            double healthStatus
    ) {

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("opensearch.documents.count", client, this::getDocumentCount)
                .description("Documents in the Photon index")
                .tag("index", PhotonIndex.NAME).register(registry);
        Gauge.builder("opensearch.index.size.bytes", client, this::getIndexSizeBytes)
                .description("Store size of the Photon index primaries")
                .tag("index", PhotonIndex.NAME).baseUnit("bytes").register(registry);
        Gauge.builder("opensearch.search", client, this::getSearchTotal)
                .description("Search queries served by the Photon index primaries")
                .tag("index", PhotonIndex.NAME).register(registry);
        Gauge.builder("opensearch.search.time.millis", client, this::getSearchTimeMillis)
                .description("Cumulative time spent serving search queries")
                .tag("index", PhotonIndex.NAME).baseUnit("milliseconds").register(registry);
        Gauge.builder("opensearch.indexing", client, this::getIndexingTotal)
                .description("Documents indexed by the Photon index primaries")
                .tag("index", PhotonIndex.NAME).register(registry);
        Gauge.builder("opensearch.indexing.time.millis", client, this::getIndexingTimeMillis)
                .description("Cumulative time spent indexing documents")
                .tag("index", PhotonIndex.NAME).baseUnit("milliseconds").register(registry);
        Gauge.builder("opensearch.cluster.shards.active", client, this::getActiveShards)
                .description("Active shards in the cluster").register(registry);
        Gauge.builder("opensearch.cluster.shards.relocating", client, this::getRelocatingShards)
                .description("Relocating shards in the cluster").register(registry);
        Gauge.builder("opensearch.cluster.shards.unassigned", client, this::getUnassignedShards)
                .description("Unassigned shards in the cluster").register(registry);
        Gauge.builder("opensearch.cluster.health.status", client, this::getHealthStatus)
                .description("Cluster health status: 2 = green, 1 = yellow, 0 = red").register(registry);
    }

    private CachedStats getCache() {
        CachedStats current = cache;
        if (current == null || current.isExpired()) {
            synchronized (this) {
                current = cache;
                if (current == null || current.isExpired()) {
                    refreshCache();
                    current = cache;
                }
            }
        }
        return current != null ? current : createEmptyCache();
    }

    private CachedStats createEmptyCache() {
        return new CachedStats(System.currentTimeMillis(), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }

    private void refreshCache() {
        double documentCount = 0, indexSizeBytes = 0, searchTotal = 0, searchTimeMillis = 0;
        double indexingTotal = 0, indexingTimeMillis = 0, activeShards = 0, relocatingShards = 0;
        double unassignedShards = 0, healthStatus = 0;

        try {
            documentCount = client.count(c -> c.index(PhotonIndex.NAME)).count();
            var stats = client.indices().stats(s -> s.index(PhotonIndex.NAME)).indices().get(PhotonIndex.NAME);
            if (stats != null) {
                if (stats.primaries().store() != null) {
                    indexSizeBytes = stats.primaries().store().sizeInBytes();
                }
                if (stats.primaries().search() != null) {
                    searchTotal = stats.primaries().search().queryTotal();
                    searchTimeMillis = stats.primaries().search().queryTimeInMillis();
                }
                if (stats.primaries().indexing() != null) {
                    indexingTotal = stats.primaries().indexing().indexTotal();
                    indexingTimeMillis = stats.primaries().indexing().indexTimeInMillis();
                }
            }
            var health = client.cluster().health();
            activeShards = health.activeShards();
            relocatingShards = health.relocatingShards();
            unassignedShards = health.unassignedShards();
            // Higher is healthier. This is the inverse of the elasticsearch_exporter convention,
            // so the encoding is spelled out in the gauge description exposed as the HELP text.
            healthStatus = health.status() == HealthStatus.Green ? 2 : health.status() == HealthStatus.Yellow ? 1 : 0;
        } catch (Exception e) {
            LOGGER.warn("Failed to refresh cache", e);
        }

        cache = new CachedStats(System.currentTimeMillis(), documentCount, indexSizeBytes, searchTotal,
                searchTimeMillis, indexingTotal, indexingTimeMillis, activeShards, relocatingShards,
                unassignedShards, healthStatus);
    }

    private double getDocumentCount(OpenSearchClient client) {
        return getCache().documentCount;
    }

    private double getIndexSizeBytes(OpenSearchClient client) {
        return getCache().indexSizeBytes;
    }

    private double getSearchTotal(OpenSearchClient client) {
        return getCache().searchTotal;
    }

    private double getSearchTimeMillis(OpenSearchClient client) {
        return getCache().searchTimeMillis;
    }

    private double getIndexingTotal(OpenSearchClient client) {
        return getCache().indexingTotal;
    }

    private double getIndexingTimeMillis(OpenSearchClient client) {
        return getCache().indexingTimeMillis;
    }

    private double getActiveShards(OpenSearchClient client) {
        return getCache().activeShards;
    }

    private double getRelocatingShards(OpenSearchClient client) {
        return getCache().relocatingShards;
    }

    private double getUnassignedShards(OpenSearchClient client) {
        return getCache().unassignedShards;
    }

    private double getHealthStatus(OpenSearchClient client) {
        return getCache().healthStatus;
    }
}

