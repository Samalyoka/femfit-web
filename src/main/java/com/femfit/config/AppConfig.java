package com.femfit.config;

import com.femfit.util.pool.ConnectionPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;

/**
 * Root Spring application context.
 * Configures: Connection Pool, MessageSource, i18n.
 */
@Configuration
@ComponentScan(basePackages = {
        "com.femfit.service",
        "com.femfit.dao",
        "com.femfit.util"
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

    /**
     * Stores the user's selected locale in the HTTP session.
     */
    @Bean
    public SessionLocaleResolver localeResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }

    /**
     * Intercepts ?lang=ru requests and changes the session locale.
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    /**
     * BCrypt password encoder with work factor 12.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}