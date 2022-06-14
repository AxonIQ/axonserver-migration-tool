package io.axoniq.axonserver.migration.migrators;

import io.axoniq.axonserver.grpc.event.Event;
import io.axoniq.axonserver.migration.DomainEvent;
import io.axoniq.axonserver.migration.EventProducer;
import io.axoniq.axonserver.migration.db.MigrationStatus;
import io.axoniq.axonserver.migration.db.MigrationStatusRepository;
import io.axoniq.axonserver.migration.properties.MigrationBaseProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Profile("!test")
@ConditionalOnProperty(value = "axoniq.migration.migrateEvents", havingValue = "true", matchIfMissing = true)
public class EventMigrator implements Migrator {

    private final Logger logger = LoggerFactory.getLogger(EventMigrator.class);

    private final MigrationBaseProperties migrationProperties;
    private final EventProducer eventProducer;
    private final EventSerializer eventSerializer;
    private final MigrationStatusRepository migrationStatusRepository;
    private final SequenceProviderStrategy sequenceProvider;
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
                                                                          migrationProperties.getBatchSize());
            if (result.isEmpty()) {
                logger.info("No more events found");
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
                    logger.info(
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
                    .map(this::buildEvent)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            storeEvents(events);

            migrationStatus.setLastEventGlobalIndex(lastProcessedToken);
            migrationStatusRepository.save(migrationStatus);
            reporter.reportBatchSaved(lastProcessedToken, result.size(), events.size());
            sequenceProvider.commit();
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
                logger.error("Missing event at: {}. Found global indexes in batch: {}",
                             (maxNonRecentEvent + i),
                             recentGlobalIndexes);
                logger.error(
                        "This indicates that there is a gap in the database which occurred recently. Since we cannot guarantee data ordering, we are stopping the migration.");
                return true;
            }
        }
        return false;
    }

    private boolean isAnIgnoredEventType(DomainEvent entry) {
        return migrationProperties.getIgnoredEvents().contains(entry.getPayloadType());
    }

    private Event buildEvent(final DomainEvent entry) {
        if (isAnIgnoredEventType(entry)) {
            if (entry.getType() != null) {
                sequenceProvider.reportSkip(entry.getAggregateIdentifier());
            }
            return null;
        }
        Event.Builder eventBuilder = Event.newBuilder()
                                          .setPayload(eventSerializer.toPayload(entry))
                                          .setMessageIdentifier(entry.getEventIdentifier());

        if (entry.getType() != null) {
            String aggregateIdentifier = entry.getAggregateIdentifier() + migrationProperties.getAggregateSuffix();
            eventBuilder.setAggregateType(entry.getType())
                        .setAggregateSequenceNumber(sequenceProvider.getNextSequenceForAggregate(aggregateIdentifier, entry.getSequenceNumber()))
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
        if (logger.isDebugEnabled()) {
            logger.debug("Storing {} events", events.size());
        }

        try {
            eventStoreStrategy.storeEvents(events);
        } catch (Exception exception) {
            List<String> structure = events.stream()
                                           .map(e -> String.format("%s___%d",
                                                                   e.getAggregateIdentifier(),
                                                                   e.getAggregateSequenceNumber()))
                                           .collect(Collectors.toList());
            logger.error("Exception while storing. The event list has the following structure: {}", structure);
            // Clear any cache on the sequence numbers, it's unreliable now.
            sequenceProvider.rollback();
            throw exception;
        }
    }

    private boolean isRecentEvent(DomainEvent entry) {
        return entry.getTimeStampAsLong() > System.currentTimeMillis() - migrationProperties.getRecentMillis();
    }
}
