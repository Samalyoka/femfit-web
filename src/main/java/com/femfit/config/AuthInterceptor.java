package com.femfit.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Adds isAuthenticated flag to every model so Thymeleaf
 * can show/hide nav buttons without sec:authorize.
 */

public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
        if (modelAndView != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAuthenticated = auth != null
                    && auth.isAuthenticated()
                    && !(auth.getPrincipal() instanceof String s && s.equals("anonymousUser"));
            modelAndView.addObject("isAuthenticated", isAuthenticated);
            if (isAuthenticated && auth.getAuthorities() != null) {
                auth.getAuthorities().stream()
                        .findFirst()
                        .ifPresent(a -> modelAndView.addObject("userRole",
                                a.getAuthority().replace("ROLE_", "")));
            }
        }
    }
}