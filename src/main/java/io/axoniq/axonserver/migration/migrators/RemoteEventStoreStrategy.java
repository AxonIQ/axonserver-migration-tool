package io.axoniq.axonserver.migration.migrators;

import io.axoniq.axonserver.grpc.event.Event;
import lombok.RequiredArgsConstructor;
import org.axonframework.axonserver.connector.AxonServerConnectionManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
@ConditionalOnProperty(value = "axoniq.migration.method", havingValue = "REMOTE", matchIfMissing = true)
public class RemoteEventStoreStrategy implements EventStoreStrategy {

    private final AxonServerConnectionManager axonDBClient;

    @Override
    public void storeEvents(List<Event> events) throws Exception {
        axonDBClient.getConnection()
                    .eventChannel()
                    .appendEvents(events.toArray(new Event[0]))
                    .get(30, TimeUnit.SECONDS);
    }

    @Override
    public void appendSnapshot(Event snapshot) throws Exception {
        axonDBClient.getConnection().eventChannel().appendSnapshot(snapshot).get(30, TimeUnit.SECONDS);
    }
}
