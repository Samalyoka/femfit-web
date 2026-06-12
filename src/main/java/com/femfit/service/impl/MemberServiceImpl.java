package com.femfit.service.impl;

import com.femfit.dao.MemberDao;
import com.femfit.dto.PageDto;
import com.femfit.dto.RegisterDto;
import com.femfit.exception.EmailAlreadyTakenException;
import com.femfit.exception.InvalidPasswordException;
import com.femfit.model.Role;
import com.femfit.model.Member;
import com.femfit.service.MemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link MemberService}.
 *
 * <p>Design patterns applied:
 * <ul>
 *   <li><strong>Strategy</strong> — PasswordEncoder is injected and can be
 *       swapped (BCrypt, SHA-256, etc.) without changing this class.</li>
 *   <li><strong>DAO</strong> — delegates all DB access to {@link MemberDao}.</li>
 * </ul>
 * </p>
 */
@Service
public class MemberServiceImpl implements MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberServiceImpl.class);

    private final MemberDao memberDao;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public MemberServiceImpl(MemberDao memberDao, PasswordEncoder passwordEncoder) {
        this.memberDao = memberDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Member register(RegisterDto dto) {
        log.info("Registering new user: {}", dto.getEmail());

        if (memberDao.existsByEmail(dto.getEmail())) {
            log.warn("Registration failed — email already taken: {}", dto.getEmail());
            throw new EmailAlreadyTakenException("Email already registered: " + dto.getEmail());
        }

        Member member = Member.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .birthDate(dto.getBirthDate())
                .role(Role.CLIENT)
                .active(true)
                .build();

        Member saved = memberDao.save(member);
        log.info("User registered successfully: id={}", saved.getId());
        return saved;
    }

    @Override
    public Optional<Member> findById(Long id) {
        return memberDao.findById(id);
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        return memberDao.findByEmail(email);
    }

    @Override
    public PageDto<Member> findByRole(Role role, int page, int size) {
        int offset = (page - 1) * size;
        List<Member> members = memberDao.findByRole(role, offset, size);
        int total = memberDao.countByRole(role);
        return new PageDto<>(members, page, size, total);
    }

    @Override
    public void updateProfile(Member member) {
        log.info("Updating profile for user id={}", member.getId());
        memberDao.update(member);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        Member member = memberDao.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (!passwordEncoder.matches(oldPassword, member.getPasswordHash())) {
            log.warn("Password change failed for user id={} — wrong old password", userId);
            throw new InvalidPasswordException("Current password is incorrect");
        }

        memberDao.updatePassword(userId, passwordEncoder.encode(newPassword));
        log.info("Password changed for user id={}", userId);
    }

    @Override
    public void setActive(Long userId, boolean isActive) {
        log.info("Setting active={} for user id={}", isActive, userId);
        memberDao.setActive(userId, isActive);
    }

    @Override
    public void setDiscount(Long userId, int discountPercent) {
        if (discountPercent < 0 || discountPercent > 100) {
            throw new IllegalArgumentException("Discount must be between 0 and 100");
        }
        log.info("Setting discount={}% for user id={}", discountPercent, userId);
        memberDao.setDiscount(userId, discountPercent);
    }
}