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

package io.axoniq.axonserver.migration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

/**
 * Contains all the configuration properties of the migration tool.
 *
 * @author Mitchell Herrijgers
 */
@ConfigurationProperties("axoniq.migration")
@Configuration
@Getter
@Setter
public class MigrationBaseProperties {
    private MigrationSource source;
    private int batchSize = 100;
    private int recentMillis = 10000;

    private boolean migrateSnapshots;
    private boolean migrateEvents;
    private boolean reorderSequenceNumbers;
    private List<String> ignoredEvents = Collections.emptyList();

    private boolean continuous = false;
    private int continuousTimeout = 100;

    private MigrationDestination destination;

    public enum MigrationSource {
        RDBMS,
        MONGO,
        AXONSERVER,
    }

    public enum MigrationDestination {
        AXONSERVER
    }

    /**
     * Whether to reorder the sequence numbers because there are gaps. Gaps can be there due to manual database edits,
     * or because the migration tool has been told to skip certain event types.
     */
    public boolean shouldRequestSequenceNumbers() {
        return !ignoredEvents.isEmpty() || reorderSequenceNumbers;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public int getContinuousTimeout() {
        return continuousTimeout;
    }
}
