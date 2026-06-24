package com.femfit.dao.impl;

import com.femfit.dao.OrderDao;
import com.femfit.model.Order;
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
 * JDBC implementation of {@link OrderDao}.
 * Transaction management implemented manually via JDBC for data integrity.
 */
@Repository
public class OrderDaoImpl implements OrderDao {

    private static final Logger log = LoggerFactory.getLogger(OrderDaoImpl.class);

    private final ConnectionPool pool;

    @Autowired
    public OrderDaoImpl(ConnectionPool pool) {
        this.pool = pool;
    }

    private static final String INSERT = """
            INSERT INTO orders (member_id, cycle_id, trainer_id, status, paid_amount, created_at)
            VALUES (?, ?, ?, 'ACTIVE', ?, NOW())
            RETURNING id, created_at
            """;

    private static final String SELECT_BY_ID = """
            SELECT o.id, o.member_id, o.cycle_id, o.trainer_id, o.status,
                   o.paid_amount, o.created_at, o.completed_at,
                   u.first_name || ' ' || u.last_name AS client_name,
                   tc.title AS cycle_title,
                   tc.title_ru AS cycle_title_ru,
                   tc.title_kz AS cycle_title_kz,
                   t.first_name || ' ' || t.last_name AS trainer_name
            FROM orders o
            JOIN members u ON o.member_id = u.id
            JOIN training_cycles tc ON o.cycle_id = tc.id
            LEFT JOIN members t ON o.trainer_id = t.id
            WHERE o.id = ?
            """;

    private static final String SELECT_BY_USER = """
            SELECT o.id, o.member_id, o.cycle_id, o.trainer_id, o.status,
                   o.paid_amount, o.created_at, o.completed_at,
                   u.first_name || ' ' || u.last_name AS client_name,
                   tc.title AS cycle_title,
                   tc.title_ru AS cycle_title_ru,
                   tc.title_kz AS cycle_title_kz,
                   t.first_name || ' ' || t.last_name AS trainer_name
            FROM orders o
            JOIN members u ON o.member_id = u.id
            JOIN training_cycles tc ON o.cycle_id = tc.id
            LEFT JOIN members t ON o.trainer_id = t.id
            WHERE o.member_id = ?
            ORDER BY o.created_at DESC
            """;

    private static final String SELECT_BY_USER_PAGED = """
            SELECT o.id, o.member_id, o.cycle_id, o.trainer_id, o.status,
                   o.paid_amount, o.created_at, o.completed_at,
                   u.first_name || ' ' || u.last_name AS client_name,
                   tc.title AS cycle_title,
                   tc.title_ru AS cycle_title_ru,
                   tc.title_kz AS cycle_title_kz,
                   t.first_name || ' ' || t.last_name AS trainer_name
            FROM orders o
            JOIN members u ON o.member_id = u.id
            JOIN training_cycles tc ON o.cycle_id = tc.id
            LEFT JOIN members t ON o.trainer_id = t.id
            WHERE o.member_id = ?
            ORDER BY
              CASE o.status
                WHEN 'PENDING'   THEN 1
                WHEN 'ACTIVE'    THEN 2
                WHEN 'COMPLETED' THEN 3
                WHEN 'CANCELLED' THEN 4
                ELSE 5
              END,
              o.created_at DESC
            LIMIT ? OFFSET ?
            """;

    private static final String COUNT_BY_USER = "SELECT COUNT(*) FROM orders WHERE member_id = ?";

    private static final String COUNT_COMPLETED_BY_USER =
            "SELECT COUNT(*) FROM orders WHERE member_id = ? AND status = 'COMPLETED'";

    private static final String SELECT_ACTIVE_BY_TRAINER = """
            SELECT o.id, o.member_id, o.cycle_id, o.trainer_id, o.status,
                   o.paid_amount, o.created_at, o.completed_at,
                   u.first_name || ' ' || u.last_name AS client_name,
                   tc.title AS cycle_title,
                   tc.title_ru AS cycle_title_ru,
                   tc.title_kz AS cycle_title_kz,
                   t.first_name || ' ' || t.last_name AS trainer_name
            FROM orders o
            JOIN members u ON o.member_id = u.id
            JOIN training_cycles tc ON o.cycle_id = tc.id
            LEFT JOIN members t ON o.trainer_id = t.id
            WHERE o.trainer_id = ? AND o.status IN ('PENDING', 'ACTIVE')
            ORDER BY o.created_at DESC
            """;

    private static final String SELECT_ALL = """
            SELECT o.id, o.member_id, o.cycle_id, o.trainer_id, o.status,
                   o.paid_amount, o.created_at, o.completed_at,
                   u.first_name || ' ' || u.last_name AS client_name,
                   tc.title AS cycle_title,
                   tc.title_ru AS cycle_title_ru,
                   tc.title_kz AS cycle_title_kz,
                   t.first_name || ' ' || t.last_name AS trainer_name
            FROM orders o
            JOIN members u ON o.member_id = u.id
            JOIN training_cycles tc ON o.cycle_id = tc.id
            LEFT JOIN members t ON o.trainer_id = t.id
            ORDER BY
            CASE o.status
            WHEN 'PENDING'   THEN 1
            WHEN 'ACTIVE'    THEN 2
            WHEN 'COMPLETED' THEN 3
            WHEN 'CANCELLED' THEN 4
            ELSE 5
            END,
            o.created_at DESC
            LIMIT ? OFFSET ?
            """;

