package de.komoot.photon.nominatim.testdb;

import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import de.komoot.photon.PhotonDoc;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

public class PlacexTestRow {
    private static final WKTReader wkt = new WKTReader();
    private static long place_id_sequence = 10000;
    private Long placeId;
    private Long parentPlaceId;
    private String osmType = "N";
    private Long osmId;
    private String key;
    private String value;
    private Map<String, String> names = new HashMap<>();
    private Map<String, String> address = new HashMap<>();
    private Integer rankAddress = 30;
    private Integer rankSearch = 30;
    private String centroid;
    private String geometry;
    private String countryCode = "us";
    private Double importance = null;

    public PlacexTestRow(String key, String value) {
        placeId = place_id_sequence++;
        this.key = key;
        this.value = value;
        osmId = placeId;
        centroid = "POINT (1.0 34.0)";
        geometry = "POLYGON ((6.4440619 52.1969454, 6.4441094 52.1969158, 6.4441408 52.1969347, 6.4441138 52.1969516, 6.4440933 52.1969643, 6.4440619 52.1969454))";
    }

    public static PlacexTestRow make_street(String name) {
        return new PlacexTestRow("highway", "residential").name(name).rankAddress(26).rankSearch(26);
    }

    private static String asJson(Map<String, String> map) {
        if (map == null) {
            return null;
        }

        JSONObject json = new JSONObject(map);

        return json.toString();
    }

    public PlacexTestRow id(long pid) {
        placeId = pid;
        return this;
    }

    public PlacexTestRow name(String name) {
        return name("name", name);
    }

    public PlacexTestRow name(String key, String name) {
        names.put(key, name);

        return this;
    }

    public PlacexTestRow osm(String type, long id) {
        osmType = type;
        osmId = id;
        return this;
    }

    public PlacexTestRow centroid(double x, double y) {
        centroid = "POINT(" + x + " " + y + ")";
        return this;
    }

    public PlacexTestRow housenumber(int value) {
        addr("housenumber", Integer.toString(value));
        return this;
    }

    public PlacexTestRow addr(String type, String value) {
        address.put(type, value);
        return this;
    }

    public PlacexTestRow country(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public PlacexTestRow importance(Double importance) {
        this.importance = importance;
        return this;
    }

    public PlacexTestRow rankSearch(int rank) {
        this.rankSearch = rank;
        return this;
    }

    public PlacexTestRow rankAddress(int rank) {
        this.rankAddress = rank;
        return this;
    }

    public PlacexTestRow parent(PlacexTestRow row) {
        this.parentPlaceId = row.getPlaceId();
        return this;
    }

    public PlacexTestRow add(JdbcTemplate jdbc) {
        jdbc.update("INSERT INTO placex (place_id, parent_place_id, osm_type, osm_id, class, type, rank_search, rank_address,"
                        + " centroid, geometry, name, country_code, importance, address, indexed_status)"
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? FORMAT JSON, ?, ?, ? FORMAT JSON, 0)",
                placeId, parentPlaceId, osmType, osmId, key, value, rankSearch, rankAddress, centroid, geometry,
                asJson(names), countryCode, importance, asJson(address));

        return this;
    }

    public void addAddresslines(JdbcTemplate jdbc, PlacexTestRow... rows) {
        for (PlacexTestRow row : rows) {
            jdbc.update("INSERT INTO place_addressline (place_id, address_place_id, cached_rank_address, isaddress)"
                            + "VALUES(?, ?, ?, true)",
                    placeId, row.getPlaceId(), row.getRankAddress());
        }
    }

    public void assertEquals(PhotonDoc doc) throws ParseException {
        Assertions.assertEquals(osmType, doc.getOsmType());
        Assertions.assertEquals(osmId, (Long) doc.getOsmId());
        Assertions.assertEquals(key, doc.getTagKey());
        Assertions.assertEquals(value, doc.getTagValue());
        Assertions.assertEquals(rankAddress, (Integer) doc.getRankAddress());
        Assertions.assertEquals(new WKTReader().read(centroid), doc.getCentroid());
        Assertions.assertEquals(names, doc.getName());
    }

    public Long getPlaceId() {
        return this.placeId;
    }

    public String getKey() {
        return this.key;
    }

    public String getValue() {
        return this.value;
    }

    public Map<String, String> getNames() {
        return this.names;
    }

    public Integer getRankAddress() {
        return this.rankAddress;
    }
}
