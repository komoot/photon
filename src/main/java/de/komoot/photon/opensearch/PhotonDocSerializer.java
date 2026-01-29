package de.komoot.photon.opensearch;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import de.komoot.photon.DatabaseProperties;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.nominatim.model.AddressType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

@NullMarked
public class PhotonDocSerializer extends StdSerializer<PhotonDoc> {
    private final DatabaseProperties dbProperties;

    public PhotonDocSerializer(DatabaseProperties dbProperties) {
        super(PhotonDoc.class);
        this.dbProperties = dbProperties;
    }

    @Override
    public void serialize(PhotonDoc value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        final var atype = value.getAddressType();
        final var isNamed = !value.getName().isEmpty();
        final var isAddress = value.getHouseNumber() != null;
        final var termCollector = new NameCollector();
        final var nameCollector  = new NameCollector();

        gen.writeStartObject();
        gen.writeNumberField(DocFields.OSM_ID, value.getOsmId());
        gen.writeStringField(DocFields.OSM_TYPE, value.getOsmType());
        gen.writeStringField(DocFields.OSM_KEY, value.getTagKey());
        gen.writeStringField(DocFields.OSM_VALUE, value.getTagValue());
        gen.writeStringField(DocFields.OBJECT_TYPE, atype == null ? "locality" : atype.getName());
        gen.writeNumberField(DocFields.IMPORTANCE, value.getImportance());

        if (value.getCentroid() != null) {
            gen.writeObjectFieldStart(DocFields.COORDINATE);
            gen.writeNumberField("lat", value.getCentroid().getY());
            gen.writeNumberField("lon", value.getCentroid().getX());
            gen.writeEndObject();
        }

        if (value.getGeometry() != null && !value.getGeometry().getGeometryType().equals("Point")) {
            // Convert JTS Geometry to GeoJSON
            GeoJsonWriter geoJsonWriter = new GeoJsonWriter();
            String geoJson = geoJsonWriter.write(value.getGeometry());

            gen.writeFieldName(DocFields.GEOMETRY);
            gen.writeRawValue(geoJson);
        }

        if (isAddress) {
            gen.writeStringField(DocFields.HOUSENUMBER, value.getHouseNumber());
            termCollector.add(value.getHouseNumber(), isNamed ? 4 : 5);
        }

        if (value.getPostcode() != null) {
            gen.writeStringField(DocFields.POSTCODE, value.getPostcode());
            termCollector.add(value.getPostcode(), 2);
        }

        if (isNamed) {
            gen.writeObjectField(DocFields.NAME, value.getName());

            for (var entry : value.getName().entrySet()) {
                final var key = entry.getKey();
                final var name = entry.getValue();
                final boolean isPrimaryName = "default".equals(key)
                    || Arrays.asList(dbProperties.getLanguages()).contains(key);
                termCollector.add(name, isPrimaryName ? 5 : 2);
                nameCollector.add(name, isPrimaryName ? 2 : 1);
            }
        }


        for (var entry : value.getAddressParts().entrySet()) {
            final var type = entry.getKey();
            final var values = entry.getValue();
            final var prio = (value.getHouseNumber() != null && type == AddressType.STREET) ? 5 : type.getSearchPrio();

            gen.writeObjectField(type.getName(), values);
            termCollector.addAll(values.values(), prio);
        }

        String countryCode = value.getCountryCode();
        if (countryCode != null) {
            gen.writeStringField(DocFields.COUNTRYCODE, countryCode);
            termCollector.add(countryCode, 2);
        }

        dbProperties.configExtraTags().writeFilteredExtraTags(gen, DocFields.EXTRA, value.getExtratags());
        writeExtent(gen, value.getBbox());

        for (var contextVal : value.getContext().values()) {
            termCollector.addAll(contextVal, 1);
        }

        if (!value.getCategories().isEmpty()) {
            gen.writeObjectField(DocFields.CATEGORIES, value.getCategories());
            for (var cat : value.getCategories()) {
                termCollector.add("#" + cat, 1);
            }
        }

        gen.writeObjectFieldStart(DocFields.COLLECTOR);
        gen.writeStringField("all", termCollector.toCollectorString());
        if (isNamed) {
            gen.writeStringField("name", nameCollector.toCollectorString());
        }
        if (isAddress) {
            var parentNames = value.getAddressParts().get(AddressType.STREET);
            if (parentNames != null) {
                gen.writeStringField("parent", new NameCollector(parentNames.values()).toCollectorString());
            }
        }

        gen.writeObjectFieldStart("field");
        if (isNamed) {
            gen.writeObjectField(DocFields.NAME,
                    value.getName().values().stream().distinct().collect(Collectors.toList()));
        }
        for (var entry : value.getAddressParts().entrySet()) {
            gen.writeObjectField(
                    entry.getKey().getName(),
                    entry.getValue().values().stream().distinct().collect(Collectors.toList()));
        }
        gen.writeEndObject(); // collector.field

        gen.writeEndObject(); // collector

        gen.writeEndObject();
    }

    private static void writeExtent(JsonGenerator gen, @Nullable Envelope bbox) throws IOException {
        if (bbox == null || bbox.getArea() == 0.) return;

        //https://opensearch.org/docs/latest/field-types/supported-field-types/geo-shape/#envelope
        gen.writeObjectFieldStart(DocFields.EXTENT);
        gen.writeStringField("type", "envelope");

        gen.writeArrayFieldStart("coordinates");

        gen.writeStartArray();
        gen.writeNumber(bbox.getMinX());
        gen.writeNumber(bbox.getMaxY());
        gen.writeEndArray();
        gen.writeStartArray();
        gen.writeNumber(bbox.getMaxX());
        gen.writeNumber(bbox.getMinY());
        gen.writeEndArray();

        gen.writeEndArray();
        gen.writeEndObject();
    }

}