    private static final String COUNT_ALL = "SELECT COUNT(*) FROM orders";

    private static final String UPDATE_STATUS = """
            UPDATE orders SET status = ?,
                completed_at = CASE WHEN ? = 'COMPLETED' THEN NOW() ELSE completed_at END
            WHERE id = ?
            """;

    private static final String ASSIGN_TRAINER = """
            UPDATE orders SET trainer_id = ?, status = 'ACTIVE' WHERE id = ?
            """;

    @Override
    public Order save(Order order) {
        Connection conn = pool.getConnection();
        // Manual transaction management for data integrity
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
                ps.setLong(1, order.getMemberId());
                ps.setInt(2, order.getCycleId());
                if (order.getTrainerId() != null) {
                    ps.setLong(3, order.getTrainerId());
                } else {
                    ps.setNull(3, Types.BIGINT);
                }
                ps.setBigDecimal(4, order.getPaidAmount());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        order.setId(rs.getLong("id"));
                        order.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    }
                }
            }
            conn.commit();
            log.info("Order saved with transaction: id={}", order.getId());
            return order;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { log.error("Rollback failed", ex); }
            log.error("Error saving order, transaction rolled back: {}", e.getMessage());
            throw new RuntimeException("Failed to save order", e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { log.error("AutoCommit reset failed", e); }
            pool.releaseConnection(conn);
        }
    }

    @Override
    public Optional<Order> findById(Long id) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding order {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to find order", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return Optional.empty();
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        Connection conn = pool.getConnection();
        List<Order> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_USER)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding orders for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to find orders", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return list;
    }

    @Override
    public List<Order> findByUserId(Long userId, int offset, int limit) {
        Connection conn = pool.getConnection();
        List<Order> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_USER_PAGED)) {
            ps.setLong(1, userId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding paged orders for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to find orders", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return list;
    }

    @Override
    public int countByUserId(Long userId) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(COUNT_BY_USER)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Error counting orders for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to count orders", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return 0;
    }

    @Override
    public int countCompletedByUserId(Long userId) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(COUNT_COMPLETED_BY_USER)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Error counting completed orders for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to count completed orders", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return 0;
    }

    @Override
    public List<Order> findActiveByTrainerId(Long trainerId) {
        Connection conn = pool.getConnection();
        List<Order> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_ACTIVE_BY_TRAINER)) {
            ps.setLong(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding orders for trainer {}: {}", trainerId, e.getMessage());
            throw new RuntimeException("Failed to find orders", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return list;
    }

    @Override
    public List<Order> findAll(int offset, int limit) {
        Connection conn = pool.getConnection();
        List<Order> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_ALL)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding all orders: {}", e.getMessage());
            throw new RuntimeException("Failed to find orders", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return list;
    }

    @Override
    public int countAll() {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(COUNT_ALL);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            log.error("Error counting orders: {}", e.getMessage());
            throw new RuntimeException("Failed to count orders", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return 0;
    }

    @Override
    public void updateStatus(Long orderId, String status) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS)) {
            ps.setString(1, status);
            ps.setString(2, status);
            ps.setLong(3, orderId);
            ps.executeUpdate();
            log.debug("Order {} status updated to {}", orderId, status);
        } catch (SQLException e) {
            log.error("Error updating order status: {}", e.getMessage());
            throw new RuntimeException("Failed to update order status", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    @Override
    public void assignTrainer(Long orderId, Long trainerId) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(ASSIGN_TRAINER)) {
            ps.setLong(1, trainerId);
            ps.setLong(2, orderId);
            ps.executeUpdate();
            log.info("Trainer {} assigned to order {}", trainerId, orderId);
        } catch (SQLException e) {
            log.error("Error assigning trainer: {}", e.getMessage());
            throw new RuntimeException("Failed to assign trainer", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        return Order.builder()
                .id(rs.getLong("id"))
                .memberId(rs.getLong("member_id"))
                .cycleId(rs.getInt("cycle_id"))
                .trainerId(rs.getObject("trainer_id") != null ? rs.getLong("trainer_id") : null)
                .status(rs.getString("status"))
                .paidAmount(rs.getBigDecimal("paid_amount"))
                .createdAt(rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toLocalDateTime() : null)
                .completedAt(rs.getTimestamp("completed_at") != null
                        ? rs.getTimestamp("completed_at").toLocalDateTime() : null)
                .clientName(rs.getString("client_name"))
                .cycleTitle(rs.getString("cycle_title"))
                .cycleTitleRu(rs.getString("cycle_title_ru"))
                .cycleTitleKz(rs.getString("cycle_title_kz"))
                .trainerName(rs.getString("trainer_name"))
                .build();
    }
}