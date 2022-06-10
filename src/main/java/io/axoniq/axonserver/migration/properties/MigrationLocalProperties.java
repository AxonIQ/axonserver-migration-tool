package io.axoniq.axonserver.migration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("axoniq.migration.local")
@Configuration
@Getter
@Setter
public class MigrationLocalProperties {
    private String eventStorePath = "data";
}
