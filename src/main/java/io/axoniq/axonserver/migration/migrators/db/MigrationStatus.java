package io.axoniq.axonserver.migration.migrators.db;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Entity representing the current status of the migration. Saved using an H2 database to allow the migration tool to
 * have a consistent state between runs.
 *
 * @author Marc Gathier
 */
@Entity
@Cacheable(false)
@NoArgsConstructor
@Getter
@Setter
public class MigrationStatus {
    @Id
    private long id = 1;

    private long lastEventGlobalIndex = -1;
    private String lastSnapshotTimestamp = "1970-01-01T00:00:00Z";
    private String lastSnapshotEventId;
}
