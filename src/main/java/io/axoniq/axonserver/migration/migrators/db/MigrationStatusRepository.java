package io.axoniq.axonserver.migration.migrators.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @see MigrationStatus
 *
 * @author Marc Gathier
 */
@Transactional(transactionManager = "migrationEntityManagerFactory")
public interface MigrationStatusRepository extends JpaRepository<MigrationStatus, Long> {
}
