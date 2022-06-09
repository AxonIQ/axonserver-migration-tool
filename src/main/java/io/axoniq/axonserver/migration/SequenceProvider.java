package io.axoniq.axonserver.migration;

import io.axoniq.axonserver.migration.properties.MigrationProperties;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import javax.annotation.PreDestroy;

@Service
public class SequenceProvider {

    private final DB db;
    private final Map<String, Integer> skippedEventsMap;


    @Autowired
    public SequenceProvider(MigrationProperties migrationProperties) {
        db = DBMaker.fileDB(migrationProperties.getSkippedEventsFile())
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
