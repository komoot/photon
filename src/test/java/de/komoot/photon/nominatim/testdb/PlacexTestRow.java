package de.komoot.photon.nominatim.testdb;

import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import de.komoot.photon.PhotonDoc;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

public class PlacexTestRow {
    private static long placeIdSequence = 10000;
    private final Long placeId;
    private Long parentPlaceId;
    private String osmType = "N";
    private Long osmId;
    private final String key;
    private final String value;
    private final Map<String, String> names = new HashMap<>();
    private final Map<String, String> address = new HashMap<>();
    private final Map<String, String> extraTags = new HashMap<>();
    private Integer rankAddress = 30;
    private Integer rankSearch = 30;
    private String centroid;
    private String geometry;
    private String postcode;
    private String countryCode = "us";
    private int admin_level = 15;
    private Double importance = null;

    public PlacexTestRow(String key, String value) {
        placeId = placeIdSequence++;
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
        if (map == null || map.isEmpty()) {
            return null;
        }

        return JsonMapper.shared().writeValueAsString(map);
    }

    public PlacexTestRow name(String name) {
        return name("name", name);
    }

    public PlacexTestRow name(String key, String name) {
        names.put(key, name);

        return this;
    }

    public PlacexTestRow extraTag(String key, String name) {
        extraTags.put(key, name);

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

    public PlacexTestRow geometry(String geometry) {
        this.geometry = geometry;
        return this;
    }

    public PlacexTestRow admin_level(int lvl) {
        this.admin_level = lvl;
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

    public PlacexTestRow ranks(int rank) {
        this.rankAddress = rank;
        this.rankSearch = rank;
        return this;
    }

    public PlacexTestRow parent(PlacexTestRow row) {
        this.parentPlaceId = row.getPlaceId();
        return this;
    }

    public PlacexTestRow postcode(String postcode) {
        this.postcode = postcode;
        return this;
    }

    public PlacexTestRow add(JdbcTemplate jdbc) {
        jdbc.update(
                "INSERT INTO placex (place_id, parent_place_id, osm_type, osm_id,"
                        + " class, type, rank_search, rank_address, admin_level,"
                        + " centroid, geometry, name, extratags, country_code,"
                        + " importance, address, postcode, indexed_status)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? FORMAT JSON, ? FORMAT JSON, ?, ?, ? FORMAT JSON, ?, 0)",
                placeId, parentPlaceId, osmType, osmId,
                key, value, rankSearch, rankAddress, admin_level,
                centroid, geometry, asJson(names), asJson(extraTags), countryCode,
                importance, asJson(address), postcode);
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
        assertThat(doc)
                .hasFieldOrPropertyWithValue("placeId", Long.toString(placeId))
                .hasFieldOrPropertyWithValue("osmType", osmType)
                .hasFieldOrPropertyWithValue("osmId", osmId)
                .hasFieldOrPropertyWithValue("tagKey", key)
                .hasFieldOrPropertyWithValue("tagValue", value)
                .hasFieldOrPropertyWithValue("centroid", new WKTReader().read(centroid))
                .hasFieldOrPropertyWithValue("postcode", postcode)
                .satisfies(
                        d -> {
                            if (countryCode == null) {
                                assertThat(d.getCountryCode()).isNull();
                            } else {
                                assertThat(countryCode.toUpperCase()).isEqualTo(d.getCountryCode());
                            }
                        });
    }

    public Long getPlaceId() {
        return this.placeId;
    }

    public String getPlaceString() {
        return Long.toString(this.placeId);
    }

    public Map<String, String> getNames() {
        return this.names;
    }

    public Integer getRankAddress() {
        return this.rankAddress;
    }
}
