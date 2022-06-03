package io.axoniq.axonserver.migration.migrators;


import io.axoniq.axonserver.grpc.event.Event;
import io.axoniq.axonserver.migration.DomainEvent;
import io.axoniq.axonserver.migration.EventProducer;
import io.axoniq.axonserver.migration.SequenceProvider;
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

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


/**
 * Responsible for migrating the events from the database to Axon Server. Will keep looping until it either: - Reaches
 * the end of the event stream - Reaches a recent gap in global index numbers
 * <p>
 * The status of the migration is stored in the {@link MigrationStatusRepository}, so the tool can resume where it left
 * off the previous run.
 * <p>
 * You can disable the events migration by setting the {@code axoniq.migration.migrateEvents} property to
 * {@code false}.
 *
 * @author Marc Gathier
 * @author Mitchell Herrijgers
 */
@Component
@Profile("!test")
@ConditionalOnProperty(value = "axoniq.migration.migrateEvents", havingValue = "true", matchIfMissing = true)
public class EventMigrator extends AbstractMigrator {

    private final Logger logger = LoggerFactory.getLogger(EventMigrator.class);

    private final MigrationProperties migrationProperties;
    private final EventProducer eventProducer;
    private final AxonServerConnectionManager axonDBClient;
    private final MigrationStatusRepository migrationStatusRepository;
    private final SequenceProvider sequenceProvider;

    private final AtomicLong eventsMigrated = new AtomicLong();
    private final AtomicLong lastReported = new AtomicLong();

    public EventMigrator(MigrationProperties migrationProperties, EventProducer eventProducer, Serializer serializer,
                         MigrationStatusRepository migrationStatusRepository,
                         AxonServerConnectionManager axonDBClient,
                         SequenceProvider sequenceProvider) {
        super(serializer);
        this.migrationProperties = migrationProperties;
        this.eventProducer = eventProducer;
        this.axonDBClient = axonDBClient;
        this.migrationStatusRepository = migrationStatusRepository;
        this.sequenceProvider = sequenceProvider;
    }

    public void migrate() throws ExecutionException, InterruptedException, TimeoutException {
        MigrationStatus migrationStatus = migrationStatusRepository.findById(1L).orElse(new MigrationStatus());

        long lastProcessedToken = migrationStatus.getLastEventGlobalIndex();

        logger.info("Starting migration of event from globalIndex: {}, batchSize = {}",
                    lastProcessedToken,
                    migrationProperties.getBatchSize());

        while (true) {
            sequenceProvider.clearCache(false);
            List<? extends DomainEvent> result = eventProducer.findEvents(lastProcessedToken,
                                                                          migrationProperties.getBatchSize());
            if (result.isEmpty()) {
                logger.info("No more events found");
                return;
            }

            // Validate the result list and check for gaps.
            // If there is a gap, and the event is recent, it can indicate that a row has already been written
            // to the database but not committed yet. Abort this run.
            List<Long> recentGlobalIndexes = result.stream().filter(this::isRecentEvent)
                                                   .map(DomainEvent::getGlobalIndex)
                                                   .collect(Collectors.toList());
            long maxNonRecentEvent = result.stream().filter(s -> !isRecentEvent(s))
                                           .map(DomainEvent::getGlobalIndex)
                                           .max(Comparator.naturalOrder())
                                           .orElse(lastProcessedToken);
            for (int i = 1; i <= recentGlobalIndexes.size(); i++) {
                if (!recentGlobalIndexes.contains(maxNonRecentEvent + i)) {
                    logger.error("Missing event at: {}. Found global indexes in batch: {}",
                                 (maxNonRecentEvent + i),
                                 recentGlobalIndexes);
                    logger.error(
                            "This indicates that there is a gap in the database which occurred recently. Since we cannot guarantee data ordering, we are stopping the migration.");
                    return;
                }
            }

            DomainEvent lastEntry = result.get(result.size() - 1);
            lastProcessedToken = lastEntry.getGlobalIndex();

            List<Event> events = result
                    .stream()
                    .filter(this::isNotAnIgnoredEventType)
                    .map(this::buildEvent)
                    .collect(Collectors.toList());

            // Check for errors and log additional info
            events.stream().filter(e -> e.getAggregateIdentifier().length() > 0)
                  .collect(Collectors.groupingBy(Event::getAggregateIdentifier))
                  .forEach((aggregate, aggregateEvents) -> {
                      aggregateEvents.stream()
                                     .collect(Collectors.groupingBy(Event::getAggregateSequenceNumber))
                                     .forEach((seqNumber, seqNumberEvents) -> {
                                         if (seqNumberEvents.size() > 1) {
                                             throw new IllegalStateException(
                                                     "Multiple events with same sequenceNumber " + seqNumber
                                                             + " detected for aggregate " + aggregate
                                                             + ". All sequence numbers for this aggregate: "
                                                             + aggregateEvents.stream()
                                                                              .map(Event::getAggregateSequenceNumber)
                                                                              .collect(Collectors.toList())
                                             );
                                         }
                                     });
                  });

            storeEvents(events);

            if (eventsMigrated.addAndGet(events.size()) > lastReported.get() + 1000) {
                lastReported.set(eventsMigrated.intValue());
                logger.info("Migrated {} events, currently at global index {}",
                            eventsMigrated.get(),
                            lastProcessedToken);
            }
            migrationStatus.setLastEventGlobalIndex(lastProcessedToken);
            migrationStatusRepository.save(migrationStatus);
        }
    }

    private boolean isNotAnIgnoredEventType(DomainEvent entry) {
        return !migrationProperties.getIgnoredEvents().contains(entry.getPayloadType());
    }

    private Event buildEvent(final DomainEvent entry) {
        Event.Builder eventBuilder = Event.newBuilder()
                                          .setPayload(toPayload(entry))
                                          .setMessageIdentifier(entry.getEventIdentifier());

        if (entry.getType() != null) {
            eventBuilder.setAggregateType(entry.getType())
                        .setAggregateSequenceNumber(sequenceProvider.getNextSequenceForAggregate(entry.getAggregateIdentifier()))
                        .setAggregateIdentifier(entry.getAggregateIdentifier());
        }

        eventBuilder.setTimestamp(entry.getTimeStampAsLong());
        convertMetadata(entry.getMetaData(), eventBuilder);
        return eventBuilder.build();
    }

    private void storeEvents(List<Event> events) throws ExecutionException, InterruptedException, TimeoutException {
        if (events.isEmpty()) {
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Storing {} events", events.size());
        }

        try {
            axonDBClient.getConnection()
                        .eventChannel()
                        .appendEvents(events.toArray(new Event[0]))
                        .get(30, TimeUnit.SECONDS);
        } catch (Exception exception) {
            List<String> structure = events.stream()
                                           .map(e -> String.format("%s___%d",
                                                                   e.getAggregateIdentifier(),
                                                                   e.getAggregateSequenceNumber()))
                                           .collect(Collectors.toList());
            logger.error("Exception while storing. The event list has the following structure: {}", structure);
            // Clear any cache on the sequence numbers, it's unreliable now.
            sequenceProvider.clearCache(true);
            throw exception;
        }
    }

    private boolean isRecentEvent(DomainEvent entry) {
        return entry.getTimeStampAsLong() > System.currentTimeMillis() - migrationProperties.getRecentMillis();
    }
}
