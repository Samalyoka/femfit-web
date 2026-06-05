package com.femfit.dao;

import com.femfit.model.Booking;

import java.util.List;
import java.util.Optional;

/**
 * DAO interface for {@link Booking} entity.
 * All implementations must use plain JDBC with PreparedStatements.
 */
public interface BookingDao {

    /**
     * Creates a new booking.
     *
     * @param booking the booking to save
     * @return saved booking with generated id
     */
    Booking save(Booking booking);

    /**
     * Finds a booking by id.
     *
     * @param id booking id
     * @return Optional with booking or empty
     */
    Optional<Booking> findById(Long id);

    /**
     * Returns all upcoming bookings for a user with joined class info.
     *
     * @param userId the user's id
     * @return list of bookings with class and trainer info
     */
    List<Booking> findUpcomingByUserId(Long userId);

    /**
     * Returns all bookings for a specific schedule slot.
     *
     * @param scheduleId the schedule id
     * @return list of bookings
     */
    List<Booking> findByScheduleId(Long scheduleId);

    /**
     * Counts confirmed bookings for a schedule (to check capacity).
     *
     * @param scheduleId the schedule id
     * @return number of confirmed bookings
     */
    int countConfirmedByScheduleId(Long scheduleId);

    /**
     * Checks if a user already has a booking for a schedule.
     *
     * @param userId     the user's id
     * @param scheduleId the schedule id
     * @return true if booking exists
     */
    boolean existsByUserAndSchedule(Long userId, Long scheduleId);

    /**
     * Updates booking status (CONFIRMED, CANCELLED, ATTENDED).
     *
     * @param bookingId the booking id
     * @param status    new status
     */
    void updateStatus(Long bookingId, String status);

    /**
     * Cancels a booking by setting status to CANCELLED.
     *
     * @param bookingId the booking id
     * @param userId    the user's id (for security check)
     */
    void cancel(Long bookingId, Long userId);
}