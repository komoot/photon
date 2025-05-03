package de.komoot.photon.json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.komoot.photon.ConfigExtraTags;
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
    private final ConfigExtraTags extraTags;

    private final JsonGenerator writer;
    private final GeoJsonWriter geojsonWriter = new GeoJsonWriter();

    public JsonDumper(String filename, String[] languages, ConfigExtraTags extraTags) throws IOException {
        this.languages = languages;
        this.extraTags = extraTags;

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
        writer.writeObjectField("features", new NominatimDumpFileFeatures());
        writer.writeEndObject();
        writeEndDocument();

        // country data
        writeStartDocument(CountryInfo.DOCUMENT_TYPE);
        writer.writeStartArray();
        for (var e : countryNames.entrySet()) {
            if (!e.getKey().isBlank()) {
                writer.writeStartObject();
                writer.writeObjectField("country_code", e.getKey());
                writer.writeObjectField("name", e.getValue());
                writer.writeEndObject();
            }
        }
        writer.writeEndArray();
        writeEndDocument();

    }

    public void writeNominatimDocument(PhotonDoc doc) throws IOException {
        writer.writeStartObject();
        writer.writeNumberField("place_id", doc.getPlaceId());
        writer.writeStringField("object_type", doc.getOsmType());
        writer.writeNumberField("object_id", doc.getOsmId());

        writer.writeArrayFieldStart("categories");
        writer.writeString(String.format("osm.%s.%s", doc.getTagKey(), doc.getTagValue()));
        writer.writeEndArray();

        writer.writeNumberField("rank_address", doc.getRankAddress());

        if (doc.getAdminLevel() != null) {
            writer.writeNumberField("admin_level", doc.getAdminLevel());
        }

        writer.writeNumberField("importance", doc.getImportance());

        if (doc.getRankAddress() > 28) {
            writer.writeNumberField("parent_place_id", doc.getParentPlaceId());
        }

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
            writer.writeObjectField("address", addressNames);
        }

        extraTags.writeFilteredExtraTags(writer, "extra", doc.getExtratags());

        if (doc.getPostcode() != null) {
            writer.writeStringField("postcode", doc.getPostcode());
        }

        if (doc.getCountryCode() != null) {
            writer.writeStringField("country_code", doc.getCountryCode().toLowerCase());
        }

        final var coords = doc.getCentroid().getCoordinate();
        writer.writeArrayFieldStart("centroid");
        writer.writeNumber(coords.x);
        writer.writeNumber(coords.y);
        writer.writeEndArray();

        final var bbox = doc.getBbox();
        if (bbox != null) {
            writer.writeArrayFieldStart("bbox");
            writer.writeNumber(bbox.getMinX());
            writer.writeNumber(bbox.getMaxY());
            writer.writeNumber(bbox.getMaxX());
            writer.writeNumber(bbox.getMinY());
            writer.writeEndArray();
        }

        final var geom = doc.getGeometry();
        if (geom != null) {
            writer.writeFieldName("geometry");
            writer.writeRawValue(geojsonWriter.write(geom));
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
