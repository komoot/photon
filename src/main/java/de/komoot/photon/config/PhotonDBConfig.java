package de.komoot.photon.config;

import com.beust.jcommander.Parameter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
            Comma-separated list of addresses of external OpenSearch nodes the client can connect to;
            when left empty, then an internal server is started and used
            """)
    private List<String> transportAddresses = new ArrayList<>();

    @Parameter(names = "-cluster", category = GROUP, placeholder = "NAME", description = """
            Name of the database cluster to use for the Photon database
            """)
    private String cluster = "photon";

    @Nullable
    @Parameter(names = "-opensearch-user", category = GROUP, description = """
            Username for basic authentication with an external OpenSearch cluster
            """)
    private String opensearchUser;

    @Nullable
    @Parameter(names = "-opensearch-password", category = GROUP, password = true, description = """
            Password for basic authentication with an external OpenSearch cluster
            """)
    private String opensearchPassword;

    @Parameter(names = "-opensearch-ssl", category = GROUP, description = """
            Enable SSL/TLS for the connection to an external OpenSearch cluster
            """)
    private boolean opensearchSsl = false;

    @Nullable
    @Parameter(names = "-opensearch-truststore", category = GROUP, description = """
            Path to a PKCS12 truststore for server certificate validation
            """)
    private String opensearchTruststore;

    @Nullable
    @Parameter(names = "-opensearch-truststore-password", category = GROUP, password = true, description = """
            Password for the truststore
            """)
    private String opensearchTruststorePassword;

    @Nullable
    @Parameter(names = "-opensearch-keystore", category = GROUP, description = """
            Path to a PKCS12 keystore for client certificate authentication (mTLS)
            """)
    private String opensearchKeystore;

    @Nullable
    @Parameter(names = "-opensearch-keystore-password", category = GROUP, password = true, description = """
            Password for the keystore
            """)
    private String opensearchKeystorePassword;

    public String getDataDirectory() {
        return this.dataDirectory;
    }

    public String getCluster() {
        return this.cluster;
    }

    public List<String> getTransportAddresses() {
        return this.transportAddresses;
    }

    public @Nullable String getOpensearchUser() {
        return opensearchUser;
    }

    public @Nullable String getOpensearchPassword() {
        return opensearchPassword;
    }

    public boolean isOpensearchSsl() {
        return opensearchSsl;
    }

    public @Nullable String getOpensearchTruststore() {
        return opensearchTruststore;
    }

    public @Nullable String getOpensearchTruststorePassword() {
        return opensearchTruststorePassword;
    }

    public @Nullable String getOpensearchKeystore() {
        return opensearchKeystore;
    }

    public @Nullable String getOpensearchKeystorePassword() {
        return opensearchKeystorePassword;
    }

    public boolean hasAuthentication() {
        return opensearchUser != null && opensearchPassword != null;
    }
}
