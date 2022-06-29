package io.axoniq.axonserver.migration.source.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import io.axoniq.axonserver.migration.source.EventProducer;
import io.axoniq.axonserver.migration.source.SnapshotEvent;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static org.axonframework.common.DateTimeUtils.formatInstant;

/**
 * Produces events when defining the migration source as {@code MONGO}. Queries the database using the MongoTemplate to
 * look up events and snapshots, that can then be migrated.
 *
 * @author Stefan Dragisic
 */
@Component
@ConditionalOnProperty(value = "axoniq.migration.source", havingValue = "MONGO")
public class MongoEventProducer implements EventProducer {

    private final MongoTemplate template;

    private FindIterable<Document> eventsCursor = null;
    MongoCursor<Document> eventsIterator = null;

    private FindIterable<Document> snapshotCursor = null;
    MongoCursor<Document> snapshotIterator = null;


    final Duration LOOK_BACK_DURATION;
    final Duration SNAPSHOTS_LOOK_BACK_DURATION;

    public MongoEventProducer(MongoTemplate template,
                              @Value("${axoniq.migration.eventsLookBackSeconds:10}") int eventsLookBackSeconds,
                              @Value("${axoniq.migration.snapshotsLookBackSeconds:15}") int snapshotsLookBackSeconds) {
        this.template = template;
        LOOK_BACK_DURATION = Duration.ofSeconds(eventsLookBackSeconds);
        SNAPSHOTS_LOOK_BACK_DURATION = Duration.ofSeconds(snapshotsLookBackSeconds);
    }

    @Override
    public List<? extends MongoDomainEvent> findEvents(long lastToken, int batchSize) {

        if (eventsCursor == null && lastToken == -1) {
            MongoCollection<Document> eventCollection = template.getCollection("domainevents");
            eventsCursor = eventCollection.find(and(lt("timestamp",
                    formatInstant(Instant.now().minus(LOOK_BACK_DURATION)))));
            eventsCursor = eventsCursor.sort(new BasicDBObject("timestamp", 1)
                    .append("sequenceNumber", 1));
            eventsCursor = eventsCursor.batchSize(batchSize);
             eventsIterator = eventsCursor.iterator();
        } else if (eventsCursor == null){
            MongoCollection<Document> eventCollection = template.getCollection("domainevents");
            eventsCursor = eventCollection.find(and(gte("timestamp",
                    formatInstant(Instant.ofEpochMilli(lastToken))),
                    and(lte("timestamp", Instant.now().minus(LOOK_BACK_DURATION).toString()))));
            eventsCursor = eventsCursor.batchSize(batchSize);
            eventsIterator = eventsCursor.iterator();
        }

       return getResults(eventsIterator,batchSize);

    }

    @Override
    public List<? extends SnapshotEvent> findSnapshots(String lastProcessedTimestamp, int batchSize) {

        if (snapshotCursor == null && Instant.parse(lastProcessedTimestamp).equals(Instant.ofEpochMilli(0))) {
            MongoCollection<Document> snapshotCollection = template.getCollection("snapshotevents");
            snapshotCursor = snapshotCollection.find(and(lt("timestamp",
                    Instant.now().minus(SNAPSHOTS_LOOK_BACK_DURATION).toString())));
            snapshotCursor = snapshotCursor.sort(new BasicDBObject("timestamp", 1)
                    .append("sequenceNumber", 1));
            snapshotCursor = snapshotCursor.batchSize(batchSize);
            snapshotIterator = snapshotCursor.iterator();
        } else if (snapshotCursor == null) {
            MongoCollection<Document> snapshotCollection = template.getCollection("snapshotevents");

            snapshotCursor = snapshotCollection.find(and(gt("timestamp",
                    lastProcessedTimestamp),and(lte("timestamp", Instant.now().minus(SNAPSHOTS_LOOK_BACK_DURATION).toString()))));

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
