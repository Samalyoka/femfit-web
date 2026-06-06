package com.femfit.service;

import com.femfit.dao.UserDao;
import com.femfit.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Connects Spring Security authentication to our custom JDBC UserDao.
 * Loads user by email and maps our Role enum to Spring Security GrantedAuthority.
 */
@Service
public class FemFitUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(FemFitUserDetailsService.class);

    private final UserDao userDao;

    @Autowired
    public FemFitUserDetailsService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Loads a user by email for Spring Security authentication.
     *
     * @param email the user's email (used as username)
     * @return Spring Security UserDetails
     * @throws UsernameNotFoundException if no user found with given email
     */

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        User user = userDao.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", email);
                    return new UsernameNotFoundException("User not found: " + email);
                });

        log.debug("Found user: id={}, email={}, active={}, role={}, passwordHash={}",
                user.getId(), user.getEmail(), user.isActive(), user.getRole(),
                user.getPasswordHash().substring(0, 10) + "..."); // первые 10 символов хеша

        if (!user.isActive()) {
            log.warn("Inactive user tried to login: {}", email);
            throw new UsernameNotFoundException("Account is deactivated: " + email);
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build();
    }
}