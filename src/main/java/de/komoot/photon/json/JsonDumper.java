package de.komoot.photon.json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.komoot.photon.*;
import de.komoot.photon.nominatim.model.AddressType;
import de.komoot.photon.nominatim.model.NameMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@NullMarked
public class JsonDumper implements Importer {
    private static final Logger LOGGER = LogManager.getLogger();

    private final DatabaseProperties dbProperties;

    private final JsonGenerator writer;
    private final GeoJsonWriter geojsonWriter = new GeoJsonWriter();

    public JsonDumper(String filename, DatabaseProperties dbProps) throws IOException {
        this.dbProperties = dbProps;

        final var mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        if ("-".equals(filename)) {
            writer = mapper.createGenerator(System.out);
        } else {
            writer = mapper.createGenerator(new File(filename), JsonEncoding.UTF8);
        }

        geojsonWriter.setEncodeCRS(false);
    }

    @Override
    public void add(Iterable<PhotonDoc> docs) {
        final var it = docs.iterator();

        if (!it.hasNext()) {
            return;
        }

        try {
            writeStartDocument(NominatimPlaceDocument.DOCUMENT_TYPE);

            // always write as an array to indicate that addresses are already expanded
            writer.writeStartArray();
            while (it.hasNext()) {
                writeNominatimDocument(it.next());
            }
            writer.writeEndArray();

            writeEndDocument();
        } catch (IOException e) {
            LOGGER.error("Could not write document {}", docs.iterator().next().getPlaceId(), e);
        }
    }

    @Override
    public void finish() {
        try {
            writer.close();
        } catch (IOException e) {
            LOGGER.warn("Error while closing output file",  e);
        }
    }

    public void writeHeader(Map<String, NameMap> countryNames) throws IOException {
        writeStartDocument(NominatimDumpHeader.DOCUMENT_TYPE);

        writer.writeStartObject();
        writer.writeStringField(DumpFields.HEADER_VERSION, NominatimDumpHeader.EXPECTED_VERSION);
        writer.writeStringField(DumpFields.HEADER_GENERATOR, "photon");
        writer.writeStringField(DumpFields.HEADER_DB_VERSION, Server.DATABASE_VERSION);
        writer.writeObjectField(DumpFields.HEADER_DB_TIME, dbProperties.getImportDate());
        writer.writeObjectField(DumpFields.HEADER_FEATURES, new NominatimDumpFileFeatures());
        writer.writeEndObject();
        writeEndDocument();

        // country data
        writeStartDocument(CountryInfo.DOCUMENT_TYPE);
        writer.writeStartArray();
        for (var e : countryNames.entrySet()) {
            if (!e.getKey().isBlank()) {
                writer.writeStartObject();
                writer.writeObjectField("country_code", e.getKey());
                writer.writeObjectFieldStart("name");
                for (var entry : e.getValue().entrySet()) {
                    writer.writeStringField(
                            convertNameKey(entry.getKey(), "name"),
                            entry.getValue());
                }
                writer.writeEndObject();
                writer.writeEndObject();
            }
        }
        writer.writeEndArray();
        writeEndDocument();

    }

