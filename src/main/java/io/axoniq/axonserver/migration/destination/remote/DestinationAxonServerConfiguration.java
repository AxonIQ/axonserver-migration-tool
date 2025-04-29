package io.axoniq.axonserver.migration.destination.remote;

import org.axonframework.axonserver.connector.AxonServerConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "axoniq.migration.destination.axonserver")
@Component("destinationAxonServerConfiguration")
public class DestinationAxonServerConfiguration extends AxonServerConfiguration {

}
