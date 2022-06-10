package io.axoniq.axonserver.migration.jpa;

import io.axoniq.axonserver.migration.DomainEvent;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
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
@NamedQuery(name = "DomainEventEntry.countAboveGlobalIndex", query = "select count(e) from DomainEventEntry e where e.globalIndex > :lastToken")
@NamedQuery(name = "DomainEventEntry.countUnderGlobalIndex", query = "select count(e) from DomainEventEntry e where e.globalIndex <= :lastToken")
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
