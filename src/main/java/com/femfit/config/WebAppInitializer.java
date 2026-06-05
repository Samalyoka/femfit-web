package com.femfit.config;

import com.femfit.filter.EncodingFilter;
import com.femfit.filter.LoggingFilter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Replaces web.xml entirely.
 * Registers DispatcherServlet, Spring contexts, and Servlet filters.
 */
public class WebAppInitializer implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {

        // Root application context (services, DAOs)
        AnnotationConfigWebApplicationContext rootContext =
                new AnnotationConfigWebApplicationContext();
        rootContext.register(AppConfig.class);  // ← убери SecurityConfig отсюда
        servletContext.addListener(new ContextLoaderListener(rootContext));

        // Web (MVC) context
        AnnotationConfigWebApplicationContext webContext =
                new AnnotationConfigWebApplicationContext();
        webContext.register(WebMvcConfig.class, SecurityConfig.class);  // ← добавь сюда

        // Register DispatcherServlet
        ServletRegistration.Dynamic dispatcher =
                servletContext.addServlet("dispatcher", new DispatcherServlet(webContext));
        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("/");

        // Register EncodingFilter (UTF-8 for RU/KZ locales)
        FilterRegistration.Dynamic encodingFilter =
                servletContext.addFilter("encodingFilter", new EncodingFilter());
        encodingFilter.addMappingForUrlPatterns(null, false, "/*");

        // Register LoggingFilter (request logging)
        FilterRegistration.Dynamic loggingFilter =
                servletContext.addFilter("loggingFilter", new LoggingFilter());
        loggingFilter.addMappingForUrlPatterns(null, false, "/*");

        servletContext.setInitParameter("defaultHtmlEscape", "true");
    }
}