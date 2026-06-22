package com.femfit.dao.impl;

import com.femfit.dao.BookingDao;
import com.femfit.exception.BookingException;
import com.femfit.model.Booking;
import com.femfit.datasource.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link BookingDao}.
 * Uses PreparedStatements exclusively — no string concatenation in SQL.
 *
 * Provides booking management including:
 * - Creating bookings with pessimistic locking to prevent overbooking
 * - Finding bookings by various criteria
 * - Updating and cancelling bookings
 * - Checking booking status and capacity
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
            INSERT INTO bookings (member_id, schedule_id, booked_at, status)
            VALUES (?, ?, NOW(), 'CONFIRMED')
            RETURNING id, booked_at
            """;

    private static final String SELECT_BY_ID = """
            SELECT b.id, b.member_id, b.schedule_id, b.booked_at, b.status,
                   fc.name AS class_name,
                   u.first_name || ' ' || u.last_name AS trainer_name,
                   (co.occurrence_date + cs.start_time) AS scheduled_at, cs.room
            FROM bookings b
            JOIN class_occurrences co ON b.schedule_id = co.id
            JOIN class_schedules cs ON co.schedule_id = cs.id
            JOIN fitness_classes fc ON cs.class_id = fc.id
            JOIN trainers t ON cs.trainer_id = t.id
            JOIN members u ON t.id = u.id
            WHERE b.id = ?
            """;

    private static final String SELECT_UPCOMING_BY_USER = """
            SELECT b.id, b.member_id, b.schedule_id, b.booked_at, b.status,
                   fc.name AS class_name,
                   u.first_name || ' ' || u.last_name AS trainer_name,
                   (co.occurrence_date + cs.start_time) AS scheduled_at, cs.room
            FROM bookings b
            JOIN class_occurrences co ON b.schedule_id = co.id
            JOIN class_schedules cs ON co.schedule_id = cs.id
            JOIN fitness_classes fc ON cs.class_id = fc.id
            JOIN trainers t ON cs.trainer_id = t.id
            JOIN members u ON t.id = u.id
            WHERE b.member_id = ?
              AND b.status = 'CONFIRMED'
              AND (co.occurrence_date + cs.start_time) > NOW()
            ORDER BY scheduled_at ASC
            """;

    private static final String SELECT_BY_SCHEDULE = """
            SELECT b.id, b.member_id, b.schedule_id, b.booked_at, b.status,
                   fc.name AS class_name, (co.occurrence_date + cs.start_time) AS scheduled_at, cs.room,
                   u.first_name || ' ' || u.last_name AS trainer_name
            FROM bookings b
            JOIN class_occurrences co ON b.schedule_id = co.id
            JOIN class_schedules cs ON co.schedule_id = cs.id
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
                WHERE member_id = ? AND schedule_id = ? AND status = 'CONFIRMED'
            )
            """;

    private static final String UPDATE_STATUS = """
            UPDATE bookings SET status = ? WHERE id = ?
            """;

    private static final String CANCEL = """
            UPDATE bookings SET status = 'CANCELLED'
            WHERE id = ? AND member_id = ?
            """;

    /**
     * Locks the class_occurrences row for the given dated session and
     * returns the class capacity (from fitness_classes via the recurring
     * template). FOR UPDATE OF co ensures concurrent bookWithLock() calls
     * for the same occurrence (scheduleId, which is really an occurrence
     * id post-migration-v4) are serialized — the second caller blocks
     * until the first commits or rolls back.
     *
     * <p>Locking the occurrence (not the template) is essential: the same
     * template row backs every future week's session, so locking the
     * template would serialize bookings across ALL weeks of that class,
     * not just the one being booked.</p>
     */
    private static final String LOCK_SCHEDULE_AND_GET_CAPACITY = """
            SELECT fc.capacity
            FROM class_occurrences co
            JOIN class_schedules cs ON co.schedule_id = cs.id
            JOIN fitness_classes fc ON cs.class_id = fc.id
            WHERE co.id = ?
            FOR UPDATE OF co
            """;

    /**
     * Saves a new booking to the database.
     * Sets the booking ID and booked timestamp from the database response.
     *
     * @param booking the booking to save (memberId and scheduleId must be set)
     * @return the saved booking with ID and timestamp populated
     * @throws RuntimeException if the save operation fails
     */
    @Override
    public Booking save(Booking booking) {
        Connection conn = pool.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setLong(1, booking.getMemberId());
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

    /**
     * Books a class with pessimistic locking to prevent race conditions and overbooking.
     *
     * Transaction flow:
     * 1. Lock the schedule row (FOR UPDATE) — serializes concurrent bookings for same schedule
     * 2. Check if user already booked this class
     * 3. Check if class is still available (below capacity)
     * 4. Insert the booking
     * 5. Commit
     *
     * @param userId the member ID
     * @param scheduleId the class schedule ID
     * @return the created booking with ID and timestamp
     * @throws BookingException if user already booked, class is full, or schedule not found
     * @throws RuntimeException if the booking operation fails
     */
    @Override
    public Booking bookWithLock(Long userId, Long scheduleId) {
        Connection conn = pool.getConnection();
        try {
            conn.setAutoCommit(false);

            // 1. Lock the schedule row and read capacity.
            //    Any other transaction calling bookWithLock() for the same
            //    scheduleId will block here until this transaction commits/rolls back.
            int capacity;
            try (PreparedStatement ps = conn.prepareStatement(LOCK_SCHEDULE_AND_GET_CAPACITY)) {
                ps.setLong(1, scheduleId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new BookingException("Class schedule not found");
                    }
                    capacity = rs.getInt("capacity");
                }
            }

            // 2. Duplicate booking check (sees a consistent snapshot under the lock).
            try (PreparedStatement ps = conn.prepareStatement(EXISTS_BY_USER_SCHEDULE)) {
                ps.setLong(1, userId);
                ps.setLong(2, scheduleId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getBoolean(1)) {
                        throw new BookingException("You have already booked this class");
                    }
                }
            }

            // 3. Capacity check.
            int confirmed = 0;
            try (PreparedStatement ps = conn.prepareStatement(COUNT_CONFIRMED)) {
                ps.setLong(1, scheduleId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) confirmed = rs.getInt(1);
                }
            }
            if (confirmed >= capacity) {
                throw new BookingException("This class is fully booked");
            }

            // 4. Insert booking.
            Booking booking = Booking.builder()
                    .memberId(userId)
                    .scheduleId(scheduleId)
                    .status("CONFIRMED")
                    .build();
            try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
                ps.setLong(1, userId);
                ps.setLong(2, scheduleId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        booking.setId(rs.getLong("id"));
                        booking.setBookedAt(rs.getTimestamp("booked_at").toLocalDateTime());
                    }
                }
            }

            conn.commit();
            log.info("Booking created (locked): id={}, userId={}, scheduleId={}",
                    booking.getId(), userId, scheduleId);
            return booking;

        } catch (BookingException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            log.error("Error in bookWithLock for userId={}, scheduleId={}: {}", userId, scheduleId, e.getMessage());
            throw new RuntimeException("Failed to book class", e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                log.error("Failed to reset autoCommit: {}", e.getMessage());
            }
            pool.releaseConnection(conn);
        }
    }

    /**
     * Safely rolls back a transaction without throwing exceptions.
     * Logs errors if rollback fails.
     *
     * @param conn the database connection to rollback
     */
    private void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ex) {
            log.error("Rollback failed: {}", ex.getMessage());
        }
    }

    /**
     * Finds all schedule IDs that have been booked by a member with given email.
     * Used to display booking status on the schedule page (highlight already booked classes).
     *
     * @param email the member's email address
     * @return list of schedule IDs the member has booked (confirmed bookings only),
     *         empty list if user has no bookings or doesn't exist
     */
    @Override
    public List<Long> findBookedScheduleIdsByEmail(String email) {
        String sql = """
                SELECT b.schedule_id
                FROM bookings b
                JOIN members m ON b.member_id = m.id
                WHERE m.email = ? AND b.status = 'CONFIRMED'
                ORDER BY b.schedule_id
                """;

        try (Connection con = pool.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, email);

            List<Long> scheduleIds = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    scheduleIds.add(rs.getLong("schedule_id"));
                }
            }
            return scheduleIds;
        } catch (SQLException e) {
            log.error("Failed to find booked schedule IDs for email {}: {}", email, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Finds a booking by ID with all related details (class name, trainer name, room, etc.).
     *
     * @param id the booking ID
     * @return Optional containing the booking if found, empty otherwise
     * @throws RuntimeException if the query fails
     */
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

    /**
     * Finds all upcoming confirmed bookings for a user (classes that haven't happened yet).
     * Results are ordered by scheduled time (ascending).
     *
     * @param userId the member ID
     * @return list of upcoming bookings, empty list if none found
     * @throws RuntimeException if the query fails
     */
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

    /**
     * Finds all bookings for a specific class schedule.
     * Used to see who has booked a particular class slot.
     *
     * @param scheduleId the class schedule ID
     * @return list of bookings for this schedule, empty list if none found
     * @throws RuntimeException if the query fails
     */
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

    /**
     * Counts the number of confirmed bookings for a class schedule.
     * Used to determine if a class is full and prevent overbooking.
     *
     * @param scheduleId the class schedule ID
     * @return the count of confirmed bookings, 0 if none found or error occurs
     */
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

    /**
     * Checks if a user has an existing confirmed booking for a class schedule.
     * Used to prevent duplicate bookings and to display booking status.
     *
     * @param userId the member ID
     * @param scheduleId the class schedule ID
     * @return true if the user has already booked this class, false otherwise
     * @throws RuntimeException if the query fails
     */
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

    /**
     * Updates the status of a booking (e.g., CONFIRMED → COMPLETED or CANCELLED).
     *
     * @param bookingId the booking ID
     * @param status the new status (e.g., 'COMPLETED', 'CANCELLED')
     * @throws RuntimeException if the update fails
     */
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

    /**
     * Cancels a booking by changing its status to 'CANCELLED'.
     * Verifies that the booking belongs to the specified user (security check).
     *
     * @param bookingId the booking ID
     * @param userId the member ID (security verification)
     * @throws RuntimeException if the cancellation fails
     */
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

    /**
     * Maps a ResultSet row to a Booking object.
     * Handles null timestamps gracefully.
     *
     * @param rs the result set row
     * @return the mapped Booking object
     * @throws SQLException if column retrieval fails
     */
    private static final String SELECT_FOR_REMINDER = """
            SELECT b.id AS booking_id,
                   m.email AS member_email,
                   m.first_name AS member_first_name,
                   fc.name AS class_name,
                   (co.occurrence_date + cs.start_time) AS scheduled_at,
                   cs.room
            FROM bookings b
            JOIN members m ON b.member_id = m.id
            JOIN class_occurrences co ON b.schedule_id = co.id
            JOIN class_schedules cs ON co.schedule_id = cs.id
            JOIN fitness_classes fc ON cs.class_id = fc.id
            WHERE b.status = 'CONFIRMED'
              AND (co.occurrence_date + cs.start_time) >= ?
              AND (co.occurrence_date + cs.start_time) <  ?
            ORDER BY scheduled_at
            """;

    @Override
    public java.util.List<com.femfit.dto.BookingReminderDto> findBookingsForReminder(
            java.time.LocalDateTime windowStart, java.time.LocalDateTime windowEnd) {
        Connection conn = pool.getConnection();
        java.util.List<com.femfit.dto.BookingReminderDto> list = new java.util.ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_FOR_REMINDER)) {
            ps.setTimestamp(1, java.sql.Timestamp.valueOf(windowStart));
            ps.setTimestamp(2, java.sql.Timestamp.valueOf(windowEnd));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(com.femfit.dto.BookingReminderDto.builder()
                            .bookingId(rs.getLong("booking_id"))
                            .memberEmail(rs.getString("member_email"))
                            .memberFirstName(rs.getString("member_first_name"))
                            .className(rs.getString("class_name"))
                            .scheduledAt(rs.getTimestamp("scheduled_at") != null
                                    ? rs.getTimestamp("scheduled_at").toLocalDateTime() : null)
                            .room(rs.getString("room"))
                            .build());
                }
            }
        } catch (SQLException e) {
            log.error("Error finding bookings for reminder: {}", e.getMessage());
            throw new RuntimeException("Failed to find bookings for reminder", e);
        } finally {
            pool.releaseConnection(conn);
        }
        return list;
    }

    private Booking mapRow(ResultSet rs) throws SQLException {
        return Booking.builder()
                .id(rs.getLong("id"))
                .memberId(rs.getLong("member_id"))
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