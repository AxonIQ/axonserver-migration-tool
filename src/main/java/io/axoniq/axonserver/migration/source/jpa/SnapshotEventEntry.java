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

package io.axoniq.axonserver.migration.source.jpa;

import io.axoniq.axonserver.migration.source.SnapshotEvent;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQuery;

/**
 * Entity representing a snapshot in the RDBMS store.
 *
 * @author Marc Gathier
 */
@Entity
@NamedQuery(name = "SnapshotEventEntry.findByTimestamp", query = "select e from SnapshotEventEntry e where e.timeStamp >= :lastTimeStamp order by e.timeStamp asc, e.eventIdentifier")
@IdClass(SnapshotEventEntry.PK.class)
public class SnapshotEventEntry extends BaseEventEntry implements SnapshotEvent {

    @Id
    private String aggregateIdentifier;
    @Id
    private long sequenceNumber;
    @Id
    private String type;

    @Override
    public String getAggregateIdentifier() {
        return aggregateIdentifier;
    }

    @Override
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public String getType() {
        return type;
    }


    public static class PK implements Serializable {
        private static final long serialVersionUID = 9182347799552520594L;

        private String aggregateIdentifier;
        private long sequenceNumber;
        private String type;



        /**
         * Constructor for JPA. Not to be used directly
         */
        public PK() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SnapshotEventEntry.PK pk = (SnapshotEventEntry.PK) o;
            return sequenceNumber == pk.sequenceNumber && Objects.equals(aggregateIdentifier, pk.aggregateIdentifier) &&
                    Objects.equals(type, pk.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(aggregateIdentifier, type, sequenceNumber);
        }

        @Override
        public String toString() {
            return "PK{type='" + type + '\'' + ", aggregateIdentifier='" + aggregateIdentifier + '\'' +
                    ", sequenceNumber=" + sequenceNumber + '}';
        }
    }
}
