package io.axoniq.axonserver.migration.migrators;

import io.axoniq.axonserver.migration.properties.MigrationRemoteProperties;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import javax.annotation.PreDestroy;

@Service
@ConditionalOnProperty(value = "axoniq.migration.method", havingValue = "REMOTE", matchIfMissing = true)
public class RemoteSequenceProvider implements SequenceProviderStrategy {

    private final DB db;
    private final Map<String, Integer> skippedEventsMap;

    @Autowired
    public RemoteSequenceProvider(MigrationRemoteProperties migrationRemoteProperties) {
        db = DBMaker.fileDB(migrationRemoteProperties.getSkippedEventsFile())
                    .fileMmapEnable()
                    .transactionEnable()
                    .make();
        skippedEventsMap = db
                .hashMap("map", org.mapdb.Serializer.STRING, org.mapdb.Serializer.INTEGER)
                .createOrOpen();
    }

    @PreDestroy
    public void close() {
        db.close();
    }

    public void commit() {
        this.db.commit();
    }

    public void rollback() {
        this.db.rollback();
    }

    public void reportSkip(String aggregateIdentifier) {
        this.skippedEventsMap.put(aggregateIdentifier, skippedEventsMap.getOrDefault(aggregateIdentifier, 0) + 1);
    }

    public synchronized Long getNextSequenceForAggregate(String aggregateIdentifier, long currentNumber) {
        return currentNumber - skippedEventsMap.getOrDefault(aggregateIdentifier, 0);
    }
}
