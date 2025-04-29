package io.axoniq.axonserver.migration.source.axonserver;

import org.axonframework.axonserver.connector.AxonServerConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "axoniq.migration.source.axonserver")
@Component("sourceAxonServerConfiguration")
public class SourceAxonServerConfiguration extends AxonServerConfiguration {

}
