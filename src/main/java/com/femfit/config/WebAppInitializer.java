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
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Replaces web.xml entirely.
 * Registers DispatcherServlet, Spring contexts, and Servlet filters.
 * Spring Security filter chain is activated via DelegatingFilterProxy
 * which lazily resolves springSecurityFilterChain bean from web context.
 */
public class WebAppInitializer implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {

        AnnotationConfigWebApplicationContext rootContext =
                new AnnotationConfigWebApplicationContext();
        rootContext.register(AppConfig.class, SecurityConfig.class);
        servletContext.addListener(new ContextLoaderListener(rootContext));

        AnnotationConfigWebApplicationContext webContext =
                new AnnotationConfigWebApplicationContext();
        webContext.register(WebMvcConfig.class);

        ServletRegistration.Dynamic dispatcher =
                servletContext.addServlet("dispatcher", new DispatcherServlet(webContext));
        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("/");

        FilterRegistration.Dynamic securityFilter = servletContext.addFilter(
                "springSecurityFilterChain",
                new DelegatingFilterProxy("springSecurityFilterChain"));
        securityFilter.addMappingForUrlPatterns(null, false, "/*");

        FilterRegistration.Dynamic encodingFilter =
                servletContext.addFilter("encodingFilter", new EncodingFilter());
        encodingFilter.addMappingForUrlPatterns(null, false, "/*");

        FilterRegistration.Dynamic loggingFilter =
                servletContext.addFilter("loggingFilter", new LoggingFilter());
        loggingFilter.addMappingForUrlPatterns(null, false, "/*");

        servletContext.setInitParameter("defaultHtmlEscape", "true");
    }
}