package de.komoot.photon.opensearch;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import de.komoot.photon.*;
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
        gen.writeNumberField(Constants.OSM_ID, value.getOsmId());
        gen.writeStringField(Constants.OSM_TYPE, value.getOsmType());
        gen.writeStringField(Constants.OSM_KEY, value.getTagKey());
        gen.writeStringField(Constants.OSM_VALUE, value.getTagValue());
        gen.writeStringField(Constants.OBJECT_TYPE, atype == null ? "locality" : atype.getName());
        gen.writeNumberField(Constants.IMPORTANCE, value.getImportance());

        if (value.getCentroid() != null) {
            gen.writeObjectFieldStart("coordinate");
            gen.writeNumberField("lat", value.getCentroid().getY());
            gen.writeNumberField("lon", value.getCentroid().getX());
            gen.writeEndObject();
        }

        if (value.getGeometry() != null && !value.getGeometry().getGeometryType().equals("Point")) {
            // Convert JTS Geometry to GeoJSON
            GeoJsonWriter geoJsonWriter = new GeoJsonWriter();
            String geoJson = geoJsonWriter.write(value.getGeometry());

            gen.writeFieldName("geometry");
            gen.writeRawValue(geoJson);
        }

        if (isAddress) {
            gen.writeStringField("housenumber", value.getHouseNumber());
            termCollector.add(value.getHouseNumber(), isNamed ? 4 : 5);
        }

        if (value.getPostcode() != null) {
            gen.writeStringField("postcode", value.getPostcode());
            termCollector.add(value.getPostcode(), 2);
        }

        if (isNamed) {
            gen.writeObjectField("name", value.getName());

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
            gen.writeStringField(Constants.COUNTRYCODE, countryCode);
            termCollector.add(countryCode, 2);
        }

        dbProperties.configExtraTags().writeFilteredExtraTags(gen, "extra", value.getExtratags());
        writeExtent(gen, value.getBbox());

        for (var contextVal : value.getContext().values()) {
            termCollector.addAll(contextVal, 1);
        }

        if (!value.getCategories().isEmpty()) {
            gen.writeObjectField("categories", value.getCategories());
            for (var cat : value.getCategories()) {
                termCollector.add("#" + cat, 1);
            }
        }

        gen.writeObjectFieldStart("collector");
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
            gen.writeObjectField("name",
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
        gen.writeObjectFieldStart("extent");
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
