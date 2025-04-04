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

import lombok.RequiredArgsConstructor;
import org.axonframework.axonserver.connector.AxonServerConfiguration;
import org.axonframework.axonserver.connector.AxonServerConnectionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that defines the correct beans to using a {@code AXONSERVER} destination.
 *
 * @author Mitchell Herrijgers
 */
@Configuration
@ConditionalOnProperty(value = "axoniq.migration.destination", havingValue = "AXONSERVER", matchIfMissing = true)
@RequiredArgsConstructor
public class DestinationAxonServerEventStoreConfiguration {
    private final ApplicationContext applicationContext;

    @Bean
    public AxonServerConnectionManager axonServerConnectionManager(
            @Qualifier("destinationAxonServerConfiguration") AxonServerConfiguration axonServerConfiguration) {
        axonServerConfiguration.setComponentName(clientName(applicationContext.getId()));
        return AxonServerConnectionManager.builder()
                                          .axonServerConfiguration(axonServerConfiguration)
                                          .build();
    }

    private String clientName(String id) {
        if( id == null) return "AxonServerMigration";
        if (id.contains(":")) return id.substring(0, id.indexOf(':'));
        return id;
    }
}
