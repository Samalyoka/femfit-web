package com.femfit.config;

import com.femfit.service.FemFitUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.core.annotation.Order;

/**
 * Spring Security configuration.
 * Passwords stored as BCrypt hashes — plain-text storage is prohibited.
 * Authentication delegated to FemFitUserDetailsService (our JDBC members table).
 *
 * Access-denied (403) is handled here via accessDeniedPage, which renders
 * error/403.html — this is intercepted by Spring Security's
 * ExceptionTranslationFilter *before* the request reaches DispatcherServlet,
 * so it cannot be handled by GlobalExceptionHandler. All other unhandled
 * application errors (500) are handled centrally by
 * {@link com.femfit.exception.GlobalExceptionHandler}.
 */
@Configuration
@EnableWebSecurity
@Order(1)
public class SecurityConfig {

    private final FemFitUserDetailsService userDetailsService;
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    public SecurityConfig(FemFitUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
     * Security filter chain definition — establishes access control rules, login/logout, CSRF protection, and access-denied handling.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .headers(headers -> headers
                        .cacheControl(cache -> cache.disable())
                )
                .authenticationManager(authenticationManager())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                new AntPathRequestMatcher("/"),
                                new AntPathRequestMatcher("/home"),
                                new AntPathRequestMatcher("/schedule"),
                                new AntPathRequestMatcher("/schedule/**"),
                                new AntPathRequestMatcher("/plans"),
                                new AntPathRequestMatcher("/auth/login"),
                                new AntPathRequestMatcher("/auth/register"),
                                new AntPathRequestMatcher("/static/**"),
                                new AntPathRequestMatcher("/error"),
                                new AntPathRequestMatcher("/error/**")
                        ).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/admin/**")).hasRole("ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/trainer/**")).hasRole("TRAINER")
                        .requestMatchers(new AntPathRequestMatcher("/client/**")).hasRole("CLIENT")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/home", true)
                        .failureUrl("/auth/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .clearAuthentication(true)
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/error/403")
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                new AntPathRequestMatcher("/auth/login"),
                                new AntPathRequestMatcher("/client/book/**"),
                                new AntPathRequestMatcher("/client/booking/cancel/**")
                        )
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new org.springframework.security.authentication.ProviderManager(provider);
    }
}