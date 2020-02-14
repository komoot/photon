package de.komoot.photon.elasticsearch;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import javax.annotation.Nonnull;

import org.json.JSONObject;

import de.komoot.photon.utils.DateFormatter;
import lombok.Getter;
import lombok.Setter;

/**
 * Container for the replication state
 * 
 * @author Simon
 *
 */
@Getter
@Setter
public class ReplicationState {

    private static final String TIMESTAMP_KEY       = "timestamp";
    private static final String FORMAT_KEY          = "format";
    private static final String SEQUENCE_NUMBER_KEY = "sequenceNumber";

    final long   sequenceNumber;
    final String format;
    final Date   timeStamp;

    final DateFormatter dateFormatter;

    /**
     * Construct a new instance
     * 
     * @param sequenceNumber the current sequence number
     * @param format the format of the replication file
     * @param timeStamp the current time
     */
    public ReplicationState(long sequenceNumber, @Nonnull String format, @Nonnull Date timeStamp) {
        this.sequenceNumber = sequenceNumber;
        this.format = format;
        this.timeStamp = timeStamp;
        dateFormatter = new DateFormatter();
    }

    /**
     * Read the replication state from a local file
     * 
     * @param stateFilePath the Path to the file
     * @return a ReplicationState instance
     * @throws IOException if something goes wrong
     */
    @Nonnull
    public static ReplicationState readState(@Nonnull Path stateFilePath) throws IOException {
        String content = new String(Files.readAllBytes(stateFilePath), Charset.forName("UTF-8"));
        JSONObject state = new JSONObject(content);

        return new ReplicationState(state.getLong(SEQUENCE_NUMBER_KEY), state.getString(FORMAT_KEY), null);
    }

    /**
     * Read the replication state from an url
     * 
     * @param urlString the url from the file
     * @return a ReplicationState instance
     * @throws IOException if something goes wrong
     */
    @Nonnull
    public static ReplicationState readState(@Nonnull String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("User-Agent", "Photon");
        urlConnection.setInstanceFollowRedirects(true);
        try {
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            JSONObject state = new JSONObject(result.toString("UTF-8"));
            return new ReplicationState(state.getLong(SEQUENCE_NUMBER_KEY), state.getString(FORMAT_KEY), null);
        } finally {
            urlConnection.disconnect();
        }
    }

    /**
     * Get a JSON representation of the replication state
     * 
     * @return a JSONObject containg the state
     */
    @Nonnull
    public JSONObject getJson() {
        final JSONObject state = new JSONObject();
        state.put(SEQUENCE_NUMBER_KEY, sequenceNumber);
        state.put(FORMAT_KEY, format);
        state.put(TIMESTAMP_KEY, dateFormatter.format(timeStamp));
        return state;
    }

    /**
     * Get a JSON representation of the replication state as a String
     * 
     * @return a String containing JSON 
     */
    @Nonnull
    public String toJsonString() {
        return getJson().toString(4);
    }
}
