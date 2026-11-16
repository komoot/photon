package de.komoot.photon.analyzer.config.nested;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.Deserializers;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.type.CollectionType;

import java.nio.file.Path;

@NullMarked
public class NestedConfigModule extends SimpleModule {
    class NestedConfigDeserializer extends Deserializers.Base {

        @Override
        public boolean hasDeserializerFor(DeserializationConfig config, Class<?> valueType) {
            return (valueType == NestableValue.class) || (valueType == NestableArray.class);
        }

        @Override
        public ValueDeserializer<?> findBeanDeserializer(JavaType type, DeserializationConfig config, BeanDescription.Supplier beanDescRef) {
            if (type.getRawClass() == NestableValue.class) {
                return new NestedConfigValueDeserializer<>(resolver, type.containedType(0).getRawClass());
            }

            return super.findBeanDeserializer(type, config, beanDescRef);
        }

        @Override
        @Nullable
        public ValueDeserializer<?> findCollectionDeserializer(CollectionType type, DeserializationConfig config, BeanDescription.Supplier beanDescRef, TypeDeserializer elementTypeDeserializer, ValueDeserializer<?> elementDeserializer) {
            if (type.getRawClass() == NestableArray.class) {
                return new NestedConfigArrayDeserializer<>(resolver, type.containedType(0).getRawClass());
            }

            return null;
        }
    }

    private final NestedConfigResolver resolver;

    public NestedConfigModule(@Nullable Path baseDir) {
        super("NestedConfigModule");
        this.resolver = new NestedConfigResolver(baseDir);
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.addDeserializers(new NestedConfigDeserializer());
    }
}