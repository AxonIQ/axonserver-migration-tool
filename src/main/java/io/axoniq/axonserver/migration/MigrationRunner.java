/*
 * Copyright (c) 2010-2023. AxonIQ
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.axoniq.axonserver.migration;


import io.axoniq.axonserver.migration.migrators.Migrator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;


/**
 * Runs the available {@link Migrator} beans. Which are available depend on the property configuration.
 * <p>
 * If a continuous migration is specified through {@link MigrationBaseProperties#isContinuous()}  the migration will run
 * indefinitely, waiting for {@link MigrationBaseProperties#getContinuousTimeout()} milliseconds before trying again.
 *
 * @author Marc Gathier
 * @author Mitchell Herrijgers
 * @see io.axoniq.axonserver.migration.migrators.EventMigrator
 * @see io.axoniq.axonserver.migration.migrators.SnapshotMigrator
 */
@Component
@RequiredArgsConstructor
public class MigrationRunner implements CommandLineRunner {

    private final Logger logger = LoggerFactory.getLogger(MigrationRunner.class);

    private final List<Migrator> migrators;
    private final MigrationBaseProperties migrationProperties;
    private final ApplicationContext context;

    @Override
    public void run(String... options) throws Exception {
        if (migrators.isEmpty()) {
            throw new IllegalArgumentException("There are no Migrators to run. Did you disable all migrations?");
        }
        do {
            try {
                for (Migrator migrator : migrators) {
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
            } catch (Exception e) {
                logger.error("Unknown error during migration", e);
            }
        } while (migrationProperties.isContinuous());

        logger.info("Migration completed");
        SpringApplication.exit(context);

        // Sleep here to wait for H2 database writes to complete and other threads to shut down gracefully...
        Thread.sleep(2000);
        System.exit(0);
    }
}
