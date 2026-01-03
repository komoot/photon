package de.komoot.photon.config;

import com.beust.jcommander.Parameter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PhotonDBConfig {

    public PhotonDBConfig(String dataDirectory, String cluster, List<String> transportAddresses) {
        this.dataDirectory = dataDirectory;
        this.cluster = cluster;
        this.transportAddresses = transportAddresses;
    }

    public PhotonDBConfig() {
    }

    @Parameter(names = "-data-dir", description = "Photon data directory.")
    private String dataDirectory = new File(".").getAbsolutePath();

    @Parameter(names = "-transport-addresses", description = "Comma-separated list of addresses of external ElasticSearch nodes the client can connect to. An empty list (the default) forces an internal node to start.")
    private List<String> transportAddresses = new ArrayList<>();

    @Parameter(names = "-cluster", description = "Name of ElasticSearch cluster to put the server into.")
    private String cluster = "photon";

    public String getDataDirectory() {
        return this.dataDirectory;
    }

    public String getCluster() {
        return this.cluster;
    }

    public List<String> getTransportAddresses() {
        return this.transportAddresses;
    }
}
