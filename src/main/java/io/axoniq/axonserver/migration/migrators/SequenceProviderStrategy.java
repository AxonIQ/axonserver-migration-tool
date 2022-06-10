package io.axoniq.axonserver.migration.migrators;

public interface SequenceProviderStrategy {
    Long getNextSequenceForAggregate(String aggregateIdentifier, long currentNumber);
    void reportSkip(String aggregateIdentifier);

    void commit();
    void rollback();
}
