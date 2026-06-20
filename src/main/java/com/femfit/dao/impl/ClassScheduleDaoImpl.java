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
 *
 * <p>Reads from {@code class_occurrences} (the bookable, dated sessions)
 * joined back to their {@code class_schedules} recurring template to get
 * the day/time, room, class, and trainer info. {@code scheduled_at} is
 * computed in SQL as {@code occurrence_date + start_time} so the rest of
 * the application (bookings, the schedule page) keeps working with a single
 * timestamp exactly as before migration v4 — only the source of that
 * timestamp changed, from a stored column to a computed one.</p>
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
            SELECT co.id, cs.class_id, cs.trainer_id,
                   (co.occurrence_date + cs.start_time) AS scheduled_at,
                   cs.room, co.is_cancelled,
                   fc.name AS class_name, fc.name_ru AS class_name_ru, fc.name_kz AS class_name_kz,
                   fc.capacity, fc.duration_minutes,
                   u.first_name || ' ' || u.last_name AS trainer_name,
                   fc.category, fc.difficulty_level,
                   fc.capacity - COUNT(b.id) FILTER (WHERE b.status = 'CONFIRMED') AS spots_left
            FROM class_occurrences co
            JOIN class_schedules cs ON co.schedule_id = cs.id
            JOIN fitness_classes fc ON cs.class_id = fc.id
            JOIN members u ON cs.trainer_id = u.id
            LEFT JOIN bookings b ON co.id = b.schedule_id
            WHERE co.id = ?
            GROUP BY co.id, cs.class_id, cs.trainer_id, cs.start_time, cs.room,
                     fc.name, fc.name_ru, fc.name_kz, fc.capacity, fc.duration_minutes,
                     u.first_name, u.last_name, fc.category, fc.difficulty_level
            """;

    private static final String SELECT_UPCOMING = """
            SELECT co.id, cs.class_id, cs.trainer_id,
                   (co.occurrence_date + cs.start_time) AS scheduled_at,
                   cs.room, co.is_cancelled,
                   fc.name AS class_name, fc.name_ru AS class_name_ru, fc.name_kz AS class_name_kz,
                   fc.capacity, fc.duration_minutes,
                   u.first_name || ' ' || u.last_name AS trainer_name,
                   fc.category, fc.difficulty_level,
                   fc.capacity - COUNT(b.id) FILTER (WHERE b.status = 'CONFIRMED') AS spots_left
            FROM class_occurrences co
            JOIN class_schedules cs ON co.schedule_id = cs.id
            JOIN fitness_classes fc ON cs.class_id = fc.id
            JOIN members u ON cs.trainer_id = u.id
            LEFT JOIN bookings b ON co.id = b.schedule_id
            WHERE (co.occurrence_date + cs.start_time) BETWEEN NOW() AND NOW() + INTERVAL '7 days'
              AND co.is_cancelled = FALSE
              AND cs.is_active = TRUE
            GROUP BY co.id, cs.class_id, cs.trainer_id, cs.start_time, cs.room,
                     fc.name, fc.name_ru, fc.name_kz, fc.capacity, fc.duration_minutes,
                     u.first_name, u.last_name, fc.category, fc.difficulty_level
            ORDER BY scheduled_at ASC
            """;

    private static final String SELECT_UPCOMING_BY_CATEGORY = """
            SELECT co.id, cs.class_id, cs.trainer_id,
                   (co.occurrence_date + cs.start_time) AS scheduled_at,
                   cs.room, co.is_cancelled,
                   fc.name AS class_name, fc.name_ru AS class_name_ru, fc.name_kz AS class_name_kz,
                   fc.capacity, fc.duration_minutes,
                   u.first_name || ' ' || u.last_name AS trainer_name,
                   fc.category, fc.difficulty_level,
                   fc.capacity - COUNT(b.id) FILTER (WHERE b.status = 'CONFIRMED') AS spots_left
            FROM class_occurrences co
            JOIN class_schedules cs ON co.schedule_id = cs.id
            JOIN fitness_classes fc ON cs.class_id = fc.id
            JOIN members u ON cs.trainer_id = u.id
            LEFT JOIN bookings b ON co.id = b.schedule_id
            WHERE (co.occurrence_date + cs.start_time) BETWEEN NOW() AND NOW() + INTERVAL '7 days'
              AND co.is_cancelled = FALSE
              AND cs.is_active = TRUE
              AND fc.category = ?
            GROUP BY co.id, cs.class_id, cs.trainer_id, cs.start_time, cs.room,
                     fc.name, fc.name_ru, fc.name_kz, fc.capacity, fc.duration_minutes,
                     u.first_name, u.last_name, fc.category, fc.difficulty_level
            ORDER BY scheduled_at ASC
            """;

    /**
     * Generates occurrences for the next N weeks from today, for every
     * active recurring template. Idempotent — relies on the (schedule_id,
     * occurrence_date) UNIQUE constraint, so calling this repeatedly (e.g.
     * on every app startup) never creates duplicates. This is what keeps
     * the schedule page from ever running dry again.
     */
    private static final String GENERATE_UPCOMING_OCCURRENCES = """
            INSERT INTO class_occurrences (schedule_id, occurrence_date)
            SELECT cs.id, d::date
            FROM class_schedules cs
            CROSS JOIN generate_series(CURRENT_DATE, CURRENT_DATE + (? || ' days')::interval, INTERVAL '1 day') AS d
            WHERE cs.is_active = TRUE
              AND EXTRACT(ISODOW FROM d)::int = cs.day_of_week
            ON CONFLICT (schedule_id, occurrence_date) DO NOTHING
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

    /**
     * Generates occurrences for the next {@code weeksAhead} weeks from today
     * for every active recurring template. Safe to call on every application
     * startup or from a scheduled job — duplicates are silently skipped.
     *
     * @param weeksAhead how many weeks ahead to ensure occurrences exist for
     */
    @Override
    public void generateUpcomingOccurrences(int weeksAhead) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(GENERATE_UPCOMING_OCCURRENCES)) {
            ps.setInt(1, weeksAhead * 7);
            int inserted = ps.executeUpdate();
            log.info("Generated {} new class occurrence(s) for the next {} week(s)", inserted, weeksAhead);
        } catch (SQLException e) {
            log.error("Error generating upcoming occurrences: {}", e.getMessage());
            throw new RuntimeException("Failed to generate occurrences", e);
        } finally {
            pool.releaseConnection(conn);
        }
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
                .classNameRu(rs.getString("class_name_ru"))
                .classNameKz(rs.getString("class_name_kz"))
                .trainerName(rs.getString("trainer_name"))
                .category(rs.getString("category"))
                .difficultyLevel(rs.getString("difficulty_level"))
                .spotsLeft(rs.getInt("spots_left"))
                .durationMinutes(rs.getInt("duration_minutes"))
                .build();
    }
}