package de.komoot.photon.opensearch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ResponseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

@NullMarked
class BulkRequestPool {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int MAX_BULK_RETRIES = 4;
    private static final long INITIAL_RETRY_DELAY_MS = 500L;
    private static final int ERROR_LOG_LIMIT_PER_BULK = 10;

    private final OpenSearchAsyncClient client;

    private final List<@Nullable CompletableFuture<BulkResponse>> bulks;
    private final BlockingQueue<Integer> doneBulks;

    public BulkRequestPool(OpenSearchTransport transport, int size) {
        this.client = new OpenSearchAsyncClient(transport);

        this.bulks = new ArrayList<>(size);
        this.doneBulks = new ArrayBlockingQueue<>(size);

        for (int i = 0; i < size; ++i) {
            bulks.add(null);
            doneBulks.add(i);
        }
    }

    public void submit(BulkRequest request) {
        try {
            int slot = finishNextSlot();
            sendWithRetry(slot, request, 0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread was interrupted.");
        }
    }

    public void drain() {
        boolean interrupted = false;
        Exception error = null;

        while (bulks.stream().anyMatch(Objects::nonNull)) {
            try {
                finishNextSlot();
            } catch (InterruptedException e) {
                interrupted = true;
            } catch (Exception e) {
                error = e;
            }
        }

        // Updater will continue using the pool after draining.
        doneBulks.clear();
        for (int i = 0; i < bulks.size(); ++i) {
            doneBulks.add(i);
        }


        if (interrupted) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread was interrupted.");
        }

        if (error != null) {
            throw new RuntimeException("Error processing final bulks", error);
        }
    }

    private int finishNextSlot() throws InterruptedException {
        int slot = doneBulks.take();
        var prevRequest = bulks.set(slot, null);
        if (prevRequest != null) {
            BulkResponse response;
            try {
                response = prevRequest.get();
            } catch (ExecutionException e) {
                throw new RuntimeException("Error inserting batch.", e);
            }
            if (response.errors()) {
                throw new RuntimeException(logAndCountItemErrors(response) + " failed items in bulk");
            }
        }

        return slot;
    }

    private boolean sendWithRetry(int slot, BulkRequest bulk, int retry) {
        try {
            var request = client.bulk(bulk).whenComplete((b, t) -> {
                if (retry < MAX_BULK_RETRIES && t != null && isRepeatableBulkOp(t)) {
                    long delayMs = INITIAL_RETRY_DELAY_MS * (1L << retry);
                    LOGGER.warn("Bulk rejected (429); retrying in {} ms (attempt {}/{})",
                            delayMs, retry + 1, MAX_BULK_RETRIES);

                    CompletableFuture.supplyAsync(() -> sendWithRetry(slot, bulk, retry + 1),
                            CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS))
                            .exceptionally(e -> doneBulks.add(slot));
                } else {
                    doneBulks.add(slot);
                }
            });
            bulks.set(slot, request);
        } catch (IOException e) {
            throw new RuntimeException("Error submitting bulk request to DB.", e);
        }

        return true;
    }

    private boolean isRepeatableBulkOp(Throwable t) {
        if (t instanceof CompletionException) {
            t = t.getCause();
        }

        if (t instanceof ResponseException) {
            return ((ResponseException) t).status() == 429;
        }

        if (t instanceof OpenSearchException) {
            return ((OpenSearchException) t).status() == 429;
        }

        return false;
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
