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

package io.axoniq.axonserver.migration.source.axonserver;

import io.axoniq.axonserver.connector.event.EventStream;
import io.axoniq.axonserver.grpc.event.EventWithToken;
import io.axoniq.axonserver.migration.source.EventProducer;
import io.axoniq.axonserver.migration.source.SnapshotEvent;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.axonserver.connector.AxonServerConnectionManager;
import org.axonframework.serialization.Serializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Produces events when defining the migration source as {@code AXONSERVER}.
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "axoniq.migration.source", havingValue = "AXONSERVER")
public class AxonServerEventProducer implements EventProducer {

    private final AxonServerConnectionManager connectionManager;
    private final Serializer serializer;
    private EventStream eventStream;
    private Long lastRequestedEventToken = -1L;

    public AxonServerEventProducer(
            @Qualifier("originAxonServerConnectionManager") AxonServerConnectionManager connectionManager,
            Serializer serializer
    ) {
        this.connectionManager = connectionManager;
        this.serializer = serializer;
    }

    @Override
    public List<? extends AxonServerBackedDomainEvent> findEvents(long lastToken, int batchSize) {
        if (lastToken < lastRequestedEventToken) {
            // We need to reinitialize the stream
            if (eventStream != null && !eventStream.isClosed()) {
                eventStream.close();
            }
            eventStream = connectionManager.getConnection().eventChannel().openStream(lastToken, batchSize);
        }
        if (eventStream == null || eventStream.isClosed()) {
            eventStream = connectionManager.getConnection().eventChannel().openStream(lastToken, batchSize);
        }
        lastRequestedEventToken = lastToken;

        List<AxonServerBackedDomainEvent> batch = new LinkedList<>();
        while (batch.size() < batchSize) {
            try {
                EventWithToken eventWithToken = eventStream.nextIfAvailable(30, TimeUnit.SECONDS);
                if (eventWithToken == null) {
                    // No event available. Stop polling
                    break;
                }
                batch.add(new AxonServerBackedDomainEvent(eventWithToken, serializer));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return batch;
    }

    @Override
    public List<? extends SnapshotEvent> findSnapshots(String lastProcessedTimestamp, int batchSize) {
        // Unfortunately, not possible with Axon Server.
        return Collections.emptyList();
    }
}
