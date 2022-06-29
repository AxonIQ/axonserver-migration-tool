package io.axoniq.axonserver.migration.source.jpa;

import io.axoniq.axonserver.migration.source.DomainEvent;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Entity representing an event in the RDBMS store.
 *
 * @author Marc Gathier
 */
@Entity
@Table(
        indexes = {@Index(
                columnList = "aggregateIdentifier,sequenceNumber",
                unique = true
        )}
)
@NamedQuery(name = "DomainEventEntry.findByGlobalIndex", query = "select e from DomainEventEntry e where e.globalIndex > :lastToken order by e.globalIndex asc")
@NamedQuery(name = "DomainEventEntry.maxGlobalIndex", query = "select max(e.globalIndex) from DomainEventEntry e")
@NamedQuery(name = "DomainEventEntry.minGlobalIndex", query = "select min(e.globalIndex) from DomainEventEntry e")
public class DomainEventEntry extends BaseEventEntry implements DomainEvent {
    @Id
    @GeneratedValue
    private long globalIndex;

    @Basic
    private String type;
    @Basic(optional = false)
    private String aggregateIdentifier;
    @Basic(optional = false)
    private long sequenceNumber;

    public long getGlobalIndex() {
        return globalIndex;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getAggregateIdentifier() {
        return aggregateIdentifier;
    }

    @Override
    public long getSequenceNumber() {
        return sequenceNumber;
    }

}