    public void writeNominatimDocument(PhotonDoc doc) throws IOException {
        if (doc.getCentroid() == null) {
            return; // ignore documents without coordinates
        }

        writer.writeStartObject();
        if (doc.getPlaceId() != null) {
            writer.writeStringField(DumpFields.PLACE_ID, doc.getPlaceId());
        }
        writer.writeStringField(DumpFields.PLACE_OBJECT_TYPE, doc.getOsmType());
        writer.writeNumberField(DumpFields.PLACE_OBJECT_ID, doc.getOsmId());

        writer.writeStringField(DumpFields.PLACE_OSM_KEY, doc.getTagKey());
        writer.writeStringField(DumpFields.PLACE_OSM_VALUE, doc.getTagValue());
        writer.writeObjectField(DumpFields.PLACE_CATEGORIES,doc.getCategories());

        writer.writeNumberField(DumpFields.PLACE_RANK_ADDRESS, doc.getRankAddress());
        writer.writeNumberField(DumpFields.PLACE_IMPORTANCE, doc.getImportance());

        if (!doc.getName().isEmpty()) {
            writer.writeObjectFieldStart(DumpFields.PLACE_NAMES);
            for (var entry : doc.getName().entrySet()) {
                writer.writeStringField(
                        convertNameKey(entry.getKey(), "name"),
                        entry.getValue());
            }
            writer.writeEndObject();
        }

        if (doc.getHouseNumber() != null) {
            writer.writeStringField(DumpFields.PLACE_HOUSENUMBER, doc.getHouseNumber());
        }

        final Map<String, String> addressNames = new HashMap<>();
        for (var entry : doc.getAddressParts().entrySet()) {
            final var atype = entry.getKey();
            if (atype != AddressType.COUNTRY) {
                String baseKey;
                if (atype == AddressType.LOCALITY) {
                    baseKey = "neighbourhood";
                } else if (atype == AddressType.DISTRICT) {
                    baseKey = "suburb";
                } else {
                    baseKey = atype.getName();
                }

                for (var name : entry.getValue().entrySet()) {
                    addressNames.put(convertNameKey(name.getKey(), baseKey), name.getValue());
                }
            }
        }

        for (var entry : doc.getContext().entrySet()) {
            int i = 1;
            if ("default".equals(entry.getKey())) {
                for (var name : entry.getValue()) {
                    addressNames.put(String.format("other%d", i), name);
                    ++i;
                }
            } else {
                for (var name : entry.getValue()) {
                    addressNames.put(String.format("other%d:%s", i, entry.getKey()), name);
                    ++i;
                }
            }
        }

        if (!addressNames.isEmpty()) {
            writer.writeObjectField(DumpFields.PLACE_ADDRESS, addressNames);
        }

        dbProperties.configExtraTags().writeFilteredExtraTags(writer, DumpFields.PLACE_EXTRA_TAGS, doc.getExtratags());

        if (doc.getPostcode() != null) {
            writer.writeStringField(DumpFields.PLACE_POSTCODE, doc.getPostcode());
        }

        if (doc.getCountryCode() != null) {
            writer.writeStringField(DumpFields.PLACE_COUNTRY_CODE, doc.getCountryCode().toLowerCase());
        }

        final var coords = doc.getCentroid().getCoordinate();
        writer.writeArrayFieldStart(DumpFields.PLACE_CENTROID);
        writer.writeNumber(coords.x);
        writer.writeNumber(coords.y);
        writer.writeEndArray();

        final var bbox = doc.getBbox();
        if (bbox != null) {
            writer.writeArrayFieldStart(DumpFields.PLACE_BBOX);
            writer.writeNumber(bbox.getMinX());
            writer.writeNumber(bbox.getMaxY());
            writer.writeNumber(bbox.getMaxX());
            writer.writeNumber(bbox.getMinY());
            writer.writeEndArray();
        }

        final var geom = doc.getGeometry();
        if (geom != null) {
            writer.writeFieldName(DumpFields.PLACE_GEOMETRY);
            writer.writeRawValue(geojsonWriter.write(geom));
        }

        writer.writeEndObject();
    }

    private void writeStartDocument(String type) throws IOException {
        writer.writeStartObject();
        writer.writeStringField(DumpFields.DOCUMENT_TYPE, type);
        writer.writeFieldName(DumpFields.DOCUMENT_CONTENT);
    }

    private void writeEndDocument() throws IOException {
        writer.writeEndObject();
        writer.writeRaw('\n');
    }

    private String convertNameKey(String inKey, String base) {
        if ("default".equals(inKey)) {
            return base;
        }

        return String.format("%s:%s", base, inKey);
    }
}
