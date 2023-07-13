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

package io.axoniq.axonserver.migration.destination;

import io.axoniq.axonserver.grpc.event.Event;

import java.util.List;

/**
 * Strategy that defines how events are stored and sequence numbers are determined.
 * <p>
 * Supplying a bean of this type will automatically use it as a storage mechanism. It should be unique throughout the
 * application.
 */
public interface EventStoreStrategy {

    /**
     * Stores the given events.
     *
     * @param events The events to store.
     */
    void storeEvents(List<Event> events) throws Exception;

    /**
     * Stores the given snapshot.
     *
     * @param snapshot The snapshot to store.
     */
    void appendSnapshot(Event snapshot) throws Exception;

    /**
     * Retrieves the last event id stores in the mechanism. Used for determining whether the new batch already contains
     * the event that is last in the storage to prevent double events between exceptional runs.
     *
     * @return ID of the last event. Can be null
     */
    String getLastEventId() throws Exception;

    /**
     * Fetches the next sequence number to be used for the given aggregate. Depending on the storage and its validation
     * of the aggregate sequence number, it might need logic or fetching.
     * <p>
     * Defaults to returning the current sequence.
     *
     * @param aggregate The aggregateIdentifier to get the sequence number for
     * @param current   The sequence number currently defined by the event.
     * @return The correct sequence number
     */
    default Long getNextSequenceNumber(String aggregate, Long current) throws Exception {
        return current;
    }

    /**
     * If local state is kept about the sequence number, the strategy should implement this method to clear the state.
     * Otherwise we will keep getting out of sequence errors.
     */
    default void rollback() {

    }
}
