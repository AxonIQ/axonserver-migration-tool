package io.axoniq.axonserver.migration.serialisation;

import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.axonframework.serialization.xml.XStreamSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Marc Gathier
 */
@Configuration
public class EventSerializerConfiguration {
    @Bean
    public Serializer serializer(SerializerProperties serializerProperties) {
        if (SerializerType.JACKSON.equals(serializerProperties.getEvents())) {
            return JacksonSerializer.builder().build();
        }
        return XStreamSerializer.defaultSerializer();
    }
}
