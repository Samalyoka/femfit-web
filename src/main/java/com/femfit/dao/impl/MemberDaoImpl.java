package com.femfit.dao.impl;

import com.femfit.dao.MemberDao;
import com.femfit.model.AccountType;
import com.femfit.model.Member;
import com.femfit.model.Role;
import com.femfit.datasource.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link MemberDao}.
 *
 * <p>All queries use {@link PreparedStatement} to prevent SQL injection.
 * String concatenation in SQL is strictly prohibited.</p>
 *
 * <p>Design pattern: <strong>DAO (Data Access Object)</strong></p>
 */
@Repository
public class MemberDaoImpl implements MemberDao {

    private static final Logger log = LoggerFactory.getLogger(MemberDaoImpl.class);

    private final ConnectionPool pool;

    @Autowired
    public MemberDaoImpl(ConnectionPool pool) {
        this.pool = pool;
    }

    private static final String INSERT_USER = """
            INSERT INTO members (first_name, last_name, email, phone, password_hash,
                               birth_date, role_id, is_active, registration_date, discount_percent, account_type)
            VALUES (?, ?, ?, ?, ?, ?, (SELECT id FROM roles WHERE name = ?),
                    TRUE, NOW(), 0, 'REGULAR')
            RETURNING id
            """;

    private static final String SELECT_BY_ID = """
            SELECT u.id, u.first_name, u.last_name, u.email, u.phone,
                   u.password_hash, u.birth_date, r.name AS role,
                   u.is_active, u.registration_date, u.discount_percent, u.account_type
            FROM members u
            JOIN roles r ON u.role_id = r.id
            WHERE u.id = ?
            """;

    private static final String SELECT_BY_EMAIL = """
            SELECT u.id, u.first_name, u.last_name, u.email, u.phone,
                   u.password_hash, u.birth_date, r.name AS role,
                   u.is_active, u.registration_date, u.discount_percent, u.account_type
            FROM members u
            JOIN roles r ON u.role_id = r.id
            WHERE u.email = ?
            """;

    private static final String SELECT_ALL = """
            SELECT u.id, u.first_name, u.last_name, u.email, u.phone,
                   u.password_hash, u.birth_date, r.name AS role,
                   u.is_active, u.registration_date, u.discount_percent, u.account_type
            FROM members u
            JOIN roles r ON u.role_id = r.id
            ORDER BY u.registration_date DESC
            """;

    private static final String SELECT_BY_ROLE = """
            SELECT u.id, u.first_name, u.last_name, u.email, u.phone,
                   u.password_hash, u.birth_date, r.name AS role,
                   u.is_active, u.registration_date, u.discount_percent, u.account_type
            FROM members u
            JOIN roles r ON u.role_id = r.id
            WHERE r.name = ?
            ORDER BY u.last_name, u.first_name
            LIMIT ? OFFSET ?
            """;

    private static final String COUNT_BY_ROLE = """
            SELECT COUNT(*) FROM members u
            JOIN roles r ON u.role_id = r.id
            WHERE r.name = ?
            """;

    private static final String UPDATE_USER = """
            UPDATE members SET first_name = ?, last_name = ?, phone = ?, birth_date = ?
            WHERE id = ?
            """;

    private static final String UPDATE_PASSWORD = """
            UPDATE members SET password_hash = ? WHERE id = ?
            """;

    private static final String SET_ACTIVE = """
            UPDATE members SET is_active = ? WHERE id = ?
            """;

    private static final String SET_DISCOUNT = """
            UPDATE members SET discount_percent = ? WHERE id = ?
            """;

    private static final String SET_ACCOUNT_TYPE = """
            UPDATE members SET account_type = ? WHERE id = ?
            """;

    private static final String EXISTS_BY_EMAIL = """
            SELECT EXISTS(SELECT 1 FROM members WHERE email = ?)
            """;

