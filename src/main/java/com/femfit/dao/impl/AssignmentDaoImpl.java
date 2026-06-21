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

/**
 * JDBC implementation of {@link AssignmentDao}.
 *
 * Manages training assignments created by trainers for clients' orders.
 * Each assignment contains workout routines, equipment needs, nutrition plans, and schedules.
 * Uses manual transaction management for data integrity.
 */
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
                   schedule_info, status,
                   revision_exercises_requested, revision_equipment_requested,
                   revision_nutrition_requested, revision_schedule_requested,
                   revision_comment,
                   created_at, updated_at
            FROM assignments
            WHERE order_id = ?
            """;

    private static final String SELECT_LATEST_BY_CLIENT = """
            SELECT a.id, a.order_id, a.exercises, a.equipment, a.nutrition_plan,
                   a.schedule_info, a.status,
                   a.revision_exercises_requested, a.revision_equipment_requested,
                   a.revision_nutrition_requested, a.revision_schedule_requested,
                   a.revision_comment,
                   a.created_at, a.updated_at
            FROM assignments a
            JOIN orders o ON o.id = a.order_id
            WHERE o.member_id = ?
            ORDER BY a.updated_at DESC
            LIMIT 1
            """;

    private static final String UPDATE = """
            UPDATE assignments
               SET exercises = ?, equipment = ?, nutrition_plan = ?,
                   schedule_info = ?, status = 'ACTIVE',
                   revision_exercises_requested = FALSE,
                   revision_equipment_requested = FALSE,
                   revision_nutrition_requested = FALSE,
                   revision_schedule_requested = FALSE,
                   revision_comment = NULL,
                   updated_at = NOW()
             WHERE id = ?
            """;

    private static final String UPDATE_STATUS = """
            UPDATE assignments
               SET status = ?, updated_at = NOW()
             WHERE id = ?
            """;

    private static final String REQUEST_REVISION = """
            UPDATE assignments
               SET status = 'REVISION_REQUESTED',
                   revision_exercises_requested = ?,
                   revision_equipment_requested = ?,
                   revision_nutrition_requested = ?,
                   revision_schedule_requested = ?,
                   revision_comment = ?,
                   updated_at = NOW()
             WHERE id = ?
            """;

    private static final String DELETE_BY_ORDER = """
            DELETE FROM assignments WHERE order_id = ?
            """;

    /**
     * Saves a new assignment to the database with transaction support.
     * Sets the assignment ID and timestamps from the database response.
     *
     * @param assignment the assignment to save (orderId and content fields must be set)
     * @return the saved assignment with ID and timestamps populated
     * @throws RuntimeException if the save operation fails
     */
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

    /**
     * Finds an assignment by order ID.
     *
     * @param orderId the order ID
     * @return Optional containing the assignment if found, empty otherwise
     * @throws RuntimeException if the query fails
     */
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

    /**
     * Finds the most recently updated assignment for a client.
     * Used to display the latest training plan to the client.
     *
     * @param clientId the member/client ID
     * @return Optional containing the latest assignment if found, empty otherwise
     * @throws RuntimeException if the query fails
     */
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

    /**
     * Updates an assignment's content (exercises, equipment, nutrition plan, schedule).
     * Sets the updated_at timestamp to current time.
     *
     * @param assignment the assignment with updated content (id must be set)
     * @throws RuntimeException if the update fails
     */
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

    /**
     * Updates the status of an assignment (e.g., ACTIVE → COMPLETED or REVISION_REQUESTED).
     * Sets the updated_at timestamp to current time.
     *
     * @param assignmentId the assignment ID
     * @param status the new status (e.g., 'COMPLETED', 'REVISION_REQUESTED')
     * @throws RuntimeException if the update fails
     */
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

    /**
     * Records a per-item revision request and sets status to REVISION_REQUESTED.
     *
     * @param assignmentId the assignment id
     * @param exercises    true if exercises need revision
     * @param equipment    true if equipment needs revision
     * @param nutrition    true if nutrition plan needs revision
     * @param schedule     true if schedule needs revision
     * @param comment      optional client comment
     * @throws RuntimeException if the update fails
     */
    @Override
    public void requestRevision(Long assignmentId, boolean exercises, boolean equipment,
                                 boolean nutrition, boolean schedule, String comment) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(REQUEST_REVISION)) {
            ps.setBoolean(1, exercises);
            ps.setBoolean(2, equipment);
            ps.setBoolean(3, nutrition);
            ps.setBoolean(4, schedule);
            if (comment != null) {
                ps.setString(5, comment);
            } else {
                ps.setNull(5, Types.VARCHAR);
            }
            ps.setLong(6, assignmentId);
            ps.executeUpdate();
            log.info("Revision requested for assignment id={}: exercises={}, equipment={}, nutrition={}, schedule={}",
                    assignmentId, exercises, equipment, nutrition, schedule);
        } catch (SQLException e) {
            log.error("Error requesting revision for assignment id={}", assignmentId, e);
            throw new RuntimeException("Failed to request revision", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    /**
     * Deletes all assignments for a given order.
     * Typically used when an order is cancelled.
     *
     * @param orderId the order ID
     * @throws RuntimeException if the delete fails
     */
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

    /**
     * Maps a ResultSet row to an {@link Assignment} object.
     *
     * @param rs the result set positioned at the current row
     * @return a populated Assignment object
     * @throws SQLException if a column cannot be read
     */
    private Assignment mapRow(ResultSet rs) throws SQLException {
        return Assignment.builder()
                .id(rs.getLong("id"))
                .orderId(rs.getLong("order_id"))
                .exercises(rs.getString("exercises"))
                .equipment(rs.getString("equipment"))
                .nutritionPlan(rs.getString("nutrition_plan"))
                .scheduleInfo(rs.getString("schedule_info"))
                .status(rs.getString("status"))
                .revisionExercisesRequested(rs.getBoolean("revision_exercises_requested"))
                .revisionEquipmentRequested(rs.getBoolean("revision_equipment_requested"))
                .revisionNutritionRequested(rs.getBoolean("revision_nutrition_requested"))
                .revisionScheduleRequested(rs.getBoolean("revision_schedule_requested"))
                .revisionComment(rs.getString("revision_comment"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                .build();
    }
}