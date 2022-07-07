package io.axoniq.axonserver.migration;

import lombok.Getter;
import lombok.Setter;
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
@Getter
@Setter
public class MigrationBaseProperties {
    private MigrationSource source;
    private int batchSize = 100;
    private int recentMillis = 10000;

    private boolean migrateSnapshots;
    private boolean migrateEvents;
    private boolean reorderSequenceNumbers;
    private List<String> ignoredEvents = Collections.emptyList();

    private boolean continuous = false;
    private int continuousTimeout = 100;

    private MigrationDestination destination;

    public enum MigrationSource {
        RDBMS,
        MONGO
    }

    public enum MigrationDestination {
        REMOTE,
        LOCAL
    }

    /**
     * Whether to reorder the sequence numbers because there are gaps. Gaps can be there due to manual database edits,
     * or because the migration tool has been told to skip certain event types.
     */
    public boolean shouldRequestSequenceNumbers() {
        return !ignoredEvents.isEmpty() || reorderSequenceNumbers;
    }
}
