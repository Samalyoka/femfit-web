package com.femfit.dao.impl;

import com.femfit.dao.TrainingCycleDao;
import com.femfit.datasource.ConnectionPool;
import com.femfit.model.TrainingCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link TrainingCycleDao}.
 * Uses PreparedStatements to prevent SQL injection.
 */
@Repository
public class TrainingCycleDaoImpl implements TrainingCycleDao {

    private static final Logger log = LoggerFactory.getLogger(TrainingCycleDaoImpl.class);

    private final ConnectionPool pool;

    @Autowired
    public TrainingCycleDaoImpl(ConnectionPool pool) {
        this.pool = pool;
    }

    private static final String SELECT_ALL = """
            SELECT id, title, description, duration_weeks, price, is_active, created_at, photo_url,
                   title_ru, title_kz, description_ru, description_kz
            FROM training_cycles
            ORDER BY created_at DESC
            """;

    private static final String SELECT_ACTIVE = """
            SELECT id, title, description, duration_weeks, price, is_active, created_at, photo_url,
                   title_ru, title_kz, description_ru, description_kz
            FROM training_cycles
            WHERE is_active = true
            ORDER BY created_at DESC
            """;

    private static final String SELECT_ACTIVE_PAGED = """
            SELECT id, title, description, duration_weeks, price, is_active, created_at, photo_url,
                   title_ru, title_kz, description_ru, description_kz
            FROM training_cycles
            WHERE is_active = true
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, title, description, duration_weeks, price, is_active, created_at, photo_url,
                   title_ru, title_kz, description_ru, description_kz
            FROM training_cycles
            WHERE id = ?
            """;

    private static final String INSERT = """
            INSERT INTO training_cycles (title, description, duration_weeks, price, is_active, created_at)
            VALUES (?, ?, ?, ?, true, NOW())
            RETURNING id, created_at
            """;

    private static final String UPDATE = """
            UPDATE training_cycles
            SET title = ?, description = ?, duration_weeks = ?, price = ?
            WHERE id = ?
            """;

    private static final String SET_ACTIVE = """
            UPDATE training_cycles SET is_active = ? WHERE id = ?
            """;

    private static final String COUNT_ACTIVE = """
            SELECT COUNT(*) FROM training_cycles WHERE is_active = true
            """;

    private static final String COUNT_ALL = """
            SELECT COUNT(*) FROM training_cycles
            """;

    /**
     * Returns all training cycles ordered by creation date descending.
     */
    @Override
    public List<TrainingCycle> findAll() {
        Connection conn = pool.getConnection();
        List<TrainingCycle> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error fetching all training cycles: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch training cycles", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return list;
    }

    /**
     * Returns only active training cycles.
     */
    @Override
    public List<TrainingCycle> findAllActive() {
        Connection conn = pool.getConnection();
        List<TrainingCycle> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_ACTIVE);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error fetching active training cycles: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch training cycles", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return list;
    }

    @Override
    public List<TrainingCycle> findAllActive(int offset, int limit) {
        Connection conn = pool.getConnection();
        List<TrainingCycle> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_ACTIVE_PAGED)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error fetching paged active training cycles: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch training cycles", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return list;
    }

    /**
     * Finds a training cycle by ID.
     */
    @Override
    public Optional<TrainingCycle> findById(Integer id) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error fetching training cycle id={}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to fetch training cycle", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return Optional.empty();
    }

    /**
     * Creates a new training cycle. Sets id and createdAt from DB.
     */
    @Override
    public TrainingCycle save(TrainingCycle cycle) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1, cycle.getTitle());
            ps.setString(2, cycle.getDescription());
            ps.setInt(3, cycle.getDurationWeeks());
            ps.setBigDecimal(4, cycle.getPrice());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    cycle.setId(rs.getInt("id"));
                    cycle.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                }
            }
            log.info("Training cycle created: id={}, title={}", cycle.getId(), cycle.getTitle());
            return cycle;
        } catch (SQLException e) {
            log.error("Error creating training cycle: {}", e.getMessage());
            throw new RuntimeException("Failed to create training cycle", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    /**
     * Updates title, description, duration and price of an existing cycle.
     */
    @Override
    public void update(TrainingCycle cycle) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setString(1, cycle.getTitle());
            ps.setString(2, cycle.getDescription());
            ps.setInt(3, cycle.getDurationWeeks());
            ps.setBigDecimal(4, cycle.getPrice());
            ps.setInt(5, cycle.getId());
            ps.executeUpdate();
            log.info("Training cycle updated: id={}", cycle.getId());
        } catch (SQLException e) {
            log.error("Error updating training cycle id={}: {}", cycle.getId(), e.getMessage());
            throw new RuntimeException("Failed to update training cycle", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    /**
     * Activates or deactivates a training cycle.
     */
    @Override
    public void setActive(Integer id, boolean active) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SET_ACTIVE)) {
            ps.setBoolean(1, active);
            ps.setInt(2, id);
            ps.executeUpdate();
            log.info("Training cycle id={} active={}", id, active);
        } catch (SQLException e) {
            log.error("Error setting active status for cycle id={}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to update training cycle status", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    /**
     * Counts active training cycles.
     */
    @Override
    public int countActive() {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(COUNT_ACTIVE);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            log.error("Error counting active cycles: {}", e.getMessage());
            throw new RuntimeException("Failed to count cycles", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    /**
     * Counts all training cycles.
     */
    @Override
    public int countAll() {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(COUNT_ALL);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            log.error("Error counting all cycles: {}", e.getMessage());
            throw new RuntimeException("Failed to count cycles", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    /**
     * Maps a ResultSet row to a {@link TrainingCycle} object.
     */
    private TrainingCycle mapRow(ResultSet rs) throws SQLException {
        return TrainingCycle.builder()
                .id(rs.getInt("id"))
                .title(rs.getString("title"))
                .description(rs.getString("description"))
                .durationWeeks(rs.getInt("duration_weeks"))
                .price(rs.getBigDecimal("price"))
                .active(rs.getBoolean("is_active"))
                .createdAt(rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toLocalDateTime() : null)
                .photoUrl(rs.getString("photo_url"))
                .titleRu(rs.getString("title_ru"))
                .titleKz(rs.getString("title_kz"))
                .descriptionRu(rs.getString("description_ru"))
                .descriptionKz(rs.getString("description_kz"))
                .build();
    }
}