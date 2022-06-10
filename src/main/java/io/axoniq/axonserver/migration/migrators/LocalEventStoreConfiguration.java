package io.axoniq.axonserver.migration.migrators;

import io.axoniq.axonserver.config.DefaultSystemInfoProvider;
import io.axoniq.axonserver.config.FileSystemMonitor;
import io.axoniq.axonserver.localstorage.EventStorageEngine;
import io.axoniq.axonserver.localstorage.EventType;
import io.axoniq.axonserver.localstorage.EventTypeContext;
import io.axoniq.axonserver.localstorage.file.EmbeddedDBProperties;
import io.axoniq.axonserver.localstorage.file.InputStreamEventStore;
import io.axoniq.axonserver.localstorage.file.PrimaryEventStore;
import io.axoniq.axonserver.localstorage.file.StandardIndexManager;
import io.axoniq.axonserver.localstorage.file.StorageProperties;
import io.axoniq.axonserver.localstorage.transformation.DefaultEventTransformerFactory;
import io.axoniq.axonserver.metric.DefaultMetricCollector;
import io.axoniq.axonserver.metric.MeterFactory;
import io.axoniq.axonserver.migration.properties.MigrationLocalProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.system.DiskSpaceHealthIndicatorProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.File;
import java.io.IOException;

@Configuration
@ConditionalOnProperty(value = "axoniq.migration.method", havingValue = "LOCAL")
public class LocalEventStoreConfiguration {

    @Bean
    public EventStorageEngine eventStorageEngine(
            @Value("${axon.axonserver.context:default}") String context,
            Environment environment,
            MeterRegistry meterRegistry,
            MigrationLocalProperties localProperties
    ) throws IOException {
        MeterFactory meterFactory = new MeterFactory(new SimpleMeterRegistry(), new DefaultMetricCollector());
        StorageProperties properties = new EmbeddedDBProperties(new DefaultSystemInfoProvider(environment)).getEvent();
        properties.setStorage(localProperties.getEventStorePath());
        StandardIndexManager indexManager = new StandardIndexManager(context,
                                                                     properties,
                                                                     EventType.EVENT,
                                                                     meterFactory);
        FileUtils.createParentDirectories(new File(properties.getStorage(context) + "/somefile.txt"));
        indexManager.init();
        DefaultEventTransformerFactory eventTransformerFactory = new DefaultEventTransformerFactory();
        InputStreamEventStore second = new InputStreamEventStore(new EventTypeContext(context, EventType.EVENT),
                                                                 indexManager,
                                                                 eventTransformerFactory,
                                                                 properties,
                                                                 meterFactory);
        PrimaryEventStore primaryEventStore = new PrimaryEventStore(new EventTypeContext(context, EventType.EVENT),
                                                                    indexManager,
                                                                    eventTransformerFactory,
                                                                    properties,
                                                                    second,
                                                                    meterFactory,
                                                                    new FileSystemMonitor(new DiskSpaceHealthIndicatorProperties(),
                                                                                          meterRegistry));
        primaryEventStore.init(true);
        return primaryEventStore;
    }
}
