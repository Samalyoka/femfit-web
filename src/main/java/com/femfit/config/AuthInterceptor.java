package com.femfit.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Adds isAuthenticated and userRole attributes to every model so Thymeleaf
 * templates (e.g. {@code common/layout :: nav}) can show or hide navigation
 * links without relying on the {@code sec:authorize} dialect in every fragment.
 *
 * @implNote This is an application of the <b>Interceptor pattern</b>
 * (Spring MVC {@link HandlerInterceptor}). It centralises a cross-cutting
 * concern — authentication status used purely for view rendering — in one
 * place, registered once in {@code WebMvcConfig.addInterceptors()}, instead
 * of duplicating {@code model.addAttribute("isAuthenticated", ...)} calls in
 * every controller method.
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