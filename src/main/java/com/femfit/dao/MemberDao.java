package com.femfit.dao;

import com.femfit.model.AccountType;
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

    Member save(Member member);
    Optional<Member> findById(Long id);
    Optional<Member> findByEmail(String email);
    List<Member> findAll();
    List<Member> findByRole(Role role, int offset, int limit);
    int countByRole(Role role);
    void update(Member member);
    void updatePassword(Long userId, String passwordHash);
    void setActive(Long userId, boolean isActive);
    void setDiscount(Long userId, int discountPercent);
    void setAccountType(Long userId, AccountType accountType);
    boolean existsByEmail(String email);

    /**
     * Updates the avatar_url column for a member.
     *
     * @param memberId  the member's id
     * @param avatarUrl relative web path, e.g. /static/img/avatars/42.jpg
     */
    void updateAvatarUrl(Long memberId, String avatarUrl);
}
