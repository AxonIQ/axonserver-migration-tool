package io.axoniq.axonserver.migration;

import io.axoniq.axonserver.migration.properties.MigrationProperties;
import org.axonframework.axonserver.connector.AxonServerConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class SequenceProvider {

    private final Logger logger = LoggerFactory.getLogger(SequenceProvider.class);

    private final AxonServerConnectionManager connectionManager;
    private final MigrationProperties migrationProperties;
    private final Map<String, Long> sequenceMap = new HashMap<>();
    private int clearCount = 0;

    @Autowired
    public SequenceProvider(AxonServerConnectionManager axonDBClient, MigrationProperties migrationProperties) {
        this.connectionManager = axonDBClient;
        this.migrationProperties = migrationProperties;
    }

    /**
     * Clears the map cache, so for every batch the identifiers will be fetched from Axon Server and will be consistent.
     * Should be cleared after every batch, and never during a batch.
     *
     * Can be tuned using the {@code axoniq.migration.cacheLongevity} property, defaulting to a clear every 30 resets.
     * This means the cache is cleared every 30 batches, to prevent it from growing too large.
     */
    public void clearCache(boolean force) {
        clearCount += 1;
        if(force || clearCount % migrationProperties.getCacheLongevity() == 0) {
            logger.info("Clearing {} entries from the sequence cache", sequenceMap.size());
            sequenceMap.clear();
        }
    }

    public synchronized Long getNextSequenceForAggregate(String aggregateIdentifier) {
        Long currentSequence = sequenceMap.computeIfAbsent(aggregateIdentifier,
                                                           s -> getCurrentSequence(aggregateIdentifier));
        if (currentSequence == null) {
            currentSequence = -1L;
        }

        Long newSequence = currentSequence + 1;
        sequenceMap.put(aggregateIdentifier, newSequence);
        logger.debug("Using sequence {} for aggregate {}", currentSequence, aggregateIdentifier);
        return newSequence;
    }

    private synchronized Long getCurrentSequence(String s) {
        try {
            Long sequence = connectionManager.getConnection().eventChannel().findHighestSequence(s).get();
            logger.debug("Fetched sequence number {} for aggregate {} from Axon Server", sequence, s);
            return sequence;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
