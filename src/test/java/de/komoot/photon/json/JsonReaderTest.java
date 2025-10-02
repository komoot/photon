package de.komoot.photon.json;

import de.komoot.photon.ConfigExtraTags;
import de.komoot.photon.UsageException;
import de.komoot.photon.nominatim.ImportThread;
import de.komoot.photon.nominatim.model.AddressType;
import de.komoot.photon.nominatim.testdb.CollectingImporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class JsonReaderTest {
    private final GeometryFactory geomFactory = new GeometryFactory();
    private final StringWriter inBuffer = new StringWriter();
    private final PrintWriter input = new PrintWriter(inBuffer);

    private ConfigExtraTags configExtraTags = new ConfigExtraTags();
    private String[] configCountries = null;
    private boolean configGeometryColumn = false;

    private static final String TEST_SIMPLE_CONTENT =
            "{\"place_id\":100818,\"object_type\":\"W\",\"object_id\":223306798,\"categories\" : [\"osm.waterway.stream\", \"natural.water.flowing.23-5\", \"bad.täg\"], \"rank_address\" : 0, \"rank_search\" : 22, \"importance\" : 0.10667666666666664,\"name\":{\"name\": \"Spiersbach\", \"name:de\": \"Spiersbach\", \"alt_name\": \"Spirsbach\"},\"extra\":{\"boat\": \"no\"},\"country_code\":\"at\",\"centroid\":[9.53713454,47.27052526],\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[9.5461636,47.2415541],[9.5558108,47.2955234],[9.556083,47.2962812],[9.554958,47.2966235]]}}";
    private static final String TEST_SIMPLE_STREAM =
            "{\"type\":\"Place\",\"content\":" + TEST_SIMPLE_CONTENT + "}";

    private InputStream inBufferAsStream() {
        return new ByteArrayInputStream(inBuffer.getBuffer().toString().getBytes(StandardCharsets.UTF_8));
    }

    private CollectingImporter readJson() throws IOException {
        var importer = new CollectingImporter();
        var importThread = new ImportThread(importer);

        try {
            var reader = new JsonReader(inBufferAsStream());
            if (configCountries != null) {
                reader.setCountryFilter(configCountries);
            }
            reader.setExtraTags(configExtraTags);
            reader.setUseFullGeometries(configGeometryColumn);
            reader.setCountryFilter(configCountries);
            reader.setLanguages(new String[]{"en", "de"});

            reader.readFile(importThread);
        } finally {
            importThread.finish();
        }

        assertThat(importer.getFinishCalled()).isEqualTo(1);

        return importer;
    }

    @Test
    void testSimpleImportDefault() throws IOException {
        input.println(TEST_SIMPLE_STREAM);
        var importer = readJson();

        assertThat(importer).singleElement()
                .hasFieldOrPropertyWithValue("placeId", 100818L)
                .hasFieldOrPropertyWithValue("osmType", "W")
                .hasFieldOrPropertyWithValue("osmId", 223306798L)
                .hasFieldOrPropertyWithValue("tagKey", "waterway")
                .hasFieldOrPropertyWithValue("tagValue", "stream")
                .hasFieldOrPropertyWithValue("name", Map.of("default", "Spiersbach", "de", "Spiersbach", "alt", "Spirsbach"))
                .hasFieldOrPropertyWithValue("postcode", null)
                .hasFieldOrPropertyWithValue("extratags", Map.of())
                .hasFieldOrPropertyWithValue("bbox", new Envelope(9.5461636, 9.556083, 47.2415541, 47.2966235))
                .hasFieldOrPropertyWithValue("parentPlaceId", 0L)
                .hasFieldOrPropertyWithValue("importance", 0.10667666666666664)
                .hasFieldOrPropertyWithValue("countryCode", "AT")
                .hasFieldOrPropertyWithValue("rankAddress", 0)
                .hasFieldOrPropertyWithValue("adminLevel", null)
                .hasFieldOrPropertyWithValue("houseNumber", null)
                .hasFieldOrPropertyWithValue("centroid", geomFactory.createPoint(new Coordinate(9.53713454, 47.27052526)))
                .hasFieldOrPropertyWithValue("geometry", null)
                .hasFieldOrPropertyWithValue("categories", Set.of("osm.waterway.stream", "natural.water.flowing.23-5"));
    }

    @Test
    void testImportDocumentAsArray() throws IOException {
        input.write("{\"type\":\"Place\",\"content\": [");
        input.write(TEST_SIMPLE_CONTENT);
        input.write(",");
        input.write(TEST_SIMPLE_CONTENT.replaceFirst("223306798", "223306799"));
        input.write("]}");

        var importer = readJson();

        assertThat(importer)
                .hasSize(2)
                .allSatisfy(d -> assertThat(d)
                        .hasFieldOrPropertyWithValue("placeId", 100818L)
                        .hasFieldOrPropertyWithValue("osmType", "W")
                        .hasFieldOrPropertyWithValue("tagKey", "waterway")
                        .hasFieldOrPropertyWithValue("tagValue", "stream")
                        .hasFieldOrPropertyWithValue("name", Map.of("default", "Spiersbach", "de", "Spiersbach", "alt", "Spirsbach"))
                        .hasFieldOrPropertyWithValue("postcode", null)
                        .hasFieldOrPropertyWithValue("extratags", Map.of())
                        .hasFieldOrPropertyWithValue("bbox", new Envelope(9.5461636, 9.556083, 47.2415541, 47.2966235))
                        .hasFieldOrPropertyWithValue("parentPlaceId", 0L)
                        .hasFieldOrPropertyWithValue("importance", 0.10667666666666664)
                        .hasFieldOrPropertyWithValue("countryCode", "AT")
                        .hasFieldOrPropertyWithValue("rankAddress", 0)
                        .hasFieldOrPropertyWithValue("adminLevel", null)
                        .hasFieldOrPropertyWithValue("houseNumber", null)
                        .hasFieldOrPropertyWithValue("addressParts", Map.of())
                        .hasFieldOrPropertyWithValue("centroid", geomFactory.createPoint(new Coordinate(9.53713454, 47.27052526)))
                        .hasFieldOrPropertyWithValue("geometry", null))
                .extracting("osmId")
                .containsExactly(223306798L, 223306799L);
    }

    @Test
    void testSimpleImportWithGeometry() throws IOException {
        configGeometryColumn = true;
        input.println(TEST_SIMPLE_STREAM);
        var importer = readJson();

        assertThat(importer).singleElement()
                .hasFieldOrPropertyWithValue("placeId", 100818L)
                .hasFieldOrPropertyWithValue("bbox", new Envelope(9.5461636, 9.556083, 47.2415541, 47.2966235))
                .hasFieldOrPropertyWithValue("centroid", geomFactory.createPoint(new Coordinate(9.53713454, 47.27052526)))
                .hasFieldOrPropertyWithValue("geometry", geomFactory.createLineString(new Coordinate[]{
                        new Coordinate(9.5461636, 47.2415541),
                        new Coordinate(9.5558108, 47.2955234),
                        new Coordinate(9.556083, 47.2962812),
                        new Coordinate(9.554958, 47.2966235)
                }));

    }

    @Test
    void testImportWithCountryInfo() throws IOException {
        input.println("{\"type\":\"CountryInfo\",\"content\":[{\"country_code\":\"at\",\"name\":{\"name\": \"Österreich\", \"name:ab\": \"Австриа\", \"name:de\": \"Österreich\", \"name:dv\": \"އޮސްޓްރިއާ\", \"name:dz\": \"ཨས་ཊི་ཡ\", \"name:ee\": \"Austria\", \"name:el\": \"Αυστρία\", \"name:en\": \"Austria\"}}]}");
        input.println(TEST_SIMPLE_STREAM);
        var importer = readJson();

        assertThat(importer).singleElement()
                .hasFieldOrPropertyWithValue("addressParts", Map.of(
                        AddressType.COUNTRY, Map.of("de", "Österreich", "default", "Österreich", "en", "Austria")
                ));
    }

    @Test
    void testSimpleImportAllExtraTags() throws IOException {
        configExtraTags = new ConfigExtraTags(List.of("ALL"));
        input.println(TEST_SIMPLE_STREAM);
        var importer = readJson();

        assertThat(importer).singleElement()
                .hasFieldOrPropertyWithValue("placeId", 100818L)
                .hasFieldOrPropertyWithValue("extratags", Map.of("boat", "no"));
    }

    @Test
    void testSimpleImportSomeExtraTagsPositive() throws IOException {
        configExtraTags = new ConfigExtraTags(List.of("maxspeed", "boat"));
        input.println(TEST_SIMPLE_STREAM);
        var importer = readJson();

        assertThat(importer).singleElement()
                .hasFieldOrPropertyWithValue("placeId", 100818L)
                .hasFieldOrPropertyWithValue("extratags", Map.of("boat", "no"));
    }

    @Test
    void testSimpleImportSomeExtraTagsNegative() throws IOException {
        configExtraTags = new ConfigExtraTags(List.of("maxspeed", "surface"));
        input.println(TEST_SIMPLE_STREAM);
        var importer = readJson();

        assertThat(importer).singleElement()
                .hasFieldOrPropertyWithValue("placeId", 100818L)
                .hasFieldOrPropertyWithValue("extratags", Map.of());
    }

    @Test
    void testSimpleImportCountryFilterPositive() throws IOException {
        configCountries = new String[]{"de", "li"};
        input.println(TEST_SIMPLE_STREAM);
        var importer = readJson();

        assertThat(importer.size()).isEqualTo(0);
    }

    @Test
    void testSimpleImportCountryFilterNegative() throws IOException {
        configCountries = new String[]{"at", "li"};
        input.println(TEST_SIMPLE_STREAM);
        var importer = readJson();

        assertThat(importer.size()).isEqualTo(1);
    }

    @Test
    void testAddressFromAddressLines() throws IOException {
        input.println("{\"type\":\"Place\",\"content\":{\"place_id\" : 105903, \"object_type\" : \"R\", \"object_id\" : 1155956, \"categories\" : [\"osm.place.city\"], \"rank_address\" : 16, \"rank_search\" : 16, \"importance\" : 0.6014034960585685,\"admin_level\":8,\"name\":{\"name\": \"Vaduz\", \"name:de\": \"VaduzD\", \"name:it\": \"VaduzI\"},\"centroid\":[9.52279620,47.13928620],\"geometry\":{\"type\":\"Point\",\"coordinates\":[9.4950763,47.1569405]}}}");
        input.println("{\"type\":\"Place\",\"content\":{\"place_id\" : 105764, \"object_type\" : \"N\", \"object_id\" : 4637485890, \"categories\" : [\"osm.amenity.theatre\"], \"rank_address\" : 30, \"rank_search\" : 30, \"importance\" : 9.99999999995449e-06,\"parent_place_id\":106180,\"name\":{\"name\": \"Kleintheater Schlösslekeller\"},\"address\":{\"country\": \"LI\", \"postcode\": \"9490\"},\"postcode\":\"9490\",\"country_code\":\"li\",\"addresslines\":[{\"place_id\":106180,\"rank_address\":26,\"fromarea\":false,\"isaddress\":true},{\"place_id\":105903,\"rank_address\":16,\"isaddress\":true,\"fromarea\":true}, {\"place_id\":106289,\"rank_address\":12,\"isaddress\":true,\"fromarea\":true}],\"centroid\":[9.52394930,47.12904370],\"geometry\":{\"type\":\"Point\",\"coordinates\":[9.5239493,47.1290437]}}}");

        var importer = readJson();

        assertThat(importer)
                .hasSize(2)
                .last()
                .hasFieldOrPropertyWithValue("placeId", 105764L)
                .hasFieldOrPropertyWithValue("postcode", "9490")
                .hasFieldOrPropertyWithValue("addressParts",
                        Map.of(AddressType.CITY,
                                Map.of("default", "Vaduz", "de", "VaduzD")));
    }

    @Test
    void testAddressFromAddressField() throws IOException {
        input.println("{\"type\":\"Place\",\"content\":{\"place_id\" : 105764, \"object_type\" : \"N\", \"object_id\" : 4637485890, \"categories\" : [\"osm.amenity.theatre\"], \"rank_address\" : 30, \"rank_search\" : 30, \"importance\" : 9.99999999995449e-06,\"parent_place_id\":106180,\"name\":{\"name\": \"Kleintheater Schlösslekeller\"},\"address\":{\"city\": \"Vaduz\", \"city:de\": \"VaduzD\", \"city:hu\": \"VaduzHU\", \"other1\": \"This\", \"other2\": \"That\", \"other1:de\": \"Dies\", \"other2:de\": \"Das\"},\"postcode\":\"9490\",\"country_code\":\"li\",\"addresslines\":[{\"place_id\":106180,\"rank_address\":26,\"fromarea\":false,\"isaddress\":true},{\"place_id\":105903,\"rank_address\":16,\"isaddress\":true,\"fromarea\":true}, {\"place_id\":106289,\"rank_address\":12,\"isaddress\":true,\"fromarea\":true}],\"centroid\":[9.52394930,47.12904370]}}");

        var importer = readJson();

        assertThat(importer).singleElement()
                .hasFieldOrPropertyWithValue("addressParts", Map.of(
                        AddressType.CITY, Map.of("default", "Vaduz", "de", "VaduzD")))
                .hasFieldOrPropertyWithValue("context", Map.of(
                        "default", Set.of("This", "That"),
                        "de", Set.of("Dies", "Das")));

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "{}", "[]", "\"A String\"", "34",
            "{\"content\": {}}",
            "{\"type\": \"NominatimDumpFile\"}",
            "{\"type\":\"CountryInfo\", \"content\":{}}"})
    void testInvalidHeader(String content) throws IOException {
        inBuffer.append(content);
        var reader = new JsonReader(inBufferAsStream());

        assertThatExceptionOfType(UsageException.class)
                .isThrownBy(reader::readHeader)
                .withMessageContaining("Invalid dump file");
    }

    @Test
    void testGoodHeader() throws IOException {
        inBuffer.append("{\"type\":\"NominatimDumpFile\",\"content\":{\"version\":\"0.1.0\",\"generator\":\"nominatim-5.1.0\",\"database_version\":\"5.1.0-0\",\"features\":{\"sorted_by_country\":true,\"has_addresslines\":true},\"data_timestamp\":\"2021-01-06T15:53:42+00:00\"}}");

        var reader = new JsonReader(inBufferAsStream());

        reader.readHeader();

        assertThat(reader.getImportDate()).hasSameTimeAs("2021-01-06T15:53:42+00:00");

    }
}