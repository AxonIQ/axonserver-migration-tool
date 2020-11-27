package io.axoniq.axonserver.migration.mongo;

import io.axoniq.axonserver.migration.DomainEvent;
import org.springframework.data.mongodb.core.mapping.Document;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * @author Stefan Dragisic
 */
public class MongoDomainEvent implements DomainEvent {
    public MongoDomainEvent(String timestamp,
                            String serializedPayload,
                            String serializedMetaData,
                            String eventIdentifier,
                            String payloadType,
                            String payloadRevision,
                            long globalIndex,
                            String type,
                            String aggregateIdentifier,
                            long sequenceNumber) {
        this.timestamp = timestamp;
        this.serializedPayload = serializedPayload;
        this.serializedMetaData = serializedMetaData;
        this.eventIdentifier = eventIdentifier;
        this.payloadType = payloadType;
        this.payloadRevision = payloadRevision;
        this.globalIndex = globalIndex;
        this.type = type;
        this.aggregateIdentifier = aggregateIdentifier;
        this.sequenceNumber = sequenceNumber;
    }

    private String timestamp;
    private String serializedPayload;
    private String serializedMetaData;
    private String eventIdentifier;
    private String payloadType;
    private String payloadRevision;
    private long globalIndex;
    private String type;
    private String aggregateIdentifier;
    private long sequenceNumber;

    @Override
    public long getGlobalIndex() {
        return Instant.parse(timestamp).toEpochMilli();
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getAggregateIdentifier() {
        return aggregateIdentifier;
    }

    @Override
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public String getEventIdentifier() {
        return eventIdentifier;
    }

    @Override
    public long getTimeStampAsLong() {
        return Instant.parse(timestamp).toEpochMilli();
    }

    @Override
    public String getPayloadType() {
        return payloadType;
    }

    @Override
    public String getPayloadRevision() {
        return payloadRevision;
    }

    @Override
    public byte[] getPayload() {
        return serializedPayload.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] getMetaData() {
        return serializedMetaData.getBytes(StandardCharsets.UTF_8);
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setSerializedPayload(String serializedPayload) {
        this.serializedPayload = serializedPayload;
    }

    public void setSerializedMetaData(String serializedMetaData) {
        this.serializedMetaData = serializedMetaData;
    }

    public void setEventIdentifier(String eventIdentifier) {
        this.eventIdentifier = eventIdentifier;
    }

    public void setPayloadType(String payloadType) {
        this.payloadType = payloadType;
    }

    public void setPayloadRevision(String payloadRevision) {
        this.payloadRevision = payloadRevision;
    }

    public void setGlobalIndex(long timestamp) {
        this.timestamp = Instant.ofEpochMilli(timestamp).toString();
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAggregateIdentifier(String aggregateIdentifier) {
        this.aggregateIdentifier = aggregateIdentifier;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
}
