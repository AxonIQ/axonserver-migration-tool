package io.axoniq.axonserver.migration.destination.local;

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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.system.DiskSpaceHealthIndicatorProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;

/**
 * Configuration class that defines the correct beans to using a {@code LOCAL} destination.
 *
 * @author Mitchell Herrijgers
 */
@Configuration
@ConditionalOnProperty(value = "axoniq.migration.destination", havingValue = "LOCAL")
public class LocalEventStoreConfiguration {
    @Bean
    public MeterFactory meterFactory() {
        return new MeterFactory(new SimpleMeterRegistry(), new DefaultMetricCollector());
    }

    @Bean
    @ConditionalOnProperty(value = "axoniq.axonserver.event.index-format", havingValue = "BLOOM", matchIfMissing = true)
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
    @Qualifier("events")
    public EventStorageEngine eventEventStorageEngine(
            @Value("${axoniq.axonserver.context:default}") String context,
            EmbeddedDBProperties embeddedDBProperties,
            MeterRegistry meterRegistry,
            IndexManager indexManager,
            MeterFactory meterFactory
    ) throws IOException {
        StorageProperties storageProperties = embeddedDBProperties.getEvent();
        return createStore(storageProperties, context, EventType.EVENT, indexManager, meterFactory, meterRegistry);
    }

    @Bean
    @Qualifier("snapshots")
    public EventStorageEngine snapshotEventStorageEngine(
            @Value("${axoniq.axonserver.context:default}") String context,
            EmbeddedDBProperties embeddedDBProperties,
            MeterRegistry meterRegistry,
            IndexManager indexManager,
            MeterFactory meterFactory
    ) throws IOException {
        StorageProperties storageProperties = embeddedDBProperties.getSnapshot();
        return createStore(storageProperties, context, EventType.SNAPSHOT, indexManager, meterFactory, meterRegistry);
    }

    private PrimaryEventStore createStore(StorageProperties storageProperties, String context, EventType eventType,
                                          IndexManager indexManager, MeterFactory meterFactory,
                                          MeterRegistry meterRegistry) throws IOException {
        // Ensure the directory is created
        FileUtils.createParentDirectories(new File(storageProperties.getStorage(context) + "/dummyfile.txt"));

        DefaultEventTransformerFactory eventTransformerFactory = new DefaultEventTransformerFactory();
        InputStreamEventStore second = new InputStreamEventStore(new EventTypeContext(context, eventType),
                                                                 indexManager,
                                                                 eventTransformerFactory,
                                                                 storageProperties,
                                                                 meterFactory);
        PrimaryEventStore primaryEventStore = new PrimaryEventStore(new EventTypeContext(context, eventType),
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
