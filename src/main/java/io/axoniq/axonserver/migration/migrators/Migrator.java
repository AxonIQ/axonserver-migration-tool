package io.axoniq.axonserver.migration.migrators;

/**
 * Represents a process that should be run during the migration.
 * Depending on configuration there can be multiple beans defined, which are run in succession.
 *
 * @author Mitchell Herrijgers
 */
public interface Migrator {
    void migrate() throws Exception;
}
