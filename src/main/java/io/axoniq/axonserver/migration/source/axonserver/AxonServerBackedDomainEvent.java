package io.axoniq.axonserver.migration.source.axonserver;

import io.axoniq.axonserver.grpc.event.EventWithToken;
import io.axoniq.axonserver.migration.source.DomainEvent;

public class AxonServerBackedDomainEvent implements DomainEvent {

    private final EventWithToken eventWithToken;

    public AxonServerBackedDomainEvent(EventWithToken eventWithToken) {
        this.eventWithToken = eventWithToken;
    }

    @Override
    public long getGlobalIndex() {
        return eventWithToken.getToken();
    }

    @Override
    public String getType() {
        return eventWithToken.getEvent().getAggregateType();
    }

    @Override
    public String getAggregateIdentifier() {
        return eventWithToken.getEvent().getAggregateIdentifier();
    }

    @Override
    public long getSequenceNumber() {
        return eventWithToken.getEvent().getAggregateSequenceNumber();
    }

    @Override
    public String getEventIdentifier() {
        return eventWithToken.getEvent().getMessageIdentifier();
    }

    @Override
    public long getTimeStampAsLong() {
        return eventWithToken.getEvent().getTimestamp();
    }

    @Override
    public String getPayloadType() {
        return eventWithToken.getEvent().getPayload().getType();
    }

    @Override
    public String getPayloadRevision() {
        return eventWithToken.getEvent().getPayload().getRevision();
    }

    @Override
    public byte[] getPayload() {
        return eventWithToken.getEvent().getPayload().getData().toByteArray();
    }

    @Override
    public byte[] getMetaData() {
        return eventWithToken.getEvent().getPayload().getData().toByteArray();
    }
}
