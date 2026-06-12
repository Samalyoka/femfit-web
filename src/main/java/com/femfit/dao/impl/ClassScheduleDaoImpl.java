package com.femfit.dao.impl;

import com.femfit.dao.ClassScheduleDao;
import com.femfit.model.ClassSchedule;
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
 * JDBC implementation of {@link ClassScheduleDao}.
 */
@Repository
public class ClassScheduleDaoImpl implements ClassScheduleDao {

    private static final Logger log = LoggerFactory.getLogger(ClassScheduleDaoImpl.class);

    private final ConnectionPool pool;

    @Autowired
    public ClassScheduleDaoImpl(ConnectionPool pool) {
        this.pool = pool;
    }

    private static final String SELECT_BY_ID = """
            SELECT cs.id, cs.class_id, cs.trainer_id, cs.scheduled_at, cs.room, cs.is_cancelled,
                   fc.name AS class_name, fc.capacity, fc.duration_minutes,
                   u.first_name || ' ' || u.last_name AS trainer_name,
                   fc.category,
                   fc.capacity - COUNT(b.id) FILTER (WHERE b.status = 'CONFIRMED') AS spots_left
            FROM class_schedules cs
            JOIN fitness_classes fc ON cs.class_id = fc.id
            JOIN members u ON cs.trainer_id = u.id
            LEFT JOIN bookings b ON cs.id = b.schedule_id
            WHERE cs.id = ?
            GROUP BY cs.id, fc.name, fc.capacity, fc.duration_minutes, u.first_name, u.last_name, fc.category
            """;

    private static final String SELECT_UPCOMING = """
            SELECT cs.id, cs.class_id, cs.trainer_id, cs.scheduled_at, cs.room, cs.is_cancelled,
                   fc.name AS class_name, fc.capacity, fc.duration_minutes,
                   u.first_name || ' ' || u.last_name AS trainer_name,
                   fc.category,
                   fc.capacity - COUNT(b.id) FILTER (WHERE b.status = 'CONFIRMED') AS spots_left
            FROM class_schedules cs
            JOIN fitness_classes fc ON cs.class_id = fc.id
            JOIN members u ON cs.trainer_id = u.id
            LEFT JOIN bookings b ON cs.id = b.schedule_id
            WHERE cs.scheduled_at BETWEEN NOW() AND NOW() + INTERVAL '7 days'
              AND cs.is_cancelled = FALSE
            GROUP BY cs.id, fc.name, fc.capacity, fc.duration_minutes, u.first_name, u.last_name, fc.category
            ORDER BY cs.scheduled_at ASC
            """;

    private static final String SELECT_UPCOMING_BY_CATEGORY = """
            SELECT cs.id, cs.class_id, cs.trainer_id, cs.scheduled_at, cs.room, cs.is_cancelled,
                   fc.name AS class_name, fc.capacity, fc.duration_minutes,
                   u.first_name || ' ' || u.last_name AS trainer_name,
                   fc.category,
                   fc.capacity - COUNT(b.id) FILTER (WHERE b.status = 'CONFIRMED') AS spots_left
            FROM class_schedules cs
            JOIN fitness_classes fc ON cs.class_id = fc.id
            JOIN members u ON cs.trainer_id = u.id
            LEFT JOIN bookings b ON cs.id = b.schedule_id
            WHERE cs.scheduled_at BETWEEN NOW() AND NOW() + INTERVAL '7 days'
              AND cs.is_cancelled = FALSE
              AND fc.category = ?
            GROUP BY cs.id, fc.name, fc.capacity, fc.duration_minutes, u.first_name, u.last_name, fc.category
            ORDER BY cs.scheduled_at ASC
            """;

    @Override
    public Optional<ClassSchedule> findById(Long id) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding schedule {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to find schedule", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return Optional.empty();
    }

    @Override
    public List<ClassSchedule> findUpcoming() {
        Connection conn = pool.getConnection();
        List<ClassSchedule> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_UPCOMING);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error finding upcoming schedules: {}", e.getMessage());
            throw new RuntimeException("Failed to find schedules", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return list;
    }

    @Override
    public List<ClassSchedule> findUpcomingByCategory(String category) {
        Connection conn = pool.getConnection();
        List<ClassSchedule> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_UPCOMING_BY_CATEGORY)) {
            ps.setString(1, category.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding schedules by category {}: {}", category, e.getMessage());
            throw new RuntimeException("Failed to find schedules", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return list;
    }

    private ClassSchedule mapRow(ResultSet rs) throws SQLException {
        return ClassSchedule.builder()
                .id(rs.getLong("id"))
                .classId(rs.getInt("class_id"))
                .trainerId(rs.getLong("trainer_id"))
                .scheduledAt(rs.getTimestamp("scheduled_at").toLocalDateTime())
                .room(rs.getString("room"))
                .cancelled(rs.getBoolean("is_cancelled"))
                .capacity(rs.getInt("capacity"))
                .className(rs.getString("class_name"))
                .trainerName(rs.getString("trainer_name"))
                .category(rs.getString("category"))
                .spotsLeft(rs.getInt("spots_left"))
                .durationMinutes(rs.getInt("duration_minutes"))
                .build();
    }
}