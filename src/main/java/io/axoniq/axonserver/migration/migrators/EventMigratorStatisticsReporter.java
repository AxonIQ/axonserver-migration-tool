package io.axoniq.axonserver.migration.migrators;

import io.axoniq.axonserver.migration.source.EventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Reports statistics about the progress of the current run. It logs the current index, amount of events remaining and
 * the estimated time until completion every 5 seconds.
 * <p>
 * Will not run in case the {@link EventProducer} does not support the required {@link EventProducer#getMinIndex()} and
 * {@link EventProducer#getMaxIndex()} methods.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventMigratorStatisticsReporter {

    private final EventProducer eventProducer;
    private boolean enabled = true;
    private long startToken = 0;
    private long lastProcessedToken = 0;
    private long numberStored = 0;
    private long numberSkipped = 0;
    private Instant timeStarted;
    private long minGlobalIndex;
    private long maxGlobalIndex;

    public void initialize(long lastProcessedToken) {
        this.minGlobalIndex = eventProducer.getMinIndex();
        this.maxGlobalIndex = eventProducer.getMaxIndex();
        if (this.minGlobalIndex == -1 || this.maxGlobalIndex == -1) {
            this.enabled = false;
            return;
        }

        this.lastProcessedToken = lastProcessedToken;
        this.startToken = lastProcessedToken;
        this.timeStarted = Instant.now();

        log.info("Starting migration with global index {}. So progress before starting was {}% of total",
                 this.lastProcessedToken,
                 tokenPercentage());
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
        log.info(
                "Global index: {}. Migrated events: {}, Events to go: {}. Progress: {}, Skipped: {}, Stored: {}, Skipped percentage: {}",
                lastProcessedToken,
                lastProcessedToken - minGlobalIndex,
                maxGlobalIndex - lastProcessedToken,
                tokenPercentage(),
                numberSkipped,
                numberStored,
                percentage(numberSkipped, numberSkipped + numberStored));
        long secondsSinceStart = ChronoUnit.SECONDS.between(this.timeStarted, Instant.now());
        if(secondsSinceStart < 1) {
            return;
        }
        double rate = (this.lastProcessedToken - this.startToken) / secondsSinceStart;
        double secondsRemaining = (this.maxGlobalIndex - this.startToken) / rate;
        log.info(
                "Processing at average rate of {} events/sec (including skipped, since start). Projected hours remaining: {}",
                rate,
                secondsRemaining / 3600);
    }

    private Double tokenPercentage() {
        long correctedMax = maxGlobalIndex - minGlobalIndex;
        long correctedCurrent = lastProcessedToken - minGlobalIndex;
        return percentage(correctedCurrent, correctedMax);
    }

    private Double percentage(long progress, long total) {
        return (double) progress / (double) total;
    }
}
