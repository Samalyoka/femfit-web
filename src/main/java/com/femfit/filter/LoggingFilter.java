package com.femfit.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Servlet filter that logs every incoming HTTP request.
 * Logs: HTTP method, URI, response status, execution time in ms.
 */
@WebFilter("/*")
public class LoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("LoggingFilter initialised");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();
        long startTime = System.currentTimeMillis();

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = httpResponse.getStatus();

            if (status >= 500) {
                log.error("{} {} -> {} ({}ms)", method, uri, status, duration);
            } else if (status >= 400) {
                log.warn("{} {} -> {} ({}ms)", method, uri, status, duration);
            } else {
                log.debug("{} {} -> {} ({}ms)", method, uri, status, duration);
            }
        }
    }

    @Override
    public void destroy() {
        log.debug("LoggingFilter destroyed");
    }
}