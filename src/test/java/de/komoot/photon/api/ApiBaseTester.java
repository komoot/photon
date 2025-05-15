package de.komoot.photon.api;

import de.komoot.photon.App;
import de.komoot.photon.ESBaseTester;
import org.junit.jupiter.api.AfterEach;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static spark.Spark.*;

public class ApiBaseTester extends ESBaseTester {
    private static final int LISTEN_PORT = 30234;

    @AfterEach
    void shutdown() {
        stop();
        awaitStop();
    }

    protected void startAPI(String... extraParams) throws Exception {
        final String[] params = Stream.concat(
                Stream.of("-cluster", TEST_CLUSTER_NAME,
                        "-listen-port", Integer.toString(LISTEN_PORT),
                        "-transport-addresses", "127.0.0.1"),
                Arrays.stream(extraParams)).toArray(String[]::new);

        App.main(params);
        awaitInitialization();
    }

    protected HttpURLConnection connect(String url) throws IOException {
        return (HttpURLConnection) new URL("http://127.0.0.1:" + LISTEN_PORT + url).openConnection();
    }

    protected String readURL(String url) throws IOException {
        return new BufferedReader(new InputStreamReader(connect(url).getInputStream()))
                .lines().collect(Collectors.joining("\n"));
    }
}
