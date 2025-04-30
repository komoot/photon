package de.komoot.photon;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.linearref.LengthIndexedLine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PhotonDocInterpolationSet implements Iterable<PhotonDoc> {

    private final List<PhotonDoc> docs = new ArrayList<>();

    public PhotonDocInterpolationSet(PhotonDoc base, long first, long last, long step, Geometry geom) {
        base.bbox(geom.getEnvelope());
        if (last == first) {
            docs.add(base.houseNumber(String.valueOf(first)).centroid(geom.getCentroid()));
        } else if (first < last && (last - first) < 600 && step < 10) {
            LengthIndexedLine line = new LengthIndexedLine(geom);
                double si = line.getStartIndex();
                double ei = line.getEndIndex();
                double lstep = (ei - si) / (last - first);

                var fac = geom.getFactory();
                for (long num = 0; first + num <= last; num += step) {
                    docs.add(new PhotonDoc(base)
                            .houseNumber(String.valueOf(num + first))
                            .centroid(fac.createPoint(line.extractPoint(si + lstep * num)))
                    );
                }
        }
    }

    @Override
    public Iterator<PhotonDoc> iterator() {
        return docs.iterator();
    }
}
