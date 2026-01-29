package de.komoot.photon;

import de.komoot.photon.config.PhotonDBConfig;
import de.komoot.photon.opensearch.OpenSearchResult;
import de.komoot.photon.opensearch.PhotonIndex;
import de.komoot.photon.searcher.PhotonResult;
import org.codelibs.opensearch.runner.OpenSearchRunner;
import org.opensearch.client.opensearch.core.search.Hit;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class TestServer {
    public static final String TEST_CLUSTER_NAME = "photon-test";

    private final OpenSearchRunner runner;
    private final Server testServer;

    public TestServer(String mainDirectory, String clusterName) throws IOException {
        runner = new OpenSearchRunner();
        runner.onBuild((number, settingsBuilder) -> {
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

        }).build(OpenSearchRunner.newConfigs()
                .basePath(mainDirectory)
                .clusterName(clusterName)
                .numOfNode(1)
                .baseHttpPort(9200));

        // wait for yellow status
        runner.ensureYellow();

        var config = new PhotonDBConfig(mainDirectory, clusterName, List.of("127.0.0.1:" + runner.node().settings().get("http.port")));

        testServer = new Server(config, true);
    }

    public void reloadDBProperties(DatabaseProperties dbProperties) throws IOException {
        testServer.recreateIndex(dbProperties);
        testServer.refreshIndexes();

    }

    public Importer createImporter(DatabaseProperties dbProperties) {
        return testServer.createImporter(dbProperties);
    }

    public Updater createUpdater(DatabaseProperties dbProperties) {
        return testServer.createUpdater(dbProperties);
    }

    public void stopTestServer() {
        testServer.shutdown();
        try {
            runner.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        runner.clean();
    }

    public Server getServer() {
        return testServer;
    }

    public String getHttpPort() {
        return runner.node().settings().get("http.port");
    }

    public void refreshTestServer() {
        try {
            testServer.refreshIndexes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public PhotonResult getByID(String id) {
        try {
            final var response = testServer.getClient().get(fn -> fn
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

    public List<PhotonResult> getAll() {
        try {
            final var response = testServer.getClient().search(s -> s.size(1000), OpenSearchResult.class);

            return response.hits().hits()
                    .stream().map(Hit::source)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            //ignore
        }

        return List.of();
    }
}
