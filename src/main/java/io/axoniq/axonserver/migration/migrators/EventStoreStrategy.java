package io.axoniq.axonserver.migration.migrators;

import io.axoniq.axonserver.grpc.event.Event;

import java.util.List;

public interface EventStoreStrategy {

    void storeEvents(List<Event> events) throws Exception;

    void appendSnapshot(Event snapshot) throws Exception;

    String getLastEventId() throws Exception;

    default Long getNextSequenceNumber(String aggregate, Long current) throws Exception {
        return current;
    }

    default void rollback() {

    }
}
