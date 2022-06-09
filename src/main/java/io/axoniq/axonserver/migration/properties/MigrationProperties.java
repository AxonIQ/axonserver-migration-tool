package io.axoniq.axonserver.migration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

/**
 * Contains all the configuration properties of the migration tool.
 *
 * @author Mitchell Herrijgers
 */
@ConfigurationProperties("axoniq.migration")
@Configuration
public class MigrationProperties {

    /**
     *
     */
    private int batchSize = 100;
    private int recentMillis = 10000;

    private boolean migrateSnapshots = true;
    private boolean migrateEvents = true;
    private List<String> ignoredEvents = Collections.emptyList();
    private String aggregateSuffix = "";
    private String skippedEventsFile = "skipped_events.db";

    private boolean continuous = false;
    private int continuousTimeout = 100;

    public int getBatchSize() {
        return batchSize;
    }

    public boolean isMigrateSnapshots() {
        return migrateSnapshots;
    }

    public boolean isMigrateEvents() {
        return migrateEvents;
    }

    public int getRecentMillis() {
        return recentMillis;
    }

    public List<String> getIgnoredEvents() {
        return ignoredEvents;
    }

    public String getAggregateSuffix() {
        return aggregateSuffix;
    }

    public String getSkippedEventsFile() {
        return skippedEventsFile;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public int getContinuousTimeout() {
        return continuousTimeout;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setMigrateSnapshots(boolean migrateSnapshots) {
        this.migrateSnapshots = migrateSnapshots;
    }

    public void setMigrateEvents(boolean migrateEvents) {
        this.migrateEvents = migrateEvents;
    }

    public void setIgnoredEvents(List<String> ignoredEvents) {
        this.ignoredEvents = ignoredEvents;
    }

    public void setContinuous(boolean continuous) {
        this.continuous = continuous;
    }

    public void setContinuousTimeout(int continuousTimeout) {
        this.continuousTimeout = continuousTimeout;
    }

    public void setRecentMillis(int recentMillis) {
        this.recentMillis = recentMillis;
    }

    public void setAggregateSuffix(String aggregateSuffix) {
        this.aggregateSuffix = aggregateSuffix;
    }

    public void setSkippedEventsFile(String skippedEventsFile) {
        this.skippedEventsFile = skippedEventsFile;
    }
}
