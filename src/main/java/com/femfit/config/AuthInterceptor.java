package com.femfit.config;

import com.femfit.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Adds isAuthenticated, userRole, currentUserName, currentUserInitials,
 * and currentUserAvatar to every model — used by Thymeleaf nav fragment.
 *
 * @implNote Interceptor pattern — registered once in WebMvcConfig.
 */
public class AuthInterceptor implements HandlerInterceptor {

    private final MemberService memberService;

    public AuthInterceptor(MemberService memberService) {
        this.memberService = memberService;
    }

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
                if (auth.getPrincipal() instanceof UserDetails ud) {
                    memberService.findByEmail(ud.getUsername()).ifPresentOrElse(m -> {
                        modelAndView.addObject("currentUserName",
                                m.getFirstName() + " " + m.getLastName());
                        modelAndView.addObject("currentUserInitials", m.getInitials());
                        modelAndView.addObject("currentUserAvatar", m.getAvatarUrl());
                    }, () -> {
                        modelAndView.addObject("currentUserName", ud.getUsername());
                        modelAndView.addObject("currentUserInitials", "?");
                        modelAndView.addObject("currentUserAvatar", null);
                    });
                }
            }
        }
    }
}
