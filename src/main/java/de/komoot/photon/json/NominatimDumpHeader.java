package de.komoot.photon.json;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.komoot.photon.UsageException;
import org.slf4j.Logger;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class NominatimDumpHeader {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(NominatimDumpHeader.class);

    public static final String DOCUMENT_TYPE = "NominatimDumpFile";
    public static final String EXPECTED_VERSION = "0.1.0";

    private String generator;
    private Date dataTimestamp;
    private Map<String, String> extraProperties = new HashMap<>();

    void setVersion(String version) {
        if (!EXPECTED_VERSION.equals(version)) {
            LOGGER.error("Dump file header has version '{}'. Expect version '{}'",
                         version, EXPECTED_VERSION);
            throw new UsageException("Invalid dump file.");
        }
    }

    void setGenerator(String generator) {
        this.generator = generator;
    }

    @JsonProperty("data_timestamp")
    void setDataTimestamp(Date timestamp) {
        dataTimestamp = timestamp;
    }

    public Date getDataTimestamp() {
        return dataTimestamp;
    }

    @JsonAnySetter
    void setExtraProperties(String key, String value) {
        extraProperties.put(key, value);
    }
}
