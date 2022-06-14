package io.axoniq.axonserver.migration.migrators;

import io.axoniq.axonserver.grpc.event.Event;
import io.axoniq.axonserver.localstorage.EventStorageEngine;
import io.axoniq.axonserver.localstorage.SerializedEventWithToken;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.util.CloseableIterator;
import org.springframework.stereotype.Service;

import java.util.List;
import javax.annotation.PreDestroy;

@RequiredArgsConstructor
@Service
@ConditionalOnProperty(value = "axoniq.migration.method", havingValue = "LOCAL")
@Profile("!test")
public class LocalEventStoreStrategy implements EventStoreStrategy {

    private final EventStorageEngine eventStorageEngine;

    @PreDestroy
    public void close() {
        eventStorageEngine.close(false);
    }

    @Override
    public void storeEvents(List<Event> events) throws Exception {
        eventStorageEngine.store(events);
    }

    @Override
    public void appendSnapshot(Event snapshot) throws Exception {
        throw new IllegalStateException("Storing snapshot using local migration is currently not supported");
    }

    @Override
    public String getLastEventId() throws Exception {
        long lastToken = eventStorageEngine.getLastToken();
        try (CloseableIterator<SerializedEventWithToken> iterator = eventStorageEngine.getGlobalIterator(lastToken)) {
            SerializedEventWithToken event = iterator
                    .stream().findFirst()
                    .orElse(null);
            if (event == null || event.getSerializedEvent() == null) {

                return null;
            }
            return event
                    .getSerializedEvent().getIdentifier();
        }
    }
}
