package io.axoniq.axonserver.migration.destination.remote;

import lombok.RequiredArgsConstructor;
import org.axonframework.axonserver.connector.AxonServerConfiguration;
import org.axonframework.axonserver.connector.AxonServerConnectionManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that defines the correct beans to using a {@code LOCAL} destination.
 *
 * @author Mitchell Herrijgers
 */
@Configuration
@ConditionalOnProperty(value = "axoniq.migration.destination", havingValue = "REMOTE")
@RequiredArgsConstructor
public class RemoteEventStoreConfiguration {
    private final ApplicationContext applicationContext;

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
}
