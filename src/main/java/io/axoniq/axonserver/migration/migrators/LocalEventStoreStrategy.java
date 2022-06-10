package io.axoniq.axonserver.migration.migrators;

import io.axoniq.axonserver.grpc.event.Event;
import io.axoniq.axonserver.localstorage.EventStorageEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
@ConditionalOnProperty(value = "axoniq.migration.method", havingValue = "LOCAL")
public class LocalEventStoreStrategy implements EventStoreStrategy {

    private final EventStorageEngine eventStorageEngine;

    @Override
    public void storeEvents(List<Event> events) throws Exception {
        eventStorageEngine.store(events);
    }

    @Override
    public void appendSnapshot(Event snapshot) throws Exception {
        throw new IllegalStateException("Storing snapshot using local migration is currently not supported");
    }
}
