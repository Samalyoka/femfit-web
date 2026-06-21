package com.femfit.service;

import com.femfit.dto.RegisterDto;
import com.femfit.dto.PageDto;
import com.femfit.model.AccountType;
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

    /**
     * Sets a client's account type (REGULAR or CORPORATE), and immediately
     * recalculates their discount to match the new type's rule (admin
     * function). See {@link #calculateAutoDiscount} for the exact rule.
     *
     * @param memberId    the member's id
     * @param accountType the new account type
     */
    void setAccountType(Long memberId, AccountType accountType);

    /**
     * Computes the discount a member is entitled to under the automatic
     * discount rule, without persisting anything.
     *
     * <ul>
     *   <li>CORPORATE accounts always receive a flat 10% discount,
     *       regardless of completed cycles.</li>
     *   <li>REGULAR accounts receive a discount that grows with loyalty:
     *       10+ completed cycles → 15%, 6+ → 10%, 3+ → 5%, otherwise 0%.</li>
     * </ul>
     *
     * @param member          the member (only {@code accountType} is read)
     * @param completedCycles number of the member's COMPLETED training cycle orders
     * @return the discount percentage (0-15) this member is entitled to
     */
    int calculateAutoDiscount(Member member, int completedCycles);

    /**
     * Recalculates and persists a member's discount using
     * {@link #calculateAutoDiscount}, based on their current account type
     * and number of completed training cycles. Intended to be called after
     * an order is marked COMPLETED, and is also safe to call manually
     * (e.g. from an admin "recalculate" action).
     *
     * @param memberId the member's id
     */
    void recalculateDiscount(Long memberId);
}