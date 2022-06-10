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

        this.lastProcessedToken = lastProcessedToken;
        this.startToken = lastProcessedToken;
        this.timeStarted = Instant.now();

        logger.info("Starting migration with global index {}. So progress before starting was {}% of total", this.lastProcessedToken, tokenPercentage());
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
        logger.info(
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
        logger.info("Processing at average rate of {} events/sec (including skipped, since start). Projected hours remaining: {}", rate, secondsRemaining / 3600);
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
