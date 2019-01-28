package de.komoot.photon.elasticsearch;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPInputStream;

import javax.annotation.Nonnull;

import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import de.komoot.photon.Updater;
import de.komoot.photon.utils.SequenceFormatter;
import lombok.extern.slf4j.Slf4j;

/**
 * Update Photon from a remote repository of serialised PhotonDocs
 * 
 * @author Simon Poole
 *
 */
@Slf4j
public class Replicatior {

    final String            replicationUrlString;
    final File              baseDirectory;
    final SequenceFormatter sequenceFormatter;
    final Updater           updater;
    final int               interval;
    final GeometryFactory   factory;

    /**
     * Create a new instance of Replicator
     * 
     * @param replicationUrl the base URL of where the replication files are stored
     * @param interval how often should we query the server in minutes
     */
    public Replicatior(@Nonnull final File baseDirectory, @Nonnull final String replicationUrlString, int interval, @Nonnull final Updater updater) {
        this.replicationUrlString = replicationUrlString;
        this.baseDirectory = baseDirectory;
        this.updater = updater;
        this.interval = interval;
        sequenceFormatter = new SequenceFormatter(9, 3);
        //
        factory = new GeometryFactory();
    }

    /**
     * Start the regular updates
     */
    public void start() {
        log.info(String.format("Starting replication from %s every %d minutes", replicationUrlString, interval));
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new ReplicationTask(), 60 * 1000L, interval * 60 * 1000L);
    }

    /**
     * Read the local and remote state file and then download any missing replication files
     * 
     * FIXME: should likely throw and exception if the difference between sequence numbers is very large
     *
     */
    class ReplicationTask extends TimerTask {

        @Override
        public void run() {

            try {
                // get our local state file
                ReplicationState lastState = ReplicationState.readState(Paths.get(baseDirectory + "/laststate.json"));
                long lastSequenceNumber = lastState.getSequenceNumber();
                // get the remote state file
                ReplicationState currentState = ReplicationState.readState(replicationUrlString + "/state.json");
                long currentSequenceNumber = currentState.getSequenceNumber();
                // get all replication files up to the current one and update the database
                for (long i = lastSequenceNumber + 1; i <= currentSequenceNumber; i++) {
                    log.info(String.format("Runing update for sequence %d", i));
                    URL url = new URL(replicationUrlString + "/" + sequenceFormatter.getFormattedName(i, ".json.gz"));
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestProperty("User-Agent", "Photon");
                    urlConnection.setInstanceFollowRedirects(true);
                    InputStream in = new BufferedInputStream(new GZIPInputStream(urlConnection.getInputStream()));
                    JsonReader reader = new JsonReader(in);
                    reader.addReader(com.vividsolutions.jts.geom.Point.class, new PointReader());
                    List<PhotonAction> actions = (List<PhotonAction>) reader.readObject();
                    int deletions = 0;
                    int updates = 0;
                    for (PhotonAction action : actions) {
                        switch (action.action) {
                        case DELETE:
                            updater.delete(action.id);
                            deletions++;
                            break;
                        case CREATE:
                            updater.create(action.doc);
                            updates++;
                            break;
                        case UPDATE:
                            updater.update(action.doc);
                            updates++;
                            break;
                        case UPDATE_OR_CREATE:
                            updater.updateOrCreate(action.doc);
                            updates++;
                            break;
                        }
                    }
                    updater.finish();
                    lastState = new ReplicationState(i, ReplicationUpdater.REPLICATION_FORMAT, new Date());
                    PrintStream outputStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(baseDirectory + "/laststate.json")));
                    outputStream.print(lastState.toJsonString());
                    outputStream.close();
                    reader.close();
                    log.info(String.format("Update done. %d deletions %d updates or additions.", deletions, updates));
                }
            } catch (IOException e) {
                log.error("Replicator failing", e);
                cancel();
            }
        }
    }

    /**
     * json-io needs some help de-serialising the Point class
     *
     */
    class PointReader implements JsonReader.JsonClassReaderEx {

        @SuppressWarnings("rawtypes")
        @Override
        public Point read(Object jOb, Deque<JsonObject<String, Object>> stack, Map<String, Object> args) {
            JsonObject coordinates = (JsonObject) ((JsonObject) jOb).get("coordinates");
            Object c = coordinates.get("coords");
            Double[] coords = null;
            if (c instanceof Double[]) {
                coords = (Double[]) c;
            } else if (c instanceof JsonObject && ((JsonObject) c).isArray()) {
                Object[] temp = ((JsonObject) c).getArray();
                coords = new Double[temp.length];
                for (int i = 0; i < temp.length; i++) {
                    coords[i] = (Double) temp[i];
                }
            } else if (c instanceof Object[]) {
                Object[] temp = (Object[]) c;
                coords = new Double[temp.length];
                for (int i = 0; i < temp.length; i++) {
                    coords[i] = (Double) temp[i];
                }
            } else { // crash and burn
                throw new IllegalArgumentException("PointReader unknown serialisation " + c.getClass().getCanonicalName() + " " + c.toString());
            }
            return factory.createPoint(new Coordinate(coords[0], coords[1]));
        }
    }
}
