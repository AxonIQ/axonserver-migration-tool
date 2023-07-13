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

package io.axoniq.axonserver.migration.migrators;


import io.axoniq.axonserver.grpc.event.Event;
import io.axoniq.axonserver.migration.MigrationBaseProperties;
import io.axoniq.axonserver.migration.destination.EventStoreStrategy;
import io.axoniq.axonserver.migration.migrators.db.MigrationStatus;
import io.axoniq.axonserver.migration.migrators.db.MigrationStatusRepository;
import io.axoniq.axonserver.migration.serialisation.EventSerializer;
import io.axoniq.axonserver.migration.source.EventProducer;
import io.axoniq.axonserver.migration.source.SnapshotEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Responsible for migrating the snapshots from the database to Axon Server.
 * <p>
 * You can disable the events migration by setting the {@code axoniq.migration.migrateSnapshots} property to
 * {@code false}.
 *
 * @author Marc Gathier
 */
@Component
@ConditionalOnProperty(value = "axoniq.migration.migrateSnapshots", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class SnapshotMigrator implements Migrator {

    private final MigrationBaseProperties migrationProperties;
    private final EventProducer eventProducer;
    private final MigrationStatusRepository migrationStatusRepository;
    private final EventSerializer eventSerializer;
    private final EventStoreStrategy eventStoreStrategy;
    private final Logger logger = LoggerFactory.getLogger(SnapshotMigrator.class);

    private final AtomicLong snapshotsMigrated = new AtomicLong();

    public void migrate() throws Exception {
        MigrationStatus migrationStatus = migrationStatusRepository.findById(1L).orElse(new MigrationStatus());

        String lastProcessedTimestamp = migrationStatus.getLastSnapshotTimestamp();
        String lastEventId = migrationStatus.getLastSnapshotEventId();

        boolean keepRunning = true;

        logger.info("Starting migration of snapshots from timestamp: {}, batchSize = {}",
                    lastProcessedTimestamp,
                    migrationProperties.getBatchSize());

        try {
            while (keepRunning) {
                keepRunning = false;
                List<? extends SnapshotEvent> result = eventProducer.findSnapshots(lastProcessedTimestamp,
                                                                                   migrationProperties.getBatchSize());
                if (result.isEmpty()) {
                    logger.info("No more snapshots found");
                    return;
                }
                boolean lastFound = (lastEventId == null);
                for (SnapshotEvent entry : result) {
                    if (!lastFound) {
                        lastFound =
                                entry.getTimeStamp().compareTo(lastProcessedTimestamp) > 0 || entry.getEventIdentifier()
                                                                                                   .equals(lastEventId);
                        continue;
                    }

                    Event.Builder eventBuilder = Event.newBuilder()
                                                      .setAggregateIdentifier(entry.getAggregateIdentifier())
                                                      .setPayload(eventSerializer.toPayload(entry))
                                                      .setAggregateSequenceNumber(entry.getSequenceNumber())
                                                      .setMessageIdentifier(entry.getEventIdentifier())
                                                      .setAggregateType(entry.getType());

                    eventBuilder.setTimestamp(entry.getTimeStampAsLong());
                    eventSerializer.convertMetadata(entry.getMetaData(), eventBuilder);
                    eventStoreStrategy.appendSnapshot(eventBuilder.build());

                    lastProcessedTimestamp = entry.getTimeStamp();
                    lastEventId = entry.getEventIdentifier();
                    if (snapshotsMigrated.incrementAndGet() % 1000 == 0) {
                        logger.debug("Migrated {} snapshots", snapshotsMigrated.get());
                    }
                    keepRunning = true;
                }
            }
        } finally {
            migrationStatus.setLastSnapshotEventId(lastEventId);
            migrationStatus.setLastSnapshotTimestamp(lastProcessedTimestamp);
            migrationStatusRepository.save(migrationStatus);
        }
    }
}
