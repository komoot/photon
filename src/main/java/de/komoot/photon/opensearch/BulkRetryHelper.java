package de.komoot.photon.opensearch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.transport.httpclient5.ResponseException;

import java.io.IOException;

/**
 * Sends OpenSearch bulk requests with exponential-backoff retries on HTTP 429 — the
 * status OpenSearch returns when the parent circuit breaker trips during heavy writes.
 */
@NullMarked
final class BulkRetryHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    /** Initial backoff before the first retry; doubled on each subsequent attempt. */
    private static final long INITIAL_RETRY_DELAY_MS = 500L;

    /** Maximum retry attempts after the initial request. ~64 s backoff on the last try. */
    private static final int MAX_BULK_RETRIES = 8;

    /** Cap on per-item error log lines per bulk to avoid log flooding. */
    private static final int ERROR_LOG_LIMIT_PER_BULK = 10;

    private BulkRetryHelper() {
    }

    /**
     * Send the bulk request, retrying on HTTP 429 with exponential backoff.
     * @throws RuntimeException on per-item errors, non-429 transport failures, or retry exhaustion.
     */
    static void sendWithRetry(OpenSearchClient client, BulkRequest request) {
        final int opCount = request.operations().size();
        IOException lastException = null;
        for (int attempt = 0; attempt <= MAX_BULK_RETRIES; attempt++) {
            if (attempt > 0) {
                sleepBackoff(attempt, opCount);
            }
            try {
                final BulkResponse resp = client.bulk(request);
                if (resp.errors()) {
                    throw new RuntimeException(logAndCountItemErrors(resp) + " failed items in bulk");
                }
                if (attempt > 0) {
                    LOGGER.info("Bulk retry succeeded on attempt {}/{}", attempt, MAX_BULK_RETRIES);
                }
                return;
            } catch (IOException e) {
                if (!(e instanceof ResponseException re && re.status() == 429)) {
                    throw new RuntimeException("Bulk request failed", e);
                }
                lastException = e;
            }
        }
        throw new RuntimeException("Bulk retries exhausted after " + MAX_BULK_RETRIES
                + " attempts (" + opCount + " operations)", lastException);
    }

    private static void sleepBackoff(int attempt, int opCount) {
        final long delayMs = INITIAL_RETRY_DELAY_MS * (1L << (attempt - 1));
        LOGGER.warn("Bulk rejected (429); retrying {} operations in {} ms (attempt {}/{})",
                opCount, delayMs, attempt, MAX_BULK_RETRIES);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during retry backoff", e);
        }
    }

    private static int logAndCountItemErrors(BulkResponse response) {
        int count = 0;
        for (BulkResponseItem item : response.items()) {
            if (item.error() != null) {
                if (count++ < ERROR_LOG_LIMIT_PER_BULK) {
                    LOGGER.error("Bulk item error: {}", item.toJsonString());
                }
            }
        }
        return count;
    }
}
