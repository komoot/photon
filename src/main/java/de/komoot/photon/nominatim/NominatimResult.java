package de.komoot.photon.nominatim;

import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import de.komoot.photon.PhotonDoc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Nominatim result consisting of the basic PhotonDoc for the object
 * and a map of attached house numbers together with their respective positions.
 */
class NominatimResult {
    private PhotonDoc doc;
    private Map<String, Point> housenumbers;

    public NominatimResult(PhotonDoc baseobj) {
        doc = baseobj;
        housenumbers = null;
    }

    PhotonDoc getBaseDoc() {
        return doc;
    }

    boolean isUsefulForIndex() {
        return (housenumbers != null && !housenumbers.isEmpty()) || doc.isUsefulForIndex();
    }

    List<PhotonDoc> getDocsWithHousenumber() {
        if (housenumbers == null || housenumbers.isEmpty()) {
            return ImmutableList.of(doc);
        }

        List<PhotonDoc> results = new ArrayList<>(housenumbers.size());
        for (Map.Entry<String, Point> e : housenumbers.entrySet()) {
            PhotonDoc copy = new PhotonDoc(doc);
            copy.houseNumber(e.getKey());
            copy.centroid(e.getValue());
            results.add(copy);
        }

        return results;
    }

    /**
     * Adds house numbers from a house number string.
     * <p>
     * This may either be a single house number or multiple
     * house numbers delimited by a semicolon. All locations
     * will be set to the centroid of the doc geometry.
     *
     * @param str House number string. May be null, in which case nothing is added.
     */
    public void addHousenumbersFromString(String str) {
        if (str == null || str.isEmpty())
            return;

        if (housenumbers == null)
            housenumbers = new HashMap<>();

        String[] parts = str.split(";");
        for (String part : parts) {
            String h = part.trim();
            if (!h.isEmpty())
                housenumbers.put(h, doc.getCentroid());
        }
    }

    public void addHousenumbersFromAddress(Map<String, String> address) {
        if (address == null) {
            return;
        }

        addHousenumbersFromString(address.get("housenumber"));
        addHousenumbersFromString(address.get("streetnumber"));
        addHousenumbersFromString(address.get("conscriptionnumber"));
    }

    public void addHouseNumbersFromInterpolation(long first, long last, String interpoltype, Geometry geom) {
        if (last <= first || (last - first) > 1000)
            return;

        if (housenumbers == null)
            housenumbers = new HashMap<>();

        LengthIndexedLine line = new LengthIndexedLine(geom);
        double si = line.getStartIndex();
        double ei = line.getEndIndex();
        double lstep = (ei - si) / (double) (last - first);

        // leave out first and last, they have a distinct OSM node that is already indexed
        long step = 2;
        long num = 1;
        if (interpoltype.equals("odd")) {
            if (first % 2 == 1)
                ++num;
        } else if (interpoltype.equals("even")) {
            if (first % 2 == 0)
                ++num;
        } else {
            step = 1;
        }

        GeometryFactory fac = geom.getFactory();
        for (; first + num < last; num += step) {
            housenumbers.put(String.valueOf(num + first), fac.createPoint(line.extractPoint(si + lstep * num)));
        }
    }
}
