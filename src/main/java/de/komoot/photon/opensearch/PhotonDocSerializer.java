package de.komoot.photon.opensearch;

import de.komoot.photon.ConfigExtraTags;
import de.komoot.photon.DatabaseProperties;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.analyzer.config.AnalyzerConfig;
import de.komoot.photon.analyzer.variants.VariantProcessor;
import de.komoot.photon.nominatim.model.AddressType;
import de.komoot.photon.nominatim.model.PostcodeUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

@NullMarked
public class PhotonDocSerializer extends StdSerializer<PhotonDoc> {
    private Set<String> primaryLanguages = Set.of("default");
    private ConfigExtraTags configExtraTags = new ConfigExtraTags();
    @Nullable private VariantProcessor variantProcessor;

    public PhotonDocSerializer() {
        super(PhotonDoc.class);
    }

    public void configure(DatabaseProperties props, AnalyzerConfig analyzerConfig) {
        primaryLanguages = new HashSet<>(props.getLanguages());
        primaryLanguages.add("default");

        configExtraTags = props.configExtraTags();
        variantProcessor = analyzerConfig.createProcessor(
                props.getLanguages(), props.getInputNormalizerRules());
    }

    @Override
    public void serialize(PhotonDoc value, JsonGenerator gen, SerializationContext context) {
        assert variantProcessor != null;
        final var docProcessor = variantProcessor.getTransformForDoc(value);

        final var atype = value.getAddressType();
        final var isNamed = !value.getName().isEmpty();
        final var isAddress = value.getHouseNumber() != null;
        final var termCollector = new NameCollector();
        final var nameCollector = new NameCollector();
        final var fieldCollectors = new EnumMap<AddressType, Set<String>>(AddressType.class);

        gen.writeStartObject();
        gen.writeNumberProperty(DocFields.OSM_ID, value.getOsmId());
        gen.writeStringProperty(DocFields.OSM_TYPE, value.getOsmType());
        gen.writeStringProperty(DocFields.OSM_KEY, value.getTagKey());
        gen.writeStringProperty(DocFields.OSM_VALUE, value.getTagValue());
        gen.writeStringProperty(DocFields.OBJECT_TYPE, atype.getName());
        gen.writeNumberProperty(DocFields.IMPORTANCE, value.getImportance());

        if (value.getCentroid() != null) {
            gen.writeObjectPropertyStart(DocFields.COORDINATE);
            gen.writeNumberProperty("lat", value.getCentroid().getY());
            gen.writeNumberProperty("lon", value.getCentroid().getX());
            gen.writeEndObject();
        }

        if (value.getGeometry() != null && !value.getGeometry().getGeometryType().equals("Point")) {
            String geoJson = new GeoJsonWriter().write(value.getGeometry());

            gen.writeName(DocFields.GEOMETRY);
            gen.writeRawValue(geoJson);
        }

        if (isAddress) {
            var normHnr = docProcessor.normalize(value.getHouseNumber());
            gen.writeStringProperty(DocFields.HOUSENUMBER, normHnr);
            termCollector.add(normHnr, isNamed ? 4 : 5);
        }

        if (value.getPostcode() != null) {
            gen.writeStringProperty(DocFields.POSTCODE, value.getPostcode());
            termCollector.add(docProcessor.normalize(value.getPostcode()), 2);
            var altPostcode = PostcodeUtils.postcodeAltName(value.getPostcode());
            if (altPostcode != null) {
                termCollector.add(docProcessor.normalize(altPostcode), 2);
            }
        }

        if (isNamed) {
            gen.writeName(DocFields.NAME);
            context.writeValue(gen, value.getName());

            for (var entry : value.getName().entrySet()) {
                var key = entry.getKey();
                var names = docProcessor.transform(key, entry.getValue());
                final boolean isPrimaryName = primaryLanguages.contains(key);
                termCollector.addAll(names, isPrimaryName ? 5 : 2);
                nameCollector.addAll(names, isPrimaryName ? 2 : 1);
            }
        }


        for (var entry : value.getAddressParts().entrySet()) {
            final var type = entry.getKey();
            final var values = entry.getValue();
            final var prio = (value.getHouseNumber() != null && type == AddressType.STREET) ? 5 : type.getSearchPrio();

            gen.writeName(type.getName());
            context.writeValue(gen, values);
            var fieldCollector = new HashSet<String>();
            for (var val : values.entrySet()) {
                var names = docProcessor.transform(val.getKey(), val.getValue());
                termCollector.addAll(names, prio);
                fieldCollector.addAll(names);
            }
            fieldCollectors.put(type, fieldCollector);
        }

        String countryCode = value.getCountryCode();
        if (countryCode != null) {
            gen.writeStringProperty(DocFields.COUNTRYCODE, countryCode);
            termCollector.add(docProcessor.normalize(countryCode), 2);
        }

        configExtraTags.writeFilteredExtraTags(gen, DocFields.EXTRA, value.getExtratags());
        writeExtent(gen, value.getBbox());

        for (var contextVal : value.getContext().entrySet()) {
            termCollector.addAll(docProcessor.transform(contextVal.getKey(), contextVal.getValue()), 1);
        }

        if (!value.getCategories().isEmpty()) {
            gen.writeName(DocFields.CATEGORIES);
            context.writeValue(gen, value.getCategories());
            for (var cat : value.getCategories()) {
                termCollector.add("#" + cat, 1);
            }
        }

        gen.writeObjectPropertyStart(DocFields.COLLECTOR);
        gen.writeStringProperty("all", termCollector.toCollectorString());
        if (isNamed) {
            gen.writeStringProperty("name", nameCollector.toCollectorString());
        }
        if (isAddress) {
            var parentNames = fieldCollectors.get(AddressType.STREET);
            if (parentNames != null) {
                gen.writeStringProperty("parent", NameCollector.toCollectorString(parentNames));
            }
        }

        gen.writeObjectPropertyStart("field");
        if (isNamed) {
            nameCollector.writeNamesAsArrayProperty(gen, DocFields.NAME);
        }
        for (var entry : fieldCollectors.entrySet()) {
            gen.writeArrayPropertyStart(entry.getKey().getName());

            for (String name : entry.getValue()) {
                gen.writeString(name);
            }

            gen.writeEndArray();
        }
        gen.writeEndObject(); // collector.field

        gen.writeEndObject(); // collector

        gen.writeEndObject();
    }

    private void writeExtent(JsonGenerator gen, @Nullable Envelope bbox) {
        if (bbox == null || bbox.getArea() == 0.) return;

        //https://opensearch.org/docs/latest/field-types/supported-field-types/geo-shape/#envelope
        gen.writeObjectPropertyStart(DocFields.EXTENT);
        gen.writeStringProperty("type", "envelope");

        gen.writeArrayPropertyStart("coordinates");

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
