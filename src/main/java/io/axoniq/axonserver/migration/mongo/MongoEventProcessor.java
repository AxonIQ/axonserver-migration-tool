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

import static com.mongodb.client.model.Filters.*;
import static org.axonframework.common.DateTimeUtils.formatInstant;

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

        if (eventsCursor == null && lastToken == -1) {
            MongoCollection<Document> eventCollection = template.getCollection("domainevents");
            eventsCursor = eventCollection.find();
            eventsCursor = eventsCursor.sort(new BasicDBObject("timestamp", 1)
                    .append("sequenceNumber", 1));
            eventsCursor = eventsCursor.batchSize(batchSize);
             eventsIterator = eventsCursor.iterator();
        } else if (eventsCursor == null){
            MongoCollection<Document> eventCollection = template.getCollection("domainevents");
            eventsCursor = eventCollection.find(and(gte("timestamp",
                    formatInstant(Instant.ofEpochMilli(lastToken)))));
            eventsCursor = eventsCursor.batchSize(batchSize);
            eventsIterator = eventsCursor.iterator();
        }

       return getResults(eventsIterator,batchSize);

    }

    @Override
    public List<? extends SnapshotEvent> findSnapshots(String lastProcessedTimestamp, int batchSize) {

        if (snapshotCursor == null && Instant.parse(lastProcessedTimestamp).equals(Instant.ofEpochMilli(0))) {
            MongoCollection<Document> snapshotCollection = template.getCollection("snapshotevents");
            snapshotCursor = snapshotCollection.find();
            snapshotCursor = snapshotCursor.sort(new BasicDBObject("timestamp", 1)
                    .append("sequenceNumber", 1));
            snapshotCursor = snapshotCursor.batchSize(batchSize);
            snapshotIterator = snapshotCursor.iterator();
        } else if (snapshotCursor == null) {
            MongoCollection<Document> snapshotCollection = template.getCollection("snapshotevents");
            snapshotCursor = snapshotCollection.find(and(gte("timestamp",
                    lastProcessedTimestamp)));
            snapshotCursor = snapshotCursor.batchSize(batchSize);
            snapshotIterator = snapshotCursor.iterator();
        }

        return getResults(snapshotIterator,batchSize);
    }

    private List<MongoDomainEvent> getResults(MongoCursor<Document> iterator, int batchSize) {
        List<MongoDomainEvent> results = new ArrayList<>();

        for (MongoCursor<Document> itr = iterator; results.size() < batchSize && itr.hasNext(); ) {
            Document document = itr.next();

            //DocumentPerCommitStorageStrategy
            ArrayList<Document> events = (ArrayList<Document>) document.get("events");
            if (events != null) {
                events.forEach(doc->results.add(toEvent(doc)));
            } else {
                //DocumentPerEventStorageStrategy
                results.add(toEvent(document));
            }
        }
        return results;
    }

    private MongoDomainEvent toEvent(Document document) {
        return new MongoDomainEvent(
                document.getString("timestamp"),
                document.getString("serializedPayload"),
                document.getString("serializedMetaData"),
                document.getString("eventIdentifier"),
                document.getString("payloadType"),
                document.getString("payloadRevision"),
                document.getString("type"),
                document.getString("aggregateIdentifier"),
                document.getLong("sequenceNumber")
        );
    }


}
