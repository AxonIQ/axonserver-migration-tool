package io.axoniq.axonserver.migration.source;

import java.util.List;

/**
 *
 *
 * @author Marc Gathier
 * @author Mitchell Herrijgers
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
