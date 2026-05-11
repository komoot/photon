package de.komoot.photon.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@NullMarked
@Parameters(parametersValidators = CorsMutuallyExclusiveValidator.class)
public class ApiServerConfig {
    public static final String GROUP = "API options";

    @Parameter(names = "-listen-port", category = GROUP, placeholder = "NUM", description = """
            Port for the Photon server to listen to
            """)
    private int listenPort = 2322;

    @Parameter(names = "-listen-ip", category = GROUP, placeholder = "IP", description = """
            Address for the Photon server to listen to
            """)
    private String listenIp = "127.0.0.1";

    @Parameter(names = "-cors-any", category = GROUP, description = """
            Enable cross-site resource sharing for any origin
            """)
    private boolean corsAnyOrigin = false;

    @Parameter(names = "-cors-origin", category = GROUP, placeholder = "ADDR,...", description = """
            Comma-separated list of origins for which to enable cross-site resource sharing
            """)
    private List<String> corsOrigin = new ArrayList<>();

    @Parameter(names = "-enable-update-api", category = GROUP, description = """
            Enable the additional endpoint /nominatim-update, which allows to trigger updates
            from a nominatim database; make sure to also set the PostgreSQL connection parameters
            """)
    private boolean enableUpdateApi = false;

    @Parameter(names = "-max-results", category = GROUP, placeholder = "NUM", description = """
            Maximum possible value for the 'limit' parameter for forward geocoding searches
            """)
    private int maxResults = 50;

    @Parameter(names = "-max-reverse-results", category = GROUP, placeholder = "NUM", description = """
            Maximum possible value for the 'limit' parameter for reverse geocoding searches
            """)
    private int maxReverseResults = 50;

    @Parameter(names = "-metrics-enable", category = GROUP, placeholder = "TYPE", description = """
            Enable /metrics endpoint of the given type; currently only supports 'prometheus'
            """)
    private String metrics = "";

    @Parameter(names = "-default-language", category = GROUP, placeholder = "LANG", description = """
            Language to return results in when no explicit language is chosen by the user;
            either one of the languages configured on import or 'default' to return the local
            language
            """)
    private String defaultLanguage = "default";

    @Parameter(names = "-synonym-file", category = GROUP, placeholder = "FILE", description = """
            File with synonym and classification terms
            """)
    @Nullable private String synonymFile = null;

    @Parameter(names = "-query-timeout", category = GROUP, placeholder = "SEC", description = """
            Time in seconds after which to cancel queries to the Photon database
            """)
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

    @Nullable public String getSynonymFile() {
        return this.synonymFile;
    }

    public int getQueryTimeout() {
        return this.queryTimeout;
    }
}
