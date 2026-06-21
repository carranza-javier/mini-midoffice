package com.jcarranza.minimidoffice.persistence.config;

import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@PropertySource("classpath:db.properties")
public class PersistenceConfig {

    @Value("${db.url}")
    private String dbUrl;

    @Value("${db.username}")
    private String dbUsername;

    @Value("${db.password}")
    private String dbPassword;

    @Value("${db.pool.maxSize:10}")
    private int maxPoolSize;

    @Bean(destroyMethod = "close")
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        // Explicit driver class: required in WAR/Tomcat because DriverManager
        // uses the bootstrap classloader and cannot see the driver in WEB-INF/lib.
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setJdbcUrl(dbUrl);
        ds.setUsername(dbUsername);
        ds.setPassword(dbPassword);
        ds.setMaximumPoolSize(maxPoolSize);
        ds.setConnectionTimeout(30_000);
        ds.setIdleTimeout(600_000);
        ds.setMaxLifetime(1_800_000);
        ds.setPoolName("MiniMidofficePool");
        return ds;
    }

    @Bean
    public LocalSessionFactoryBean sessionFactory(DataSource dataSource) {
        LocalSessionFactoryBean factory = new LocalSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.jcarranza.minimidoffice.domain.model");
        factory.setHibernateProperties(hibernateProperties());
        return factory;
    }

    @Bean
    public HibernateTransactionManager transactionManager(SessionFactory sessionFactory) {
        return new HibernateTransactionManager(sessionFactory);
    }

    private Properties hibernateProperties() {
        Properties p = new Properties();
        p.setProperty("hibernate.dialect",
                       "org.hibernate.dialect.PostgreSQL95Dialect");
        p.setProperty("hibernate.hbm2ddl.auto",   "validate");
        p.setProperty("hibernate.show_sql",        "false");
        p.setProperty("hibernate.format_sql",      "false");
        p.setProperty("hibernate.jdbc.batch_size", "25");
        p.setProperty("hibernate.default_schema",  "public");
        // Spring manages the Session lifecycle — required for getCurrentSession()
        p.setProperty("hibernate.current_session_context_class",
                       "org.springframework.orm.hibernate5.SpringSessionContext");
        return p;
    }
}
