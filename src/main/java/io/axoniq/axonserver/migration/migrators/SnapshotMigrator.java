package io.axoniq.axonserver.migration.migrators;


import io.axoniq.axonserver.grpc.event.Event;
import io.axoniq.axonserver.migration.EventProducer;
import io.axoniq.axonserver.migration.SnapshotEvent;
import io.axoniq.axonserver.migration.db.MigrationStatus;
import io.axoniq.axonserver.migration.db.MigrationStatusRepository;
import io.axoniq.axonserver.migration.properties.MigrationProperties;
import org.axonframework.axonserver.connector.AxonServerConnectionManager;
import org.axonframework.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Responsible for migrating the snapshots from the database to Axon Server.
 * <p>
 * You can disable the events migration by setting the {@code axoniq.migration.migrateSnapshots} property to
 * {@code false}.
 *
 * @author Marc Gathier
 */
@Profile("!test")
@Component
@ConditionalOnProperty(value = "axoniq.migration.migrateSnapshots", havingValue = "true", matchIfMissing = true)
public class SnapshotMigrator extends AbstractMigrator {

    private final MigrationProperties migrationProperties;
    private final EventProducer eventProducer;
    private final AxonServerConnectionManager axonDBClient;
    private final MigrationStatusRepository migrationStatusRepository;
    private final Logger logger = LoggerFactory.getLogger(SnapshotMigrator.class);

    private final AtomicLong snapshotsMigrated = new AtomicLong();

    public SnapshotMigrator(MigrationProperties migrationProperties,
                            EventProducer eventProducer,
                            Serializer serializer,
                            MigrationStatusRepository migrationStatusRepository,
                            AxonServerConnectionManager axonDBClient) {
        super(serializer);
        this.migrationProperties = migrationProperties;
        this.eventProducer = eventProducer;
        this.axonDBClient = axonDBClient;
        this.migrationStatusRepository = migrationStatusRepository;
    }

    public void migrate() throws InterruptedException, ExecutionException, TimeoutException {
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
                                                      .setPayload(toPayload(entry))
                                                      .setAggregateSequenceNumber(entry.getSequenceNumber())
                                                      .setMessageIdentifier(entry.getEventIdentifier())
                                                      .setAggregateType(entry.getType());

                    eventBuilder.setTimestamp(entry.getTimeStampAsLong());
                    convertMetadata(entry.getMetaData(), eventBuilder);

                    axonDBClient.getConnection().eventChannel().appendSnapshot(eventBuilder.build()).get(30,
                                                                                                         TimeUnit.SECONDS);
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
