package com.femfit.dao.impl;

import com.femfit.dao.ReviewDao;
import com.femfit.model.Review;
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
 * JDBC implementation of {@link ReviewDao}.
 *
 * Uses PreparedStatements exclusively to prevent SQL injection.
 * Reviews are linked to completed orders and cannot be duplicated
 * per member/order pair (enforced by DB unique constraint).
 */
@Repository
public class ReviewDaoImpl implements ReviewDao {

    private static final Logger log = LoggerFactory.getLogger(ReviewDaoImpl.class);

    private final ConnectionPool pool;

    @Autowired
    public ReviewDaoImpl(ConnectionPool pool) {
        this.pool = pool;
    }

    private static final String INSERT = """
            INSERT INTO reviews (order_id, member_id, trainer_id, rating, comment, created_at)
            VALUES (?, ?, ?, ?, ?, NOW())
            RETURNING id, created_at
            """;

    private static final String SELECT_BY_ORDER = """
            SELECT r.id, r.order_id, r.member_id, r.trainer_id, r.rating, r.comment, r.created_at,
                   m.first_name || ' ' || m.last_name AS member_name,
                   tc.title AS cycle_title,
                   tc.title_ru AS cycle_title_ru,
                   tc.title_kz AS cycle_title_kz
            FROM reviews r
            JOIN members m ON r.member_id = m.id
            JOIN orders o ON r.order_id = o.id
            JOIN training_cycles tc ON o.cycle_id = tc.id
            WHERE r.order_id = ?
            """;

    private static final String SELECT_BY_MEMBER = """
            SELECT r.id, r.order_id, r.member_id, r.trainer_id, r.rating, r.comment, r.created_at,
                   m.first_name || ' ' || m.last_name AS member_name,
                   tc.title AS cycle_title,
                   tc.title_ru AS cycle_title_ru,
                   tc.title_kz AS cycle_title_kz
            FROM reviews r
            JOIN members m ON r.member_id = m.id
            JOIN orders o ON r.order_id = o.id
            JOIN training_cycles tc ON o.cycle_id = tc.id
            WHERE r.member_id = ?
            ORDER BY r.created_at DESC
            """;

    private static final String SELECT_RECENT = """
            SELECT r.id, r.order_id, r.member_id, r.trainer_id, r.rating, r.comment, r.created_at,
                   m.first_name || ' ' || m.last_name AS member_name,
                   tc.title AS cycle_title,
                   tc.title_ru AS cycle_title_ru,
                   tc.title_kz AS cycle_title_kz
            FROM reviews r
            JOIN members m ON r.member_id = m.id
            JOIN orders o ON r.order_id = o.id
            JOIN training_cycles tc ON o.cycle_id = tc.id
            ORDER BY r.created_at DESC
            LIMIT ?
            """;

    private static final String EXISTS_BY_ORDER = """
            SELECT EXISTS(SELECT 1 FROM reviews WHERE order_id = ?)
            """;

    /**
     * Saves a new review. Sets id and createdAt from DB response.
     *
     * @param review the review to save
     * @return saved review with id and createdAt populated
     * @throws RuntimeException if save fails
     */
    @Override
    public Review save(Review review) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setLong(1, review.getOrderId());
            ps.setLong(2, review.getMemberId());
            if (review.getTrainerId() != null) {
                ps.setLong(3, review.getTrainerId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }
            ps.setInt(4, review.getRating());
            ps.setString(5, review.getComment());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    review.setId(rs.getLong("id"));
                    review.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                }
            }
            log.info("Review saved: id={}, orderId={}, rating={}", review.getId(), review.getOrderId(), review.getRating());
            return review;
        } catch (SQLException e) {
            log.error("Error saving review for orderId={}: {}", review.getOrderId(), e.getMessage());
            throw new RuntimeException("Failed to save review", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    /**
     * Finds a review by order ID.
     *
     * @param orderId the order ID
     * @return Optional containing the review if found, empty otherwise
     */
    @Override
    public Optional<Review> findByOrderId(Long orderId) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_ORDER)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding review for orderId={}: {}", orderId, e.getMessage());
            throw new RuntimeException("Failed to find review", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return Optional.empty();
    }

    /**
     * Finds all reviews by a member, ordered by most recent first.
     *
     * @param memberId the member ID
     * @return list of reviews
     */
    @Override
    public List<Review> findByMemberId(Long memberId) {
        Connection conn = pool.getConnection();
        List<Review> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_MEMBER)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding reviews for memberId={}: {}", memberId, e.getMessage());
            throw new RuntimeException("Failed to find reviews", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return list;
    }

    /**
     * Finds recent reviews for public display on the home page.
     *
     * @param limit maximum number of reviews to return
     * @return list of recent reviews
     */
    @Override
    public List<Review> findRecentForDisplay(int limit) {
        Connection conn = pool.getConnection();
        List<Review> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_RECENT)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding recent reviews: {}", e.getMessage());
            throw new RuntimeException("Failed to find reviews", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return list;
    }

    /**
     * Checks if a review already exists for the given order.
     *
     * @param orderId the order ID
     * @return true if review exists, false otherwise
     */
    @Override
    public boolean existsByOrderId(Long orderId) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(EXISTS_BY_ORDER)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            log.error("Error checking review existence for orderId={}: {}", orderId, e.getMessage());
            throw new RuntimeException("Failed to check review", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    /**
     * Maps a ResultSet row to a {@link Review} object.
     *
     * @param rs the result set positioned at the current row
     * @return a populated Review object
     * @throws SQLException if a column cannot be read
     */
    private Review mapRow(ResultSet rs) throws SQLException {
        return Review.builder()
                .id(rs.getLong("id"))
                .orderId(rs.getLong("order_id"))
                .memberId(rs.getLong("member_id"))
                .trainerId(rs.getObject("trainer_id") != null ? rs.getLong("trainer_id") : null)
                .rating(rs.getInt("rating"))
                .comment(rs.getString("comment"))
                .createdAt(rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toLocalDateTime() : null)
                .memberName(rs.getString("member_name"))
                .cycleTitle(rs.getString("cycle_title"))
                .cycleTitleRu(rs.getString("cycle_title_ru"))
                .cycleTitleKz(rs.getString("cycle_title_kz"))
                .build();
    }
}