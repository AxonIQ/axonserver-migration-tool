package io.axoniq.axonserver.migration.migrators;

import com.google.protobuf.ByteString;
import io.axoniq.axonserver.grpc.MetaDataValue;
import io.axoniq.axonserver.grpc.SerializedObject;
import io.axoniq.axonserver.grpc.event.Event;
import io.axoniq.axonserver.migration.BaseEvent;
import org.axonframework.axonserver.connector.util.GrpcMetaDataConverter;
import org.axonframework.messaging.MetaData;
import org.axonframework.serialization.SerializedMetaData;
import org.axonframework.serialization.Serializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Migrates a part of the old non-AxonServer store to the new Axon Server instance.
 * Contains several utility functions that will probably be used by all implementations.
 */
public abstract class AbstractMigrator {
    private final Serializer serializer;
    private final GrpcMetaDataConverter grpcMetaDataConverter;

    protected AbstractMigrator(Serializer serializer) {
        this.serializer = serializer;
        this.grpcMetaDataConverter = new GrpcMetaDataConverter(this.serializer);
    }

    public abstract void migrate() throws Exception;


    protected void convertMetadata(byte[] metadataBytes, Event.Builder eventBuilder) {
        if (metadataBytes != null) {
            MetaData metaData = serializer.deserialize(new SerializedMetaData<>(metadataBytes, byte[].class));
            Map<String, MetaDataValue> metaDataValues = new HashMap<>();
            metaData.forEach((k, v) -> metaDataValues.put(k, grpcMetaDataConverter.convertToMetaDataValue(v)));
            eventBuilder.putAllMetaData(metaDataValues);
        }
    }

    protected SerializedObject toPayload(BaseEvent entry) {
        SerializedObject.Builder builder = SerializedObject.newBuilder()
                                                           .setData(ByteString.copyFrom(entry.getPayload()))
                                                           .setType(entry.getPayloadType());

        if( entry.getPayloadRevision() != null)
            builder.setRevision(entry.getPayloadRevision());
        return builder.build();
    }
}
