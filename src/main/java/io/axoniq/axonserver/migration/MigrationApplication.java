package io.axoniq.axonserver.migration;

import io.axoniq.axonserver.config.AxonServerStandardConfiguration;
import io.axoniq.axonserver.config.MetricsConfiguration;
import io.axoniq.axonserver.enterprise.config.AxonServerEnterpriseConfiguration;
import io.axoniq.axonserver.localstorage.file.EmbeddedDBProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Marc Gathier
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        PrometheusMetricsExportAutoConfiguration.class,
        MetricsConfiguration.class,
        AxonServerStandardConfiguration.class,
        AxonServerEnterpriseConfiguration.class,
})
@EnableConfigurationProperties(EmbeddedDBProperties.class)
@EnableScheduling
public class MigrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(MigrationApplication.class, args);
    }
}
