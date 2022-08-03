package io.axoniq.axonserver.migration.destination.local;

import io.axoniq.axonserver.grpc.event.Event;
import io.axoniq.axonserver.localstorage.EventStorageEngine;
import io.axoniq.axonserver.localstorage.SerializedEventWithToken;
import io.axoniq.axonserver.migration.destination.EventStoreStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.util.CloseableIterator;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import javax.annotation.PreDestroy;

@Service
@ConditionalOnProperty(value = "axoniq.migration.destination", havingValue = "LOCAL")
public class LocalEventStoreStrategy implements EventStoreStrategy {

    private final EventStorageEngine eventStorageEngine;
    private final EventStorageEngine snapshotStorageEngine;

    public LocalEventStoreStrategy(@Qualifier("events") EventStorageEngine eventStorageEngine,
                                   @Qualifier("snapshots") EventStorageEngine snapshotStorageEngine) {
        this.eventStorageEngine = eventStorageEngine;
        this.snapshotStorageEngine = snapshotStorageEngine;
    }

    @PreDestroy
    public void close() {
        eventStorageEngine.close(false);
        snapshotStorageEngine.close(false);
    }

    @Override
    public void storeEvents(List<Event> events) throws Exception {
        eventStorageEngine.store(events);
    }

    @Override
    public void appendSnapshot(Event snapshot) throws Exception {
        snapshotStorageEngine.store(Collections.singletonList(snapshot));
    }

    @Override
    public String getLastEventId() throws Exception {
        long lastToken = eventStorageEngine.getLastToken();
        if (lastToken == -1) {
            return null;
        }
        try (CloseableIterator<SerializedEventWithToken> iterator = eventStorageEngine.getGlobalIterator(lastToken)) {
            SerializedEventWithToken event = iterator
                    .stream().findFirst()
                    .orElse(null);
            if (event == null || event.getSerializedEvent() == null) {

                return null;
            }
            return event
                    .getSerializedEvent().getIdentifier();
        } catch (NullPointerException e) {
            return null;
        }
    }
}
