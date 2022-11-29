package io.axoniq.axonserver.migration.migrators;

import io.axoniq.axonserver.grpc.event.Event;
import io.axoniq.axonserver.migration.MigrationBaseProperties;
import io.axoniq.axonserver.migration.destination.EventStoreStrategy;
import io.axoniq.axonserver.migration.migrators.db.MigrationStatus;
import io.axoniq.axonserver.migration.migrators.db.MigrationStatusRepository;
import io.axoniq.axonserver.migration.serialisation.EventSerializer;
import io.axoniq.axonserver.migration.source.DomainEvent;
import io.axoniq.axonserver.migration.source.EventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Migrates the events from an {@link EventProducer} to an {@link EventStoreStrategy} depending on the configuration.
 * <p>
 * Will evaluate on the first run whether the previous run completed a batch only partially to prevent double events.
 * Will stop evaluation when it hits a gaps during a recent period (by default 10 seconds). This means we've reached the
 * end of the event store, and we cannot guaranty the global index ordering, due to the commit order in the database
 * being different (or transactions not being completed yet)
 *
 * @author Marc Gathier
 * @author Mitchell Herrijgers
 */
@RequiredArgsConstructor
@Service
@Slf4j
@ConditionalOnProperty(value = "axoniq.migration.migrateEvents", havingValue = "true", matchIfMissing = true)
public class EventMigrator implements Migrator {

    private final MigrationBaseProperties properties;
    private final EventProducer eventProducer;
    private final EventSerializer eventSerializer;
    private final MigrationStatusRepository migrationStatusRepository;
    private final EventStoreStrategy eventStoreStrategy;
    private final EventMigratorStatisticsReporter reporter;

    public void migrate() throws Exception {
        MigrationStatus migrationStatus = migrationStatusRepository.findById(1L).orElse(new MigrationStatus());

        long lastProcessedToken = migrationStatus.getLastEventGlobalIndex();
        reporter.initialize(lastProcessedToken);

        String lastEventId = eventStoreStrategy.getLastEventId();
        boolean isFirstRun = true;

        while (true) {
            List<? extends DomainEvent> result = eventProducer.findEvents(lastProcessedToken,
                                                                          properties.getBatchSize());
            if (result.isEmpty()) {
                log.info("No more events found");
                return;
            }

            // Check if part of the previous batch was written. This can happen during a kill of the tool, with the H2 database not updating correctly
            if (isFirstRun && lastEventId != null) {
                Optional<? extends DomainEvent> matchingEventOptional = result.stream()
                                                                              .filter(e -> Objects.equals(e.getEventIdentifier(),
                                                                                                          lastEventId))
                                                                              .findFirst();
                if (matchingEventOptional.isPresent()) {
                    DomainEvent matchingEvent = matchingEventOptional.get();
                    int index = result.indexOf(matchingEvent);
                    log.info(
                            "Detected partially written batch because event id {}, found at index {}, was already written last time. Filtering the events to correct. ",
                            lastEventId,
                            index);
                    result = result.subList(index + 1, result.size());
                }
                isFirstRun = false;
                if(result.isEmpty()) {
                    continue;
                }
            }

            if (recentGapIsPresent(lastProcessedToken, result)) {
                return;
            }

            DomainEvent lastEntry = result.get(result.size() - 1);
            lastProcessedToken = lastEntry.getGlobalIndex();

            List<Event> events = result
                    .stream()
                    .map(e -> {
                        try {
                            return buildEvent(e);
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            storeEvents(events);

            migrationStatus.setLastEventGlobalIndex(lastProcessedToken);
            migrationStatusRepository.save(migrationStatus);
            reporter.reportBatchSaved(lastProcessedToken, result.size(), events.size());
        }
    }

    /**
     * Validate the result list and check for gaps. If there is a gap, and the event is recent, it can indicate that a
     * row has already been written to the database but not committed yet. This run should be aborted
     */
    private boolean recentGapIsPresent(long lastProcessedToken, List<? extends DomainEvent> result) {
        List<Long> recentGlobalIndexes = result.stream().filter(this::isRecentEvent)
                                               .map(DomainEvent::getGlobalIndex)
                                               .collect(Collectors.toList());
        long maxNonRecentEvent = result.stream().filter(s -> !isRecentEvent(s))
                                       .map(DomainEvent::getGlobalIndex)
                                       .max(Comparator.naturalOrder())
                                       .orElse(lastProcessedToken);
        for (int i = 1; i <= recentGlobalIndexes.size(); i++) {
            if (!recentGlobalIndexes.contains(maxNonRecentEvent + i)) {
                log.error("Missing event at: {}. Found global indexes in batch: {}",
                          (maxNonRecentEvent + i),
                          recentGlobalIndexes);
                log.error(
                        "This indicates that there is a gap in the database which occurred recently. Since we cannot guarantee data ordering, we are stopping the migration.");
                return true;
            }
        }
        return false;
    }

    private boolean isAnIgnoredEventType(DomainEvent entry) {
        return properties.getIgnoredEvents().contains(entry.getPayloadType());
    }

    private Event buildEvent(final DomainEvent entry) throws Exception {
        if (isAnIgnoredEventType(entry)) {
            return null;
        }
        Event.Builder eventBuilder = Event.newBuilder()
                                          .setPayload(eventSerializer.toPayload(entry))
                                          .setMessageIdentifier(entry.getEventIdentifier());

        if (entry.getType() != null) {
            String aggregateIdentifier = entry.getAggregateIdentifier();
            long nextSequenceNumber = properties.shouldRequestSequenceNumbers() ? entry.getSequenceNumber()
                    : eventStoreStrategy.getNextSequenceNumber(aggregateIdentifier, entry.getSequenceNumber());
            eventBuilder.setAggregateType(entry.getType())
                        .setAggregateSequenceNumber(nextSequenceNumber)
                        .setAggregateIdentifier(aggregateIdentifier);
        }

        eventBuilder.setTimestamp(entry.getTimeStampAsLong());
        eventSerializer.convertMetadata(entry.getMetaData(), eventBuilder);
        return eventBuilder.build();
    }

    private void storeEvents(List<Event> events) throws Exception {
        if (events.isEmpty()) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Storing {} events", events.size());
        }

        try {
            eventStoreStrategy.storeEvents(events);
        } catch (Exception exception) {
            List<String> structure = events.stream()
                                           .map(e -> String.format("%s___%d",
                                                                   e.getAggregateIdentifier(),
                                                                   e.getAggregateSequenceNumber()))
                                           .collect(Collectors.toList());
            log.error("Exception while storing. The event list has the following structure: {}", structure);
            // Clear any cache on the sequence numbers, it's unreliable now.
            eventStoreStrategy.rollback();
            throw exception;
        }
    }

    private boolean isRecentEvent(DomainEvent entry) {
        return entry.getTimeStampAsLong() > System.currentTimeMillis() - properties.getRecentMillis();
    }
}
