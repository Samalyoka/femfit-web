package com.femfit.service;

import com.femfit.dto.RegisterDto;
import com.femfit.dto.PageDto;
import com.femfit.model.Role;
import com.femfit.model.User;

import java.util.Optional;

/**
 * Service interface for user-related business logic.
 * All public methods are documented with Javadoc per project requirements.
 */
public interface UserService {

    /**
     * Registers a new client in the system.
     * Password is hashed with BCrypt before storage.
     * Plain-text passwords are never stored.
     *
     * @param dto registration form data
     * @return the newly created User with generated id
     * @throws com.femfit.exception.EmailAlreadyTakenException if email is already registered
     * @throws com.femfit.exception.ValidationException if input data is invalid
     */
    User register(RegisterDto dto);

    /**
     * Finds a user by their id.
     *
     * @param id the user id
     * @return Optional with the user, or empty if not found
     */
    Optional<User> findById(Long id);

    /**
     * Finds a user by their email address.
     * Used during Spring Security authentication.
     *
     * @param email user's email
     * @return Optional with the user, or empty if not found
     */
    Optional<User> findByEmail(String email);

    /**
     * Returns a paginated list of users with a specific role.
     *
     * @param role the role to filter by
     * @param page page number (1-based)
     * @param size page size
     * @return PageDto containing users and pagination metadata
     */
    PageDto<User> findByRole(Role role, int page, int size);

    /**
     * Updates a user's profile information.
     *
     * @param user user with updated firstName, lastName, phone, birthDate
     */
    void updateProfile(User user);

    /**
     * Changes a user's password after verifying the old one.
     *
     * @param userId      the user's id
     * @param oldPassword the current password (plain text, will be verified)
     * @param newPassword the new password (plain text, will be hashed)
     * @throws com.femfit.exception.InvalidPasswordException if oldPassword is wrong
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * Activates or deactivates a user account (admin function).
     *
     * @param userId   the user's id
     * @param isActive true to activate, false to deactivate
     */
    void setActive(Long userId, boolean isActive);

    /**
     * Sets a discount for a user (admin function).
     * Discount is applied to all future orders.
     *
     * @param userId          the user's id
     * @param discountPercent value from 0 to 100
     */
    void setDiscount(Long userId, int discountPercent);
}