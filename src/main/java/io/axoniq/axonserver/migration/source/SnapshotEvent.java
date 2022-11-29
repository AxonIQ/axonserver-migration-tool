package io.axoniq.axonserver.migration.source;

/**
 * @author Marc Gathier
 */
public interface SnapshotEvent extends BaseEvent {
    String getType();

    String getAggregateIdentifier();

    long getSequenceNumber();

    String getTimeStamp();

}
