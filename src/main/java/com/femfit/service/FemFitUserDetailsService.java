package com.femfit.service;

import com.femfit.dao.MemberDao;
import com.femfit.model.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Connects Spring Security authentication to our custom JDBC MemberDao.
 * Loads member by email and maps our Role enum to Spring Security GrantedAuthority.
 */
@Service
public class FemFitUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(FemFitUserDetailsService.class);

    private final MemberDao memberDao;

    @Autowired
    public FemFitUserDetailsService(MemberDao memberDao) {
        this.memberDao = memberDao;
    }

    /**
     * Loads a member by email for Spring Security authentication.
     *
     * @param email the member's email (used as username)
     * @return Spring Security UserDetails
     * @throws UsernameNotFoundException if no user found with given email
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading member by email: {}", email);

        Member member = memberDao.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", email);
                    return new UsernameNotFoundException("Member not found: " + email);
                });

        log.debug("Found member: id={}, email={}, active={}, role={}",
                member.getId(), member.getEmail(), member.isActive(), member.getRole());

        if (!member.isActive()) {
            log.warn("Inactive user tried to login: {}", email);
            throw new UsernameNotFoundException("Account is deactivated: " + email);
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(member.getEmail())
                .password(member.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name())))
                .build();
    }
}