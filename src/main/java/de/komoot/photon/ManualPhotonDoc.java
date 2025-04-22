package de.komoot.photon;
import de.komoot.photon.nominatim.model.AddressType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.Point;

import java.util.*;

public class ManualPhotonDoc extends PhotonDoc  {

    private final static GeometryFactory geometryFactory = new GeometryFactory();
    final private long index;
    final private String prefix;

    public ManualPhotonDoc(String prefix, long index, Double latitude, Double longitude, String street, String houseNumber, String city, String postcode, String countryCode, Map<String, String> name) {
        super(index, prefix, index, "place", "house_number");

        this.names(name);
        if (houseNumber != null) this.houseNumber(houseNumber);
        this.countryCode(countryCode);
        Point location = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        this.centroid(location);
        this.rankAddress(30);

        this.prefix = prefix;
        this.index = index;

        if (street != null) {
            this.setAddressPartIfNew(AddressType.STREET, new HashMap<String, String>() {
                { put("name", street); }
            });
        }

        this.setAddressPartIfNew(AddressType.CITY, new HashMap<String, String>() {
            { put("name", city); }
        });

        if (postcode != null) this.postcode(postcode);
    }

    @Override
    public String getUid(int objectId) {
        return this.prefix + ":" + String.valueOf(index);
    }
}