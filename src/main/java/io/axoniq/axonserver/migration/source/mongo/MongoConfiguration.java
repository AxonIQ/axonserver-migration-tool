package io.axoniq.axonserver.migration.source.mongo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Import(MongoAutoConfiguration.class)
@ConditionalOnProperty(value = "axoniq.migration.source", havingValue = "MONGO")
public class MongoConfiguration {
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
