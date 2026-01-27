package de.komoot.photon.config;

import com.beust.jcommander.Parameter;
import org.jspecify.annotations.NullMarked;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@NullMarked
public class PhotonDBConfig {
    public static final String GROUP = "Photon database options";

    public PhotonDBConfig(String dataDirectory, String cluster, List<String> transportAddresses) {
        this.dataDirectory = dataDirectory;
        this.cluster = cluster;
        this.transportAddresses = transportAddresses;
    }

    public PhotonDBConfig() {
    }

    @Parameter(names = "-data-dir", category = GROUP, placeholder = "DIR", description = """
            Data directory to use when using the internal server
            """)
    private String dataDirectory = new File(".").getAbsolutePath();

    @Parameter(names = "-transport-addresses", category = GROUP, placeholder = "ADDR,..", description = """
            Comma-separated list of addresses of external ElasticSearch nodes the client can connect to;
            when left empty, then an internal server is started and used
            """)
    private List<String> transportAddresses = new ArrayList<>();

    @Parameter(names = "-cluster", category = GROUP, placeholder = "NAME", description = """
            Name of the database cluster to use for the Photon database
            """)
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
