package io.axoniq.axonserver.migration.migrators;

import io.axoniq.axonserver.config.FileSystemMonitor;
import io.axoniq.axonserver.localstorage.EventStorageEngine;
import io.axoniq.axonserver.localstorage.EventType;
import io.axoniq.axonserver.localstorage.EventTypeContext;
import io.axoniq.axonserver.localstorage.file.EmbeddedDBProperties;
import io.axoniq.axonserver.localstorage.file.IndexManager;
import io.axoniq.axonserver.localstorage.file.InputStreamEventStore;
import io.axoniq.axonserver.localstorage.file.PrimaryEventStore;
import io.axoniq.axonserver.localstorage.file.StandardIndexManager;
import io.axoniq.axonserver.localstorage.file.StorageProperties;
import io.axoniq.axonserver.localstorage.transformation.DefaultEventTransformerFactory;
import io.axoniq.axonserver.metric.DefaultMetricCollector;
import io.axoniq.axonserver.metric.MeterFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.system.DiskSpaceHealthIndicatorProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.io.IOException;

@Configuration
@ConditionalOnProperty(value = "axoniq.migration.method", havingValue = "LOCAL")
@Profile("!test")
public class LocalEventStoreConfiguration {
    @Bean
    public MeterFactory meterFactory() {
        return new MeterFactory(new SimpleMeterRegistry(), new DefaultMetricCollector());
    }

    @Bean
    @ConditionalOnProperty(value = "axoniq.axonserver.event.index-format", havingValue = "BLOOM")
    public IndexManager bloomIndexManager(
            @Value("${axon.axonserver.context:default}") String context,
            EmbeddedDBProperties properties,
            MeterFactory meterFactory
    ) {
        return new StandardIndexManager(context,
                                        properties.getEvent(),
                                        EventType.EVENT,
                                        meterFactory);
    }

    @Bean
    public EventStorageEngine eventStorageEngine(
            @Value("${axon.axonserver.context:default}") String context,
            EmbeddedDBProperties embeddedDBProperties,
            MeterRegistry meterRegistry,
            IndexManager indexManager,
            MeterFactory meterFactory
    ) throws IOException {
        StorageProperties storageProperties = embeddedDBProperties.getEvent();
        FileUtils.createParentDirectories(new File(storageProperties.getStorage(context) + "/dummyfile.txt"));

        DefaultEventTransformerFactory eventTransformerFactory = new DefaultEventTransformerFactory();
        InputStreamEventStore second = new InputStreamEventStore(new EventTypeContext(context, EventType.EVENT),
                                                                 indexManager,
                                                                 eventTransformerFactory,
                                                                 storageProperties,
                                                                 meterFactory);
        PrimaryEventStore primaryEventStore = new PrimaryEventStore(new EventTypeContext(context, EventType.EVENT),
                                                                    indexManager,
                                                                    eventTransformerFactory,
                                                                    storageProperties,
                                                                    second,
                                                                    meterFactory,
                                                                    new FileSystemMonitor(new DiskSpaceHealthIndicatorProperties(),
                                                                                          meterRegistry));
        primaryEventStore.init(true);
        return primaryEventStore;
    }
}
