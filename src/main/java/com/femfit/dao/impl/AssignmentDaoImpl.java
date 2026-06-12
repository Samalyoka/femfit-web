package com.femfit.dao.impl;

import com.femfit.dao.AssignmentDao;
import com.femfit.model.Assignment;
import com.femfit.datasource.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.Optional;

@Repository
public class AssignmentDaoImpl implements AssignmentDao {

    private static final Logger log = LoggerFactory.getLogger(AssignmentDaoImpl.class);

    private final ConnectionPool pool;

    @Autowired
    public AssignmentDaoImpl(ConnectionPool pool) {
        this.pool = pool;
    }

    private static final String INSERT = """
            INSERT INTO assignments (order_id, exercises, equipment, nutrition_plan,
                                     schedule_info, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, 'ACTIVE', NOW(), NOW())
            RETURNING id, created_at, updated_at
            """;

    private static final String SELECT_BY_ORDER = """
            SELECT id, order_id, exercises, equipment, nutrition_plan,
                   schedule_info, status, created_at, updated_at
            FROM assignments
            WHERE order_id = ?
            """;

    private static final String SELECT_LATEST_BY_CLIENT = """
            SELECT a.id, a.order_id, a.exercises, a.equipment, a.nutrition_plan,
                   a.schedule_info, a.status, a.created_at, a.updated_at
            FROM assignments a
            JOIN orders o ON o.id = a.order_id
            WHERE o.user_id = ?
            ORDER BY a.updated_at DESC
            LIMIT 1
            """;

    private static final String UPDATE = """
            UPDATE assignments
               SET exercises = ?, equipment = ?, nutrition_plan = ?,
                   schedule_info = ?, updated_at = NOW()
             WHERE id = ?
            """;

    private static final String UPDATE_STATUS = """
            UPDATE assignments
               SET status = ?, updated_at = NOW()
             WHERE id = ?
            """;

    private static final String DELETE_BY_ORDER = """
            DELETE FROM assignments WHERE order_id = ?
            """;

    @Override
    public Assignment save(Assignment assignment) {
        Connection conn = pool.getConnection();
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
                ps.setLong(1, assignment.getOrderId());
                ps.setString(2, assignment.getExercises());
                ps.setString(3, assignment.getEquipment());
                ps.setString(4, assignment.getNutritionPlan());
                ps.setString(5, assignment.getScheduleInfo());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        assignment.setId(rs.getLong("id"));
                        assignment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                        assignment.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                    }
                }
            }
            conn.commit();
            log.info("Assignment saved: id={}", assignment.getId());
            return assignment;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { log.error("Rollback failed", ex); }
            log.error("Error saving assignment", e);
            throw new RuntimeException("Failed to save assignment", e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { log.error("AutoCommit reset failed", e); }
            pool.releaseConnection(conn);
        }
    }

    @Override
    public Optional<Assignment> findByOrderId(Long orderId) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_ORDER)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding assignment for order {}", orderId, e);
            throw new RuntimeException("Failed to find assignment by orderId", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Assignment> findLatestByClientId(long clientId) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_LATEST_BY_CLIENT)) {
            ps.setLong(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding assignment for client {}", clientId, e);
            throw new RuntimeException("Failed to find assignment by clientId", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return Optional.empty();
    }

    @Override
    public void update(Assignment assignment) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setString(1, assignment.getExercises());
            ps.setString(2, assignment.getEquipment());
            ps.setString(3, assignment.getNutritionPlan());
            ps.setString(4, assignment.getScheduleInfo());
            ps.setLong(5, assignment.getId());
            ps.executeUpdate();
            log.debug("Assignment updated: id={}", assignment.getId());
        } catch (SQLException e) {
            log.error("Error updating assignment id={}", assignment.getId(), e);
            throw new RuntimeException("Failed to update assignment", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    @Override
    public void updateStatus(Long assignmentId, String status) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS)) {
            ps.setString(1, status);
            ps.setLong(2, assignmentId);
            ps.executeUpdate();
            log.debug("Assignment status updated: id={}, status={}", assignmentId, status);
        } catch (SQLException e) {
            log.error("Error updating assignment status id={}", assignmentId, e);
            throw new RuntimeException("Failed to update assignment status", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    @Override
    public void deleteByOrderId(Long orderId) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(DELETE_BY_ORDER)) {
            ps.setLong(1, orderId);
            int rows = ps.executeUpdate();
            log.debug("Deleted {} assignment(s) for order {}", rows, orderId);
        } catch (SQLException e) {
            log.error("Error deleting assignments for order {}", orderId, e);
            throw new RuntimeException("Failed to delete assignments", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    private Assignment mapRow(ResultSet rs) throws SQLException {
        return Assignment.builder()
                .id(rs.getLong("id"))
                .orderId(rs.getLong("order_id"))
                .exercises(rs.getString("exercises"))
                .equipment(rs.getString("equipment"))
                .nutritionPlan(rs.getString("nutrition_plan"))
                .scheduleInfo(rs.getString("schedule_info"))
                .status(rs.getString("status"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                .build();
    }
}