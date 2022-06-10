package io.axoniq.axonserver.migration.migrators;

import io.axoniq.axonserver.migration.EventProducer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Profile("!test")
public class EventMigratorStatisticsReporter {

    private final Logger logger = LoggerFactory.getLogger(EventMigratorStatisticsReporter.class);
    private final EventProducer eventProducer;
    private boolean enabled = true;
    private long lastProcessedToken = 0;
    private long numberStored = 0;
    private long numberSkipped = 0;
    private Instant timeStarted;

    public void initialize(long lastProcessedToken) {
        long eventsAfter = eventProducer.countEventsAfterGlobalIndex(lastProcessedToken);
        long eventsBefore = eventProducer.countEventsBeforeGlobalIndex(lastProcessedToken);
        if (eventsAfter == -1) {
            enabled = false;
            return;
        }
        this.timeStarted = Instant.now();

        logger.info(
                "Starting migration with global index {}. This means we have already migrated {} events, still having {} to go. ({}%)",
                lastProcessedToken,
                eventsBefore,
                eventsAfter,
                (eventsAfter / (eventsBefore + eventsAfter)) * 100);
    }

    public void reportBatchSaved(long lastProcessedToken, int resultSize, int storedSize) {
        if (!enabled) {
            return;
        }
        this.numberStored += storedSize;
        this.numberSkipped += (resultSize - storedSize);
        this.lastProcessedToken = lastProcessedToken;
    }

    @Scheduled(fixedRate = 5000, initialDelay = 1000)
    public void report() {
        if (!enabled) {
            return;
        }
        long eventsAfter = eventProducer.countEventsAfterGlobalIndex(lastProcessedToken);
        long eventsBefore = eventProducer.countEventsBeforeGlobalIndex(lastProcessedToken);
        logger.info(
                "Global index: {}. Migrated events: {}, Events to go: {}. Progress: {}, Skipped: {}, Stored: {}, Skipped percentage: {}",
                lastProcessedToken,
                eventsBefore,
                eventsAfter,
                percentage(eventsBefore, eventsBefore + eventsAfter),
                numberSkipped,
                numberStored,
                percentage(numberSkipped, numberSkipped + numberStored));
        long secondsSinceStart = ChronoUnit.SECONDS.between(this.timeStarted, Instant.now());
        if(secondsSinceStart < 1) {
            return;
        }
        double rate = (this.numberStored + this.numberSkipped) / secondsSinceStart;
        double secondsRemaining = eventsAfter / rate;
        logger.info("Processing at average rate of {} events/sec (including skipped, since start). Projected hours remaining: {}", rate, secondsRemaining / 3600);
    }

    private Double percentage(long progress, long total) {
        return (double) progress / ((double) progress + (double) total);
    }
}
