package io.axoniq.axonserver.migration;

import io.axoniq.axonserver.migration.properties.SerializerProperties;
import io.axoniq.axonserver.migration.properties.SerializerType;
import org.axonframework.axonserver.connector.AxonServerConfiguration;
import org.axonframework.axonserver.connector.AxonServerConnectionManager;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.axonframework.serialization.xml.XStreamSerializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Marc Gathier
 */
@Configuration
public class AxonConfiguration {

    private final ApplicationContext applicationContext;

    public AxonConfiguration(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    public Serializer serializer(SerializerProperties serializerProperties) {
        if (SerializerType.JACKSON.equals(serializerProperties.getEvents())) {
            return JacksonSerializer.builder().build();
        }
        return XStreamSerializer.defaultSerializer();
    }


    @Bean
    public AxonServerConfiguration axonServerConfiguration() {
        AxonServerConfiguration configuration = new AxonServerConfiguration();
        configuration.setComponentName(clientName(applicationContext.getId()));
        return configuration;
    }

    private String clientName(String id) {
        if( id == null) return "AxonServerMigration";
        if (id.contains(":")) return id.substring(0, id.indexOf(':'));
        return id;
    }

    @Bean
    public AxonServerConnectionManager axonServerConnectionManager(AxonServerConfiguration axonServerConfiguration) {
        return AxonServerConnectionManager.builder()
                                          .axonServerConfiguration(axonServerConfiguration)
                                          .build();
    }


    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(StringToInstantConverter.INSTANCE);
        return new MongoCustomConversions(converters);
    }

    enum StringToInstantConverter implements Converter<String, Instant> {
        INSTANCE;
        @Override
        public Instant convert(String source) {
            return Instant.parse(source);
        }
    }
}
