package io.axoniq.axonserver.migration.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import io.axoniq.axonserver.migration.EventProducer;
import io.axoniq.axonserver.migration.SnapshotEvent;
import org.bson.Document;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Stefan Dragisic
 */
@Component
@Profile({"migrate-from-mongo"})
public class MongoEventProcessor implements EventProducer {

    private final MongoTemplate template;

    private FindIterable<Document> eventsCursor = null;
    MongoCursor<Document> eventsIterator = null;

    private FindIterable<Document> snapshotCursor = null;
    MongoCursor<Document> snapshotIterator = null;

    public MongoEventProcessor(MongoTemplate template) {
        this.template = template;
    }

    @Override
    public List<? extends MongoDomainEvent> findEvents(long lastToken, int batchSize) {

        if (eventsCursor == null) {
            MongoCollection<Document> eventCollection = template.getCollection("domainevents");
            eventsCursor = eventCollection.find();
            eventsCursor = eventsCursor.sort(new BasicDBObject("timestamp", 1)
                    .append("sequenceNumber", 1));
             eventsIterator = eventsCursor.iterator();
        }

        eventsCursor = eventsCursor.batchSize(batchSize);
        List<MongoDomainEvent> results = new ArrayList<>();

        for (MongoCursor<Document> itr = eventsIterator; results.size() < batchSize && itr.hasNext(); ) {
            Document document = itr.next();

            results.add(new MongoDomainEvent(
                    document.getString("timestamp"),
                    document.getString("serializedPayload"),
                    document.getString("serializedMetaData"),
                    document.getString("eventIdentifier"),
                    document.getString("payloadType"),
                    document.getString("payloadRevision"),
                    Instant.parse(document.getString("timestamp")).toEpochMilli(),
                    document.getString("type"),
                    document.getString("aggregateIdentifier"),
                    document.getLong("sequenceNumber")
                    )
            );

        }
        return results;

    }

    @Override
    public List<? extends SnapshotEvent> findSnapshots(String lastProcessedTimestamp, int batchSize) {

        if (snapshotCursor == null) {
            MongoCollection<Document> snapshotCollection = template.getCollection("snapshotevents");
            snapshotCursor = snapshotCollection.find();
            snapshotCursor = snapshotCursor.sort(new BasicDBObject("timestamp", 1)
                    .append("sequenceNumber", 1));
            snapshotIterator = snapshotCursor.iterator();
        }

        snapshotCursor = snapshotCursor.batchSize(batchSize);
        List<MongoSnapshotEvent> results = new ArrayList<>();

        for (MongoCursor<Document> itr = snapshotIterator; results.size() < batchSize && itr.hasNext(); ) {
            Document document = itr.next();

            results.add(new MongoSnapshotEvent(
                            document.getString("timestamp"),
                            document.getString("serializedPayload"),
                            document.getString("serializedMetaData"),
                            document.getString("eventIdentifier"),
                            document.getString("payloadType"),
                            document.getString("payloadRevision"),
                            document.getString("type"),
                            document.getString("aggregateIdentifier"),
                            document.getLong("sequenceNumber")
                    )
            );
        }
        return results;
    }


}
