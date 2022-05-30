package io.axoniq.axonserver.migration.db;

import javax.persistence.*;

/**
 * Keeps track of the last known aggregate sequence number to prevent gaps
 *
 * @author Mitchell Herrijgers
 */
@Entity
@Cacheable(false)
@Table(indexes = @Index(columnList = "aggregateType, aggregateIdentifier", unique = true)
)
public class MigrationAggregateStatus {
    @Id
    private String aggregateIdentifier;

    private String aggregateType;
    private long lastSequenceNumber = -1;


    public MigrationAggregateStatus() {
    }

    public MigrationAggregateStatus(final String aggregateType, final String aggregateIdentifier) {
        this.aggregateType = aggregateType;
        this.aggregateIdentifier = aggregateIdentifier;
    }

    public long nextSequenceNumber() {
        lastSequenceNumber += 1;
        return lastSequenceNumber;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateIdentifier() {
        return aggregateIdentifier;
    }

    public long getLastSequenceNumber() {
        return lastSequenceNumber;
    }
}
