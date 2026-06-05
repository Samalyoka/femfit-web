package com.femfit.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Spring MVC configuration: Thymeleaf views, static resources, interceptors,
 * validation, i18n interceptor registration.
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "com.femfit.controller")
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private LocaleChangeInterceptor localeChangeInterceptor;

    // ── Thymeleaf ──────────────────────────────────────────────────────────

    /**
     * Resolves templates from /WEB-INF/views/ with .html extension.
     */
    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("/WEB-INF/views/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false); // set true in production
        return resolver;
    }

    /**
     * Template engine with Spring Security dialect for sec:authorize tags.
     */
    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(templateResolver());
        engine.setTemplateEngineMessageSource(messageSource);
        engine.addDialect(new SpringSecurityDialect());
        engine.setEnableSpringELCompiler(true);
        return engine;
    }

    /**
     * Thymeleaf view resolver — replaces InternalResourceViewResolver.
     */
    @Bean
    public ThymeleafViewResolver viewResolver() {
        ThymeleafViewResolver resolver = new ThymeleafViewResolver();
        resolver.setTemplateEngine(templateEngine());
        resolver.setCharacterEncoding("UTF-8");
        resolver.setOrder(1);
        return resolver;
    }

    // ── Static resources ───────────────────────────────────────────────────

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("/static/");
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("/static/img/");
    }

    // ── Interceptors ───────────────────────────────────────────────────────

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // i18n: switches locale on ?lang=ru
        registry.addInterceptor(localeChangeInterceptor);
    }

    // ── Validation ─────────────────────────────────────────────────────────

    /**
     * Bean validation backed by Hibernate Validator.
     * Connects validation messages to MessageSource for i18n error messages.
     */
    @Bean
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        return validator;
    }

    @Override
    public org.springframework.validation.Validator getValidator() {
        return validator();
    }
}