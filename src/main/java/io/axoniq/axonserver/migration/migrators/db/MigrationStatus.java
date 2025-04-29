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

package io.axoniq.axonserver.migration.migrators.db;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Entity representing the current status of the migration. Saved using an H2 database to allow the migration tool to
 * have a consistent state between runs.
 *
 * @author Marc Gathier
 */
@Entity
@Cacheable(false)
@NoArgsConstructor
@Getter
@Setter
public class MigrationStatus {
    @Id
    private long id = 1;

    private long lastEventGlobalIndex = -1;
    private String lastSnapshotTimestamp = "1970-01-01T00:00:00Z";
    private String lastSnapshotEventId;
}
