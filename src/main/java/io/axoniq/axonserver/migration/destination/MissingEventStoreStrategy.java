package io.axoniq.axonserver.migration.destination;

import io.axoniq.axonserver.grpc.event.Event;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import javax.annotation.PostConstruct;

/**
 * {@link EventStoreStrategy} that is used when the configuration is missing. Will throw an error on boot immediately.
 */
@Service
@ConditionalOnProperty(value = "axoniq.migration.destination", matchIfMissing = true, havingValue = "false")
public class MissingEventStoreStrategy implements EventStoreStrategy {
    public static final String MIGRATION_DESTINATION_WAS_NOT_DEFINED = "Migration destination was not defined. Please supply the 'axoniq.migration.destination' property. Consult the README.md if necessary.";

    @PostConstruct
    public void throwMissingConfigurationError() {
        throw new IllegalStateException(MIGRATION_DESTINATION_WAS_NOT_DEFINED);
    }

    @Override
    public void storeEvents(List<Event> events) throws Exception {
        throw new IllegalStateException(MIGRATION_DESTINATION_WAS_NOT_DEFINED);
    }

    @Override
    public void appendSnapshot(Event snapshot) throws Exception {
        throw new IllegalStateException(MIGRATION_DESTINATION_WAS_NOT_DEFINED);
    }

    @Override
    public String getLastEventId() throws Exception {
        throw new IllegalStateException(MIGRATION_DESTINATION_WAS_NOT_DEFINED);
    }
}
