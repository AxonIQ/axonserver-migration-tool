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

package io.axoniq.axonserver.migration.destination.remote;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.axoniq.axonserver.connector.event.EventStream;
import io.axoniq.axonserver.grpc.event.Event;
import io.axoniq.axonserver.migration.destination.EventStoreStrategy;
import lombok.RequiredArgsConstructor;
import org.axonframework.axonserver.connector.AxonServerConnectionManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
@ConditionalOnProperty(value = "axoniq.migration.destination", havingValue = "AXONSERVER", matchIfMissing = true)
public class RemoteEventStoreStrategy implements EventStoreStrategy {

    private final AxonServerConnectionManager axonServerConnectionManager;
    private final Cache<String, Long> sequenceCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.of(1, ChronoUnit.MINUTES))
            .maximumSize(10000)
            .build();

    @Override
    public void storeEvents(List<Event> events) throws Exception {
        axonServerConnectionManager.getConnection()
                                   .eventChannel()
                                   .appendEvents(events.toArray(new Event[0]))
                                   .get(30, TimeUnit.SECONDS);
    }

    @Override
    public void appendSnapshot(Event snapshot) throws Exception {
        axonServerConnectionManager.getConnection().eventChannel().appendSnapshot(snapshot).get(30, TimeUnit.SECONDS);
    }

    @Override
    public String getLastEventId() throws Exception {
        Long lastToken = axonServerConnectionManager.getConnection().eventChannel().getLastToken().get();
        if (lastToken == null || lastToken == -1) {
            return null;
        }
        try (EventStream stream = axonServerConnectionManager.getConnection().eventChannel().openStream(lastToken - 1, 1)) {
            return stream.next().getEvent().getMessageIdentifier();
        }
    }

    @Override
    public Long getNextSequenceNumber(String aggregate, Long current) throws Exception {
        Long currentValue = sequenceCache.get(aggregate, agg -> {
            try {
                return axonServerConnectionManager.getConnection().eventChannel().findHighestSequence(agg).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        Long newValue = currentValue == null ? 0 : currentValue + 1;
        sequenceCache.put(aggregate, newValue);
        return newValue;
    }

    @Override
    public void rollback() {
        this.sequenceCache.cleanUp();
    }
}