    @Override
    public Member save(Member member) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT_USER)) {
            ps.setString(1, member.getFirstName());
            ps.setString(2, member.getLastName());
            ps.setString(3, member.getEmail());
            ps.setString(4, member.getPhone());
            ps.setString(5, member.getPasswordHash());
            ps.setObject(6, member.getBirthDate());
            ps.setString(7, member.getRole().name());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    member.setId(rs.getLong("id"));
                }
            }
            log.debug("User saved: id={}, email={}", member.getId(), member.getEmail());
            return member;
        } catch (SQLException e) {
            log.error("Error saving user: {}", e.getMessage());
            throw new RuntimeException("Failed to save user", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    @Override
    public Optional<Member> findById(Long id) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding user by id {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to find user by id", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding user by email: {}", e.getMessage());
            throw new RuntimeException("Failed to find user by email", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return Optional.empty();
    }

    @Override
    public List<Member> findAll() {
        Connection conn = pool.getConnection();
        List<Member> members = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                members.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error fetching all members: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch members", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return members;
    }

    @Override
    public List<Member> findByRole(Role role, int offset, int limit) {
        Connection conn = pool.getConnection();
        List<Member> members = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_ROLE)) {
            ps.setString(1, role.name());
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error finding members by role {}: {}", role, e.getMessage());
            throw new RuntimeException("Failed to find members by role", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return members;
    }

    @Override
    public int countByRole(Role role) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(COUNT_BY_ROLE)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Error counting members by role: {}", e.getMessage());
            throw new RuntimeException("Failed to count members", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return 0;
    }

    @Override
    public void update(Member member) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_USER)) {
            ps.setString(1, member.getFirstName());
            ps.setString(2, member.getLastName());
            ps.setString(3, member.getPhone());
            ps.setObject(4, member.getBirthDate());
            ps.setLong(5, member.getId());
            ps.executeUpdate();
            log.debug("User updated: id={}", member.getId());
        } catch (SQLException e) {
            log.error("Error updating user {}: {}", member.getId(), e.getMessage());
            throw new RuntimeException("Failed to update user", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    @Override
    public void updatePassword(Long userId, String passwordHash) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_PASSWORD)) {
            ps.setString(1, passwordHash);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error updating password for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to update password", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    @Override
    public void setActive(Long userId, boolean isActive) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SET_ACTIVE)) {
            ps.setBoolean(1, isActive);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error setting active={} for user {}: {}", isActive, userId, e.getMessage());
            throw new RuntimeException("Failed to update user status", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    @Override
    public void setDiscount(Long userId, int discountPercent) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SET_DISCOUNT)) {
            ps.setInt(1, discountPercent);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error setting discount for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to set discount", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    @Override
    public void setAccountType(Long userId, AccountType accountType) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SET_ACCOUNT_TYPE)) {
            ps.setString(1, accountType.name());
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error setting account type for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to set account type", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(EXISTS_BY_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            log.error("Error checking email existence: {}", e.getMessage());
            throw new RuntimeException("Failed to check email", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    /**
     * Maps a ResultSet row to a {@link Member} object.
     *
     * @param rs the result set positioned at the current row
     * @return a populated User object
     * @throws SQLException if a column cannot be read
     */
    private Member mapRow(ResultSet rs) throws SQLException {
        String accountTypeRaw = rs.getString("account_type");
        return Member.builder()
                .id(rs.getLong("id"))
                .firstName(rs.getString("first_name"))
                .lastName(rs.getString("last_name"))
                .email(rs.getString("email"))
                .phone(rs.getString("phone"))
                .passwordHash(rs.getString("password_hash"))
                .birthDate(rs.getDate("birth_date") != null
                        ? rs.getDate("birth_date").toLocalDate() : null)
                .role(Role.valueOf(rs.getString("role")))
                .active(rs.getBoolean("is_active"))
                .registrationDate(rs.getTimestamp("registration_date") != null
                        ? rs.getTimestamp("registration_date").toLocalDateTime() : null)
                .discountPercent(rs.getInt("discount_percent"))
                .accountType(accountTypeRaw != null ? AccountType.valueOf(accountTypeRaw) : AccountType.REGULAR)
                .build();
    }
}