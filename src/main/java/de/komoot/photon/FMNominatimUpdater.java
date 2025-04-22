package de.komoot.photon.nominatim;

import de.komoot.photon.PhotonDoc;
import de.komoot.photon.ManualPhotonDoc;
import de.komoot.photon.Updater;

import java.util.*;

import org.json.*;

public class FMNominatimUpdater extends NominatimUpdater {

    public FMNominatimUpdater(String host, int port, String database, String username, String password) {
        super(host, port, database, username, password);
    }

    public Boolean updateManualRecords(String prefix, JSONArray addresses, int startIndex, Boolean clean) {
        if (updateLock.tryLock()) {
            try {
                if (clean) {
                    System.out.println("Cleaning old " + prefix +  " records");
                    updater.cleanManualRecords(prefix);
                    updater.finish();
                    System.out.println("Cleaning finished");
                }

                System.out.println("Now importing " + addresses.length() + " addresses");
                for (int i = 0; i < addresses.length(); i++) {
                    if (i % 10000 == 0 && i > 0) {
                        System.out.println(i);
                        updater.finish();
                    }

                    JSONObject address = addresses.getJSONObject(i);

                    Map<String, String> name = new HashMap<>();
                    if (address.has("name")) {
                        name.put("name", address.getString("name"));
                        name.put("name:nl", address.getString("name"));
                    }

                    if (address.has("alt_name")) {
                        name.put("alt_name", address.getString("alt_name"));
                    }

                    PhotonDoc doc = new ManualPhotonDoc(
                        prefix,
                        i + startIndex,
                        address.getDouble("latitude"),
                        address.getDouble("longitude"),
                        address.optString("street"),
                        address.optString("housenumber"),
                        address.getString("location"),
                        address.optString("zipcode"),
                        address.getString("country_code"),
                        name
                    );

                    doc.setCountry(exporter.getCountryNames(doc.getCountryCode()));
                    updater.create(doc, 0);
                }
                updater.finish();
                System.out.println("Finished importing");
            } finally {
                updateLock.unlock();
            }
            return true;
        } else {
            return false;
        }
    }
}