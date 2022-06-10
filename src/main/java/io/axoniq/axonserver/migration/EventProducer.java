package io.axoniq.axonserver.migration;

import java.util.List;

/**
 * @author Marc Gathier
 */
public interface EventProducer {

    List<? extends DomainEvent> findEvents(long lastProcessedToken, int batchSize);

    List<? extends SnapshotEvent> findSnapshots(String lastProcessedTimestamp, int batchSize);

    default long getMaxIndex() {
        return -1; // -1 indicates not supported
    }

    default long getMinIndex() {
        return -1; // -1 indicates not supported
    }
}
