package de.komoot.photon.config;

import com.beust.jcommander.Parameter;

import java.util.ArrayList;
import java.util.List;

public class ApiServerConfig {
    @Parameter(names = "-listen-port", description = "Port for the Photon server to listen to.")
    private int listenPort = 2322;

    @Parameter(names = "-listen-ip", description = "Address for the Photon server to listen to.")
    private String listenIp = "0.0.0.0";

    @Parameter(names = "-cors-any", description = "Enable cross-site resource sharing for any origin.")
    private boolean corsAnyOrigin = false;

    @Parameter(names = "-cors-origin", description = "Comma-separated list of origins for which to enable cross-site resource sharing.")
    private List<String> corsOrigin = new ArrayList<>();

    @Parameter(names = "-enable-update-api", description = "Enable the additional endpoint /nominatim-update, which allows to trigger updates from a nominatim database")
    private boolean enableUpdateApi = false;

    @Parameter(names = "-max-results", description = "The maximum possible 'limit' parameter for geocoding searches")
    private int maxResults = 50;

    @Parameter(names = "-max-reverse-results", description = "The maximum possible 'limit' parameter for reverse geocoding searches")
    private int maxReverseResults = 50;

    @Parameter(names = {"-metrics-enable"}, description = "Set to 'prometheus' to enable built-in Prometheus metrics endpoint")
    private String metrics = "";

    @Parameter(names = "-default-language", description = "Language to return results in when no explicit language is chosen by the user.")
    private String defaultLanguage = "default";

    @Parameter(names = "-synonym-file", description = "File with synonym and classification terms.")
    private String synonymFile = null;

    @Parameter(names = "-query-timeout", description = "Time after which to cancel queries to the ES database (in seconds).")
    private int queryTimeout = 7;

    public int getPort() {
        return this.listenPort;
    }

    public String getIp() {
        return this.listenIp;
    }

    public boolean isCorsAnyOrigin() {
        return this.corsAnyOrigin;
    }

    public String[] getCorsOrigin() {
        return this.corsOrigin.toArray(new String[0]);
    }

    public boolean isEnableUpdateApi() {
        return this.enableUpdateApi;
    }

    public int getMaxReverseResults() {
        return maxReverseResults;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public String getMetrics() {
        return metrics;
    }

    public String getDefaultLanguage() {
        return this.defaultLanguage;
    }

    public String getSynonymFile() {
        return this.synonymFile;
    }

    public int getQueryTimeout() {
        return this.queryTimeout;
    }
}
