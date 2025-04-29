package io.axoniq.axonserver.migration.source.axonserver;

import io.axoniq.axonserver.grpc.event.EventWithToken;
import io.axoniq.axonserver.migration.source.DomainEvent;
import org.axonframework.axonserver.connector.util.GrpcMetaDataConverter;
import org.axonframework.messaging.MetaData;
import org.axonframework.serialization.Serializer;

public class AxonServerBackedDomainEvent implements DomainEvent {

    private final EventWithToken eventWithToken;
    private final Serializer serializer;
    private final GrpcMetaDataConverter grpcMetaDataConverter;

    public AxonServerBackedDomainEvent(EventWithToken eventWithToken, Serializer serializer) {
        this.eventWithToken = eventWithToken;
        this.serializer = serializer;
        this.grpcMetaDataConverter = new GrpcMetaDataConverter(serializer);
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
        MetaData javaRepresentation = grpcMetaDataConverter.convert(eventWithToken.getEvent().getMetaDataMap());
        return serializer.serialize(javaRepresentation, byte[].class).getData();
    }
}
