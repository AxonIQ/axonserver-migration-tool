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

import io.axoniq.axonserver.migration.source.BaseEvent;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Marc Gathier
 */
@MappedSuperclass
public class BaseEventEntry implements BaseEvent {
    @Column(
            nullable = false,
            unique = true
    )
    private String eventIdentifier;
    @Basic(
            optional = false
    )
    private String timeStamp;
    @Basic(
            optional = false
    )
    private String payloadType;
    @Basic
    private String payloadRevision;
    @Basic(
            optional = false
    )
    @Lob
    @Column(
            length = 10000
    )
    private byte[] payload;
    @Basic
    @Lob
    @Column(
            length = 10000
    )
    private byte[] metaData;


    public String getEventIdentifier() {
        return eventIdentifier;
    }

    public void setEventIdentifier(String eventIdentifier) {
        this.eventIdentifier = eventIdentifier;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public long getTimeStampAsLong() {
        if( timeStamp == null) return 0;
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(timeStamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return zonedDateTime.toInstant().toEpochMilli();

    }


    public String getPayloadType() {
        return payloadType;
    }


    public String getPayloadRevision() {
        return payloadRevision;
    }


    public byte[] getPayload() {
        return payload;
    }


    public byte[] getMetaData() {
        return metaData;
    }

}
