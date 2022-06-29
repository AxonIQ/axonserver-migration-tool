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
}
