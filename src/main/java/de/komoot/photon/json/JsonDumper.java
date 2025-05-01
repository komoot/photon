package de.komoot.photon.json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.komoot.photon.Importer;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Server;
import de.komoot.photon.nominatim.model.AddressType;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JsonDumper implements Importer {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(JsonDumper.class);

    private final String[] languages;

    private final JsonGenerator writer;
    private final GeoJsonWriter geojsonWriter = new GeoJsonWriter();

    public JsonDumper(String filename, String[] languages) throws IOException {
        this.languages = languages;

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

    public void writeHeader(Date importDate, Map<String, Map<String, String>> countryNames) throws IOException {
        writeStartDocument(NominatimDumpHeader.DOCUMENT_TYPE);

        writer.writeStartObject();
writer.writeStringField("version", NominatimDumpHeader.EXPECTED_VERSION);
        writer.writeStringField("generator", "photon");
        writer.writeStringField("database_version", Server.DATABASE_VERSION);
        writer.writeObjectField("data_timestamp", importDate);

        // country data
        writer.writeObjectFieldStart("country_info");
        for (var e : countryNames.entrySet()) {
            if (!e.getKey().isBlank()) {
                writer.writeObjectFieldStart(e.getKey());
                writer.writeObjectField("names", e.getValue());
                writer.writeEndObject();
            }
        }
        writer.writeEndObject();

        writer.writeEndObject();
        writeEndDocument();
    }

    public void writeNominatimDocument(PhotonDoc doc) throws IOException {
        writer.writeStartObject();
        writer.writeNumberField("place_id", doc.getPlaceId());
        writer.writeStringField("osm_type", doc.getOsmType());
        writer.writeNumberField("osm_id", doc.getOsmId());
        writer.writeStringField("tag_key", doc.getTagKey());
        writer.writeStringField("tag_value", doc.getTagValue());
        writer.writeNumberField("rank_address", doc.getRankAddress());
        writer.writeNumberField("importance", doc.getImportance());

        if (doc.getRankAddress() > 28) {
            writer.writeNumberField("parent_place_id", doc.getParentPlaceId());
        }

        // TODO: admin_level

        if (!doc.getName().isEmpty())
        writer.writeObjectField("name", doc.getName());

        if (doc.getHouseNumber() != null) {
            writer.writeStringField("housenumber", doc.getHouseNumber());
        }

        final Map<String, String> addressNames = new HashMap<>();
        for (var entry : doc.getAddressParts().keySet()) {
            if (entry != AddressType.COUNTRY) {
                doc.copyAddressName(addressNames, entry.getName(), entry, "name");

                for (String language : languages) {
                    doc.copyAddressName(
                            addressNames, entry.getName() + ":" + language,
                            entry, "name:" + language);
                }
            }
        }

        for (var entry : doc.getContextByLanguage(languages).entrySet()) {
            int i = 1;
            if ("default".equals(entry.getKey())) {
                for (var name : entry.getValue()) {
                    addressNames.put(String.format("context%d", i), name);
                    ++i;
                }
            } else {
                for (var name : entry.getValue()) {
                    addressNames.put(String.format("context%d:%s", i, entry.getKey()), name);
                    ++i;
                }
            }
        }

        if (!addressNames.isEmpty()) {
            writer.writeObjectField("address", addressNames);
        }

        if (!doc.getExtratags().isEmpty()) {
            writer.writeObjectField("extratags", doc.getExtratags());
        }

        if (doc.getPostcode() != null) {
            writer.writeStringField("postcode", doc.getPostcode());
        }

        writer.writeStringField("country_code", doc.getCountryCode().toLowerCase());

        writer.writeFieldName("centroid");
        writer.writeRawValue(geojsonWriter.write(doc.getCentroid()));

        final var bbox = doc.getBbox();
        if (bbox != null) {
            writer.writeArrayFieldStart("bbox");
            writer.writeNumber(bbox.getMinX());
            writer.writeNumber(bbox.getMaxY());
            writer.writeNumber(bbox.getMaxX());
            writer.writeNumber(bbox.getMinY());
            writer.writeEndArray();
        }
        writer.writeEndObject();
    }

    private void writeStartDocument(String type) throws IOException {
        writer.writeStartObject();
        writer.writeStringField("type", type);
        writer.writeFieldName("content");
    }

    private void writeEndDocument() throws IOException {
        writer.writeEndObject();
        writer.writeRaw('\n');
    }
}
