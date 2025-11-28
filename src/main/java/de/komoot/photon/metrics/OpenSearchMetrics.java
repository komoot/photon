package de.komoot.photon.metrics;

import de.komoot.photon.opensearch.PhotonIndex;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.HealthStatus;

public class OpenSearchMetrics implements MeterBinder {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final long CACHE_TTL_MS = 30_000;

    private final OpenSearchClient client;
    private volatile CachedStats cache;

    public OpenSearchMetrics(@NotNull OpenSearchClient client) {
        this.client = client;
    }

    private static class CachedStats {
        final long timestamp;
        final double documentCount;
        final double indexSizeBytes;
        final double searchTotal;
        final double searchTimeMillis;
        final double indexingTotal;
        final double indexingTimeMillis;
        final double activeShards;
        final double relocatingShards;
        final double unassignedShards;
        final double healthStatus;

        CachedStats(long timestamp, double documentCount, double indexSizeBytes, double searchTotal,
                    double searchTimeMillis, double indexingTotal, double indexingTimeMillis,
                    double activeShards, double relocatingShards, double unassignedShards, double healthStatus) {
            this.timestamp = timestamp;
            this.documentCount = documentCount;
            this.indexSizeBytes = indexSizeBytes;
            this.searchTotal = searchTotal;
            this.searchTimeMillis = searchTimeMillis;
            this.indexingTotal = indexingTotal;
            this.indexingTimeMillis = indexingTimeMillis;
            this.activeShards = activeShards;
            this.relocatingShards = relocatingShards;
            this.unassignedShards = unassignedShards;
            this.healthStatus = healthStatus;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    @Override
    public void bindTo(@NotNull MeterRegistry registry) {
        Gauge.builder("opensearch.documents.count", client, this::getDocumentCount)
                .tag("index", PhotonIndex.NAME).register(registry);
        Gauge.builder("opensearch.index.size.bytes", client, this::getIndexSizeBytes)
                .tag("index", PhotonIndex.NAME).baseUnit("bytes").register(registry);
        Gauge.builder("opensearch.search", client, this::getSearchTotal)
                .tag("index", PhotonIndex.NAME).register(registry);
        Gauge.builder("opensearch.search.time.millis", client, this::getSearchTimeMillis)
                .tag("index", PhotonIndex.NAME).baseUnit("milliseconds").register(registry);
        Gauge.builder("opensearch.indexing", client, this::getIndexingTotal)
                .tag("index", PhotonIndex.NAME).register(registry);
        Gauge.builder("opensearch.indexing.time.millis", client, this::getIndexingTimeMillis)
                .tag("index", PhotonIndex.NAME).baseUnit("milliseconds").register(registry);
        Gauge.builder("opensearch.cluster.shards.active", client, this::getActiveShards).register(registry);
        Gauge.builder("opensearch.cluster.shards.relocating", client, this::getRelocatingShards).register(registry);
        Gauge.builder("opensearch.cluster.shards.unassigned", client, this::getUnassignedShards).register(registry);
        Gauge.builder("opensearch.cluster.health.status", client, this::getHealthStatus).register(registry);
    }

    @NotNull
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

    @NotNull
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

