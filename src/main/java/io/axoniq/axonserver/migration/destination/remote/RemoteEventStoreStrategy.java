package io.axoniq.axonserver.migration.destination.remote;

import io.axoniq.axonserver.connector.event.EventStream;
import io.axoniq.axonserver.grpc.event.Event;
import io.axoniq.axonserver.migration.destination.EventStoreStrategy;
import lombok.RequiredArgsConstructor;
import org.axonframework.axonserver.connector.AxonServerConnectionManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
@ConditionalOnProperty(value = "axoniq.migration.destination", havingValue = "REMOTE", matchIfMissing = true)
public class RemoteEventStoreStrategy implements EventStoreStrategy {

    private final AxonServerConnectionManager axonDBClient;
    private final Map<String, Long> sequenceMap = new HashMap<>();

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

    @Override
    public String getLastEventId() throws Exception {
        Long lastToken = axonDBClient.getConnection().eventChannel().getLastToken().get();
        if (lastToken == null || lastToken == -1) {
            return null;
        }
        try (EventStream stream = axonDBClient.getConnection().eventChannel().openStream(lastToken - 1, 1)) {
            return stream.next().getEvent().getMessageIdentifier();
        }
    }

    @Override
    public Long getNextSequenceNumber(String aggregate, Long current) throws Exception {
        if(sequenceMap.size() > 50000) {
            sequenceMap.clear();
        }
        Long asCurrent = sequenceMap
                .computeIfAbsent(aggregate, agg ->
                                 {
                                     try {
                                         return axonDBClient.getConnection().eventChannel().findHighestSequence(aggregate).get();
                                     } catch (InterruptedException e) {
                                         throw new RuntimeException(e);
                                     } catch (ExecutionException e) {
                                         throw new RuntimeException(e);
                                     }
                                 }
                );
        sequenceMap.put(aggregate, asCurrent + 1);
        return asCurrent + 1;
    }

    @Override
    public void rollback() {
        this.sequenceMap.clear();
    }
}
