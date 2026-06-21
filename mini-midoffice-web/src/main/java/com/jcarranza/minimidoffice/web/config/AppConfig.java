package com.jcarranza.minimidoffice.web.config;

import com.jcarranza.minimidoffice.integration.config.IntegrationConfig;
import com.jcarranza.minimidoffice.persistence.config.PersistenceConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Root Application Context.
 *
 * Loaded by ContextLoaderListener (web.xml). Contains all beans that are
 * NOT specific to Spring MVC: services, DAOs, Hibernate, Sabre.
 *
 * Root/Web separation is mandatory for @Transactional to work correctly:
 * if services were in the Web Context, the AOP proxy of HibernateTransactionManager
 * (declared in the Root Context) would not intercept them.
 */
@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = {
    "com.jcarranza.minimidoffice.service",
    "com.jcarranza.minimidoffice.persistence.hibernate",
    "com.jcarranza.minimidoffice.persistence.jdbc",          // JdbcReportingDao
    "com.jcarranza.minimidoffice.integration.sabre.mapper"   // SabreFlightSearch/CheckMapper (@Component)
})
@Import({ PersistenceConfig.class, IntegrationConfig.class })
public class AppConfig {
}
