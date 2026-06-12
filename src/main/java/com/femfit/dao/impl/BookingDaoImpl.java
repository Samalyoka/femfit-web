package com.femfit.dao.impl;

import com.femfit.dao.BookingDao;
import com.femfit.model.Booking;
import com.femfit.util.pool.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link BookingDao}.
 * Uses PreparedStatements exclusively — no string concatenation in SQL.
 */
@Repository
public class BookingDaoImpl implements BookingDao {

    private static final Logger log = LoggerFactory.getLogger(BookingDaoImpl.class);

    private final ConnectionPool pool;

    @Autowired
    public BookingDaoImpl(ConnectionPool pool) {
        this.pool = pool;
    }

    private static final String INSERT = """
            INSERT INTO bookings (user_id, schedule_id, booked_at, status)
            VALUES (?, ?, NOW(), 'CONFIRMED')
            RETURNING id, booked_at
            """;

    private static final String SELECT_BY_ID = """
            SELECT b.id, b.user_id, b.schedule_id, b.booked_at, b.status,
                   fc.name AS class_name,
                   u.first_name || ' ' || u.last_name AS trainer_name,
                   cs.scheduled_at, cs.room
            FROM bookings b
            JOIN class_schedules cs ON b.schedule_id = cs.id
            JOIN fitness_classes fc ON cs.class_id = fc.id
            JOIN trainers t ON cs.trainer_id = t.id
            JOIN members u ON t.id = u.id
            WHERE b.id = ?
            """;

    private static final String SELECT_UPCOMING_BY_USER = """
            SELECT b.id, b.user_id, b.schedule_id, b.booked_at, b.status,
                   fc.name AS class_name,
                   u.first_name || ' ' || u.last_name AS trainer_name,
                   cs.scheduled_at, cs.room
            FROM bookings b
            JOIN class_schedules cs ON b.schedule_id = cs.id
            JOIN fitness_classes fc ON cs.class_id = fc.id
            JOIN trainers t ON cs.trainer_id = t.id
            JOIN members u ON t.id = u.id
            WHERE b.user_id = ?
              AND b.status = 'CONFIRMED'
              AND cs.scheduled_at > NOW()
            ORDER BY cs.scheduled_at ASC
            """;

    private static final String SELECT_BY_SCHEDULE = """
            SELECT b.id, b.user_id, b.schedule_id, b.booked_at, b.status,
                   fc.name AS class_name, cs.scheduled_at, cs.room,
                   u.first_name || ' ' || u.last_name AS trainer_name
            FROM bookings b
            JOIN class_schedules cs ON b.schedule_id = cs.id
            JOIN fitness_classes fc ON cs.class_id = fc.id
            JOIN trainers t ON cs.trainer_id = t.id
            JOIN members u ON t.id = u.id
            WHERE b.schedule_id = ?
            """;

    private static final String COUNT_CONFIRMED = """
            SELECT COUNT(*) FROM bookings
            WHERE schedule_id = ? AND status = 'CONFIRMED'
            """;

    private static final String EXISTS_BY_USER_SCHEDULE = """
            SELECT EXISTS(
                SELECT 1 FROM bookings
                WHERE user_id = ? AND schedule_id = ? AND status = 'CONFIRMED'
            )
            """;

    private static final String UPDATE_STATUS = """
            UPDATE bookings SET status = ? WHERE id = ?
            """;

    private static final String CANCEL = """
            UPDATE bookings SET status = 'CANCELLED'
            WHERE id = ? AND user_id = ?
            """;

    @Override
    public Booking save(Booking booking) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setLong(1, booking.getUserId());
            ps.setLong(2, booking.getScheduleId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    booking.setId(rs.getLong("id"));
                    booking.setBookedAt(rs.getTimestamp("booked_at").toLocalDateTime());
                }
            }
            log.debug("Booking saved: id={}", booking.getId());
            return booking;
        } catch (SQLException e) {
            log.error("Error saving booking: {}", e.getMessage());
            throw new RuntimeException("Failed to save booking", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    @Override
    public Optional<Booking> findById(Long id) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding booking {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to find booking", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return Optional.empty();
    }

    @Override
    public List<Booking> findUpcomingByUserId(Long userId) {
        Connection conn = pool.getConnection();
        List<Booking> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_UPCOMING_BY_USER)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding upcoming bookings for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to find bookings", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return list;
    }

    @Override
    public List<Booking> findByScheduleId(Long scheduleId) {
        Connection conn = pool.getConnection();
        List<Booking> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_SCHEDULE)) {
            ps.setLong(1, scheduleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error finding bookings for schedule {}: {}", scheduleId, e.getMessage());
            throw new RuntimeException("Failed to find bookings", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return list;
    }

    @Override
    public int countConfirmedByScheduleId(Long scheduleId) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(COUNT_CONFIRMED)) {
            ps.setLong(1, scheduleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Error counting bookings: {}", e.getMessage());
            throw new RuntimeException("Failed to count bookings", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return 0;
    }

    @Override
    public boolean existsByUserAndSchedule(Long userId, Long scheduleId) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(EXISTS_BY_USER_SCHEDULE)) {
            ps.setLong(1, userId);
            ps.setLong(2, scheduleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            log.error("Error checking booking existence: {}", e.getMessage());
            throw new RuntimeException("Failed to check booking", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    @Override
    public void updateStatus(Long bookingId, String status) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS)) {
            ps.setString(1, status);
            ps.setLong(2, bookingId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error updating booking status: {}", e.getMessage());
            throw new RuntimeException("Failed to update booking status", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    @Override
    public void cancel(Long bookingId, Long userId) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(CANCEL)) {
            ps.setLong(1, bookingId);
            ps.setLong(2, userId);
            ps.executeUpdate();
            log.debug("Booking {} cancelled by user {}", bookingId, userId);
        } catch (SQLException e) {
            log.error("Error cancelling booking {}: {}", bookingId, e.getMessage());
            throw new RuntimeException("Failed to cancel booking", e);
        } finally {
            pool.releaseConnection(conn);
        }
    }

    private Booking mapRow(ResultSet rs) throws SQLException {
        return Booking.builder()
                .id(rs.getLong("id"))
                .userId(rs.getLong("user_id"))
                .scheduleId(rs.getLong("schedule_id"))
                .bookedAt(rs.getTimestamp("booked_at") != null
                        ? rs.getTimestamp("booked_at").toLocalDateTime() : null)
                .status(rs.getString("status"))
                .className(rs.getString("class_name"))
                .trainerName(rs.getString("trainer_name"))
                .scheduledAt(rs.getTimestamp("scheduled_at") != null
                        ? rs.getTimestamp("scheduled_at").toLocalDateTime() : null)
                .room(rs.getString("room"))
                .build();
    }
}