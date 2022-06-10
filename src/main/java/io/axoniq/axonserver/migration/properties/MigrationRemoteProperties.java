package io.axoniq.axonserver.migration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("axoniq.migration.remote")
@Configuration
@Getter
@Setter
public class MigrationRemoteProperties {
    private String skippedEventsFile = "skipped_events.db";

}
