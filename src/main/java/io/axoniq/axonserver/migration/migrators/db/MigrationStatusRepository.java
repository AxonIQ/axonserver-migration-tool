package io.axoniq.axonserver.migration.migrators.db;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @see MigrationStatus
 *
 * @author Marc Gathier
 */
public interface MigrationStatusRepository extends JpaRepository<MigrationStatus, Long> {
}
