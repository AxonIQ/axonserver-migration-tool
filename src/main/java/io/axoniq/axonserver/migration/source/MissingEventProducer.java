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

package io.axoniq.axonserver.migration.source;

import io.axoniq.axonserver.migration.destination.EventStoreStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import javax.annotation.PostConstruct;

/**
 * {@link EventStoreStrategy} that is used when the configuration is missing. Will throw an error on boot immediately.
 */
@Service
@ConditionalOnProperty(value = "axoniq.migration.source", havingValue = "false", matchIfMissing = true)
public class MissingEventProducer implements EventProducer {

    public static final String MIGRATION_SOURCE_WAS_NOT_DEFINED = "Migration source was not defined. Please supply the 'axoniq.migration.source' property. Consult the README.md if necessary.";

    @PostConstruct
    public void throwMissingConfigurationError() {
        throw new IllegalStateException(MIGRATION_SOURCE_WAS_NOT_DEFINED);
    }

    @Override
    public List<? extends DomainEvent> findEvents(long lastProcessedToken, int batchSize) {
        throw new IllegalStateException(MIGRATION_SOURCE_WAS_NOT_DEFINED);
    }

    @Override
    public List<? extends SnapshotEvent> findSnapshots(String lastProcessedTimestamp, int batchSize) {
        throw new IllegalStateException(MIGRATION_SOURCE_WAS_NOT_DEFINED);
    }
}
