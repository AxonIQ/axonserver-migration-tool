package io.axoniq.axonserver.migration.db;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Mitchell Herrijgers
 */
public interface MigrationAggregateStatusRepository extends JpaRepository<MigrationAggregateStatus, Long> {
    MigrationAggregateStatus findByAggregateIdentifier(String aggregateIdentifier);
}
