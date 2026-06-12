package com.femfit.service;

import com.femfit.dto.RegisterDto;
import com.femfit.dto.PageDto;
import com.femfit.model.Role;
import com.femfit.model.Member;

import java.util.Optional;

/**
 * Service interface for member-related business logic.
 * All public methods are documented with Javadoc per project requirements.
 */
public interface MemberService {

    /**
     * Registers a new client in the system.
     * Password is hashed with BCrypt before storage.
     * Plain-text passwords are never stored.
     *
     * @param dto registration form data
     * @return the newly created Member with generated id
     * @throws com.femfit.exception.EmailAlreadyTakenException if email is already registered
     */
    Member register(RegisterDto dto);

    /**
     * Finds a member by their id.
     *
     * @param id the member id
     * @return Optional with the member, or empty if not found
     */
    Optional<Member> findById(Long id);

    /**
     * Finds a member by their email address.
     * Used during Spring Security authentication.
     *
     * @param email member's email
     * @return Optional with the member, or empty if not found
     */
    Optional<Member> findByEmail(String email);

    /**
     * Returns a paginated list of members with a specific role.
     *
     * @param role the role to filter by
     * @param page page number (1-based)
     * @param size page size
     * @return PageDto containing members and pagination metadata
     */
    PageDto<Member> findByRole(Role role, int page, int size);

    /**
     * Updates a member's profile information.
     *
     * @param member member with updated firstName, lastName, phone, birthDate
     */
    void updateProfile(Member member);

    /**
     * Changes a member's password after verifying the old one.
     *
     * @param memberId    the member's id
     * @param oldPassword the current password (plain text, will be verified)
     * @param newPassword the new password (plain text, will be hashed)
     * @throws com.femfit.exception.InvalidPasswordException if oldPassword is wrong
     */
    void changePassword(Long memberId, String oldPassword, String newPassword);

    /**
     * Activates or deactivates a member account (admin function).
     *
     * @param memberId the member's id
     * @param isActive true to activate, false to deactivate
     */
    void setActive(Long memberId, boolean isActive);

    /**
     * Sets a discount for a member (admin function).
     * Discount is applied to all future orders.
     *
     * @param memberId        the member's id
     * @param discountPercent value from 0 to 100
     */
    void setDiscount(Long memberId, int discountPercent);
}