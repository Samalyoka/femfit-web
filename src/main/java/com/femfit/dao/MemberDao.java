package com.femfit.dao;

import com.femfit.model.Member;
import com.femfit.model.Role;

import java.util.List;
import java.util.Optional;

/**
 * DAO interface for {@link Member} entity.
 * All implementations must use plain JDBC with PreparedStatements.
 * SQL string concatenation is strictly prohibited.
 */
public interface MemberDao {

    /**
     * Saves a new user to the database.
     *
     * @param member the user to save (id will be set after insert)
     * @return the saved user with generated id
     */
    Member save(Member member);

    /**
     * Finds a user by their unique id.
     *
     * @param id the user's id
     * @return Optional containing the user, or empty if not found
     */
    Optional<Member> findById(Long id);

    /**
     * Finds a user by their email address.
     * Used for authentication.
     *
     * @param email the user's email
     * @return Optional containing the user, or empty if not found
     */
    Optional<Member> findByEmail(String email);

    /**
     * Returns all users in the system.
     *
     * @return list of all users
     */
    List<Member> findAll();

    /**
     * Returns all users with a specific role, with pagination.
     *
     * @param role   the role to filter by
     * @param offset SQL OFFSET (for pagination)
     * @param limit  SQL LIMIT (page size)
     * @return paginated list of users with given role
     */
    List<Member> findByRole(Role role, int offset, int limit);

    /**
     * Counts users with a specific role.
     *
     * @param role the role to count
     * @return total count
     */
    int countByRole(Role role);

    /**
     * Updates an existing user's profile fields.
     * Does NOT update password — use {@link #updatePassword} for that.
     *
     * @param member the user with updated fields
     */
    void update(Member member);

    /**
     * Updates a user's hashed password.
     *
     * @param userId       the user's id
     * @param passwordHash new BCrypt hash
     */
    void updatePassword(Long userId, String passwordHash);

    /**
     * Sets the active/inactive status of a user.
     *
     * @param userId   the user's id
     * @param isActive true to activate, false to deactivate
     */
    void setActive(Long userId, boolean isActive);

    /**
     * Sets a discount percentage for a user (admin function).
     *
     * @param userId          the user's id
     * @param discountPercent discount value 0–100
     */
    void setDiscount(Long userId, int discountPercent);

    /**
     * Checks whether an email is already taken.
     *
     * @param email email to check
     * @return true if the email exists in the database
     */
    boolean existsByEmail(String email);
}