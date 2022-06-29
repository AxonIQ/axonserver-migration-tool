package io.axoniq.axonserver.migration.source.jpa;


import io.axoniq.axonserver.migration.source.DomainEvent;
import io.axoniq.axonserver.migration.source.EventProducer;
import io.axoniq.axonserver.migration.source.SnapshotEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Produces events when defining the migration source as {@code RDBMS}. Queries the database using JPA to look up events
 * and snapshots, that can then be migrated.
 *
 * @author Marc Gathier
 * @author Mitchell Herrijgers
 */
@Component
@ConditionalOnProperty(value = "axoniq.migration.source", havingValue = "RDBMS")
@Transactional(readOnly = true, transactionManager = "eventStoreTransactionManager")
public class JpaEventProducer implements EventProducer {

    @PersistenceContext(name = "eventstore")
    private EntityManager entityManager;


    @Override
    public List<? extends DomainEvent> findEvents(long lastProcessedToken, int batchSize) {
        return entityManager.createNamedQuery("DomainEventEntry.findByGlobalIndex", DomainEventEntry.class)
                            .setParameter("lastToken", lastProcessedToken)
                            .setMaxResults(batchSize)
                            .getResultList();
    }

    @Override
    public List<? extends SnapshotEvent> findSnapshots(String lastProcessedTimestamp, int batchSize) {
        return entityManager.createNamedQuery("SnapshotEventEntry.findByTimestamp", SnapshotEventEntry.class)
                            .setParameter("lastTimeStamp", lastProcessedTimestamp)
                            .setMaxResults(batchSize)
                            .getResultList();
    }

    @Override
    public long getMinIndex() {
        return entityManager.createNamedQuery("DomainEventEntry.minGlobalIndex", Long.class)
                            .getSingleResult();
    }

    @Override
    public long getMaxIndex() {
        return entityManager.createNamedQuery("DomainEventEntry.maxGlobalIndex", Long.class)
                            .getSingleResult();
    }
}
