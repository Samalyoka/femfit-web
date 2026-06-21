package com.femfit.config;

import com.femfit.datasource.ConnectionPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.Locale;

/**
 * Root Spring application context.
 * Configures: Connection Pool, MessageSource, i18n.
 *
 * Note: PasswordEncoder is defined in {@link SecurityConfig}, not here —
 * it's a security-specific bean used directly by SecurityConfig's
 * AuthenticationManager, so it lives alongside the rest of the security
 * setup rather than being duplicated across two configuration classes.
 */
@Configuration
@ComponentScan(basePackages = {
        "com.femfit.service",
        "com.femfit.dao"
})
@PropertySource("classpath:application.properties")
public class AppConfig {

    @Value("${db.url}")
    private String dbUrl;

    @Value("${db.username}")
    private String dbUsername;

    @Value("${db.password}")
    private String dbPassword;

    @Value("${db.driver}")
    private String dbDriver;

    @Value("${db.pool.size:10}")
    private int poolSize;

    /**
     * Registers PropertySourcesPlaceholderConfigurer as static bean
     * so @Value annotations are resolved before other beans.
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    /**
     * Custom thread-safe JDBC Connection Pool.
     * ORM (JPA/Hibernate) is NOT used — plain JDBC only.
     *
     * @return initialised ConnectionPool singleton
     */
    @Bean(destroyMethod = "shutdown")
    public ConnectionPool connectionPool() {
        return new ConnectionPool(dbUrl, dbUsername, dbPassword, dbDriver, poolSize);
    }

    /**
     * MessageSource for i18n (EN, RU, KZ).
     * Locale strings stored in src/main/resources/i18n/messages_*.properties
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source =
                new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setDefaultLocale(Locale.ENGLISH);
        source.setCacheSeconds(3600);
        return source;
    }
}