package io.axoniq.axonserver.migration.source.jpa;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 * @author Marc Gathier
 */
@Configuration
@ConditionalOnProperty(value = "axoniq.migration.source", havingValue = "RDBMS")
@EnableJpaRepositories(basePackages = "io.axoniq.axonserver.migration.jpa",
        entityManagerFactoryRef = "eventStoreEntityManagerFactory")
public class EventStoreDBConfiguration {

    @Bean
    @Primary
    @Qualifier("eventStoreEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean eventStoreEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Value("${axoniq.migration.disable-naming-strategy:false}") boolean disableNamingStrategy) {
        Map<String, Object> properties = new HashMap<>();
        if (!disableNamingStrategy) {
            properties.put("hibernate.physical_naming_strategy", SpringPhysicalNamingStrategy.class.getName());
            properties.put("hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName());
        }

        return builder
                .dataSource(eventStoreDataSource())
                .packages("io.axoniq.axonserver.migration.jpa")
                .persistenceUnit("eventstore")
                .properties(properties)
                .build();
    }

    @Bean
    @Primary
    @ConfigurationProperties("axoniq.datasource.eventstore")
    public DataSourceProperties eventStoreDataSourceProperties() {
        return new DataSourceProperties();
    }


    @Bean(name = "eventStoreTransactionManager")
    public PlatformTransactionManager barTransactionManager(
            @Qualifier("eventStoreEntityManagerFactory") EntityManagerFactory barEntityManagerFactory) {
        return new JpaTransactionManager(barEntityManagerFactory);
    }

    @Bean
    @Primary
    public DataSource eventStoreDataSource() {
        return eventStoreDataSourceProperties().initializeDataSourceBuilder().build();
    }

}
