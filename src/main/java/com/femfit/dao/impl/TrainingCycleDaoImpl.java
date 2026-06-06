package com.femfit.dao.impl;

import com.femfit.dao.TrainingCycleDao;
import com.femfit.model.TrainingCycle;
import com.femfit.util.pool.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class TrainingCycleDaoImpl implements TrainingCycleDao {

    private static final Logger log = LoggerFactory.getLogger(TrainingCycleDaoImpl.class);
    private final ConnectionPool pool;

    @Autowired
    public TrainingCycleDaoImpl(ConnectionPool pool) {
        this.pool = pool;
    }

    private static final String SELECT_ALL_ACTIVE = """
            SELECT id, title, description, duration_weeks, price, is_active, created_at
            FROM training_cycles
            WHERE is_active = true
            ORDER BY price
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, title, description, duration_weeks, price, is_active, created_at
            FROM training_cycles
            WHERE id = ?
            """;

    @Override
    public List<TrainingCycle> findAllActive() {
        Connection conn = pool.getConnection();
        List<TrainingCycle> cycles = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_ALL_ACTIVE);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cycles.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error fetching training cycles: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch training cycles", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return cycles;
    }

    @Override
    public Optional<TrainingCycle> findById(Integer id) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error fetching training cycle {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to fetch training cycle", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return Optional.empty();
    }

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
                .build();
    }
}