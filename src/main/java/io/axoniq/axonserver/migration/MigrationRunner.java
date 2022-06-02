package io.axoniq.axonserver.migration;


import io.axoniq.axonserver.migration.migrators.AbstractMigrator;
import io.axoniq.axonserver.migration.properties.MigrationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;


/**
 * Runs the available {@link AbstractMigrator} beans. Which are available depend on the property configuration.
 *
 * If a continuous migration is specified through {@link MigrationProperties#isContinuous()} the migration will run
 * indefinitely, waiting for {@link MigrationProperties#getContinuousTimeout()} milliseconds before trying again.
 *
 * @see io.axoniq.axonserver.migration.migrators.EventMigrator
 * @see io.axoniq.axonserver.migration.migrators.SnapshotMigrator
 *
 * @author Marc Gathier
 * @author Mitchell Herrijgers
 */
@Profile("!test")
@Component
public class MigrationRunner implements CommandLineRunner {

    private final List<AbstractMigrator> migrators;
    private final MigrationProperties migrationProperties;
    private final Logger logger = LoggerFactory.getLogger(MigrationRunner.class);

    private final ApplicationContext context;

    public MigrationRunner(List<AbstractMigrator> migrators,
                           MigrationProperties migrationProperties,
                           ApplicationContext context) {
        this.migrators = migrators;
        this.migrationProperties = migrationProperties;
        this.context = context;
    }

    @Override
    public void run(String... options) throws Exception {
        if (migrators.isEmpty()) {
            throw new IllegalArgumentException("There are no Migrators to run. Did you disable all migrations?");
        }
        do {
            try {
                for (AbstractMigrator migrator : migrators) {
                    migrator.migrate();
                }
                if(migrationProperties.isContinuous()) {
                    Thread.sleep(migrationProperties.getContinuousTimeout());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Migration interrupted");
            } catch (ExecutionException executionException) {
                logger.error("Error during migration", executionException.getCause());
            } catch (TimeoutException e) {
                logger.error("Error during migration", e);
            }
        } while (migrationProperties.isContinuous());

        logger.info("Migration completed");
        SpringApplication.exit(context);
    }
}
