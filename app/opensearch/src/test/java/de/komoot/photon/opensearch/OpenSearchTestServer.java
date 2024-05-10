package de.komoot.photon.opensearch;

import de.komoot.photon.Server;
import de.komoot.photon.searcher.PhotonResult;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.opensearch.common.settings.Settings;

import java.io.IOException;

public class OpenSearchTestServer extends Server {
    private OpenSearchRunner runner;
    private String instanceDir;

    public OpenSearchTestServer(String mainDirectory) {
        super(mainDirectory);

        instanceDir = mainDirectory;
    }

    public void startTestServer(String clusterName) {
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
            }).build(OpenSearchRunner.newConfigs().basePath(instanceDir).clusterName(clusterName).numOfNode(1).baseHttpPort(9200));

        // wait for yellow status
        runner.ensureYellow();

        String[] transportAddresses = {"127.0.0.1:" + runner.node().settings().get("http.port")};
        start(clusterName, transportAddresses);
    }

    public void stopTestServer() throws IOException {
        shutdown();
        runner.close();
        runner.clean();
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
