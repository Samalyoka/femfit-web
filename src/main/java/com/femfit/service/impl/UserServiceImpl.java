package com.femfit.service.impl;

import com.femfit.dao.UserDao;
import com.femfit.dto.PageDto;
import com.femfit.dto.RegisterDto;
import com.femfit.exception.EmailAlreadyTakenException;
import com.femfit.exception.InvalidPasswordException;
import com.femfit.model.Role;
import com.femfit.model.User;
import com.femfit.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link UserService}.
 *
 * <p>Design patterns applied:
 * <ul>
 *   <li><strong>Strategy</strong> — PasswordEncoder is injected and can be
 *       swapped (BCrypt, SHA-256, etc.) without changing this class.</li>
 *   <li><strong>DAO</strong> — delegates all DB access to {@link UserDao}.</li>
 * </ul>
 * </p>
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserDao userDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(RegisterDto dto) {
        log.info("Registering new user: {}", dto.getEmail());

        if (userDao.existsByEmail(dto.getEmail())) {
            log.warn("Registration failed — email already taken: {}", dto.getEmail());
            throw new EmailAlreadyTakenException("Email already registered: " + dto.getEmail());
        }

        User user = User.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .birthDate(dto.getBirthDate())
                .role(Role.CLIENT)
                .active(true)
                .build();

        User saved = userDao.save(user);
        log.info("User registered successfully: id={}", saved.getId());
        return saved;
    }

    @Override
    public Optional<User> findById(Long id) {
        return userDao.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userDao.findByEmail(email);
    }

    @Override
    public PageDto<User> findByRole(Role role, int page, int size) {
        int offset = (page - 1) * size;
        List<User> users = userDao.findByRole(role, offset, size);
        int total = userDao.countByRole(role);
        return new PageDto<>(users, page, size, total);
    }

    @Override
    public void updateProfile(User user) {
        log.info("Updating profile for user id={}", user.getId());
        userDao.update(user);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            log.warn("Password change failed for user id={} — wrong old password", userId);
            throw new InvalidPasswordException("Current password is incorrect");
        }

        userDao.updatePassword(userId, passwordEncoder.encode(newPassword));
        log.info("Password changed for user id={}", userId);
    }

    @Override
    public void setActive(Long userId, boolean isActive) {
        log.info("Setting active={} for user id={}", isActive, userId);
        userDao.setActive(userId, isActive);
    }

    @Override
    public void setDiscount(Long userId, int discountPercent) {
        if (discountPercent < 0 || discountPercent > 100) {
            throw new IllegalArgumentException("Discount must be between 0 and 100");
        }
        log.info("Setting discount={}% for user id={}", discountPercent, userId);
        userDao.setDiscount(userId, discountPercent);
    }
}