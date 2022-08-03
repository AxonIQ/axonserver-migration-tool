package io.axoniq.axonserver.migration;

import io.axoniq.axonserver.config.DefaultSystemInfoProvider;
import io.axoniq.axonserver.config.SystemInfoProvider;
import io.axoniq.axonserver.localstorage.file.EmbeddedDBProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties(EmbeddedDBProperties.class)
@EnableScheduling
public class BaseConfiguration {

    @Bean
    public SystemInfoProvider systemInfoProvider(Environment environment) {
        return new DefaultSystemInfoProvider(environment);
    }
}
