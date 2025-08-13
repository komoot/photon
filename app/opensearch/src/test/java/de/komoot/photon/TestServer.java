package de.komoot.photon;

import de.komoot.photon.opensearch.OpenSearchResult;
import de.komoot.photon.opensearch.PhotonIndex;
import de.komoot.photon.searcher.PhotonResult;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.opensearch.common.settings.Settings;

import java.io.IOException;

public class TestServer extends Server {
    public static final String TEST_CLUSTER_NAME = "photon-test";

    private OpenSearchRunner runner;
    private String instanceDir;

    public TestServer(String mainDirectory) {
        super(mainDirectory);

        instanceDir = mainDirectory;
    }

    public void startTestServer(String clusterName) throws IOException {
        runner = new OpenSearchRunner();
        runner.onBuild(new OpenSearchRunner.Builder() {
            @Override
            public void build(final int number, final Settings.Builder settingsBuilder) {
                settingsBuilder.put("http.cors.enabled", true);
                settingsBuilder.put("http.cors.allow-origin", "*");
                settingsBuilder.put("discovery.type", "single-node");
                settingsBuilder.putList("discovery.seed_hosts", "127.0.0.1:9201");
                settingsBuilder.put("logger.org.opensearch.cluster.metadata", "TRACE");
                settingsBuilder.put("cluster.search.request.slowlog.level", "TRACE");
                settingsBuilder.put("cluster.search.request.slowlog.threshold.warn", "0ms");
                settingsBuilder.put("cluster.search.request.slowlog.threshold.info", "0ms");
                settingsBuilder.put("cluster.search.request.slowlog.threshold.debug", "0ms");
                settingsBuilder.put("cluster.search.request.slowlog.threshold.trace", "0ms");

            }
            }).build(OpenSearchRunner.newConfigs()
                .basePath(instanceDir)
                .clusterName(clusterName)
                .numOfNode(1)
                .baseHttpPort(9200));

        // wait for yellow status
        runner.ensureYellow();

        String[] transportAddresses = {"127.0.0.1:" + runner.node().settings().get("http.port")};
        start(clusterName, transportAddresses);
    }

    public void stopTestServer() {
        shutdown();
        try {
            runner.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        runner.clean();
    }

    public void refreshTestServer() {
        try {
            refreshIndexes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public PhotonResult getByID(String id) {
        try {
            final var response = client.get(fn -> fn
                    .index(PhotonIndex.NAME)
                    .id(id), OpenSearchResult.class);

            if (response.found()) {
                return response.source();
            }
        } catch (IOException e) {
            // ignore
        }

        return null;
    }
}
