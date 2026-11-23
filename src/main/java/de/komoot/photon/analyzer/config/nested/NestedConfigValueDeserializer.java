package de.komoot.photon.analyzer.config.nested;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.io.FileNotFoundException;

class NestedConfigValueDeserializer<T> extends StdDeserializer<NestableValue<T>> {
    private final NestedConfigResolver resolver;
    private final Class<T> classType;

    public NestedConfigValueDeserializer(NestedConfigResolver resolver, Class<T> type) {
        super(NestableValue.class);
        this.resolver = resolver;
        this.classType = type;
    }

    @Override
    public NestableValue<T> deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            try {
                var stream = resolver.getStreamForFile(p.getValueAsString());
                if (stream != null) {
                    var parser = ctxt.createParser(stream);
                    parser.nextToken();
                    return new NestableValue<>(parser.readValueAs(classType));
                }
            } catch (FileNotFoundException e) {
                throw JacksonIOException.construct(e);
            }
        }

        return new NestableValue<>(ctxt.readValue(p, classType));
    }
}
