/*
 * Copyright (c) 2010-2023. AxonIQ
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.axoniq.axonserver.migration.serialisation;

import com.google.protobuf.ByteString;
import io.axoniq.axonserver.grpc.MetaDataValue;
import io.axoniq.axonserver.grpc.SerializedObject;
import io.axoniq.axonserver.grpc.event.Event;
import io.axoniq.axonserver.migration.source.BaseEvent;
import org.axonframework.axonserver.connector.util.GrpcMetaDataConverter;
import org.axonframework.messaging.MetaData;
import org.axonframework.serialization.SerializedMetaData;
import org.axonframework.serialization.Serializer;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Migrates a part of the old non-AxonServer store to the new Axon Server instance.
 * Contains several utility functions that will probably be used by all implementations.
 */
@Service
public class EventSerializer {
    private final Serializer serializer;
    private final GrpcMetaDataConverter grpcMetaDataConverter;

    public EventSerializer(Serializer serializer) {
        this.serializer = serializer;
        this.grpcMetaDataConverter = new GrpcMetaDataConverter(this.serializer);
    }

    public void convertMetadata(byte[] metadataBytes, Event.Builder eventBuilder) {
        if (metadataBytes != null) {
            MetaData metaData = serializer.deserialize(new SerializedMetaData<>(metadataBytes, byte[].class));
            Map<String, MetaDataValue> metaDataValues = new HashMap<>();
            metaData.forEach((k, v) -> metaDataValues.put(k, grpcMetaDataConverter.convertToMetaDataValue(v)));
            eventBuilder.putAllMetaData(metaDataValues);
        }
    }

    public SerializedObject toPayload(BaseEvent entry) {
        SerializedObject.Builder builder = SerializedObject.newBuilder()
                                                           .setData(ByteString.copyFrom(entry.getPayload()))
                                                           .setType(entry.getPayloadType());

        if( entry.getPayloadRevision() != null)
            builder.setRevision(entry.getPayloadRevision());
        return builder.build();
    }
}
