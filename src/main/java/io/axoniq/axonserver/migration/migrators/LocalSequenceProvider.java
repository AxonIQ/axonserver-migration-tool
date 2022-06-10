package io.axoniq.axonserver.migration.migrators;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "axoniq.migration.method", havingValue = "LOCAL")
public class LocalSequenceProvider implements SequenceProviderStrategy {

    public void commit() {
        // Not needed
    }

    public void rollback() {
        // Not needed
    }

    public void reportSkip(String aggregateIdentifier) {
        // Not needed
    }

    public synchronized Long getNextSequenceForAggregate(String aggregateIdentifier, long currentNumber) {
        return currentNumber;
    }
}
