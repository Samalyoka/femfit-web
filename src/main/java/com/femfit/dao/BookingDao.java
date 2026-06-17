package com.femfit.dao;

import com.femfit.model.Booking;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Booking operations.
 *
 * Provides methods to create, read, update, and delete bookings.
 * Uses pessimistic locking (bookWithLock) to prevent race conditions and overbooking.
 */
public interface BookingDao {

    /**
     * Saves a new booking to the database.
     * Sets the booking ID and booked timestamp from the database response.
     *
     * @param booking the booking to save (memberId and scheduleId must be set)
     * @return the saved booking with ID and timestamp populated
     */
    Booking save(Booking booking);

    /**
     * Books a class with pessimistic locking to prevent race conditions and overbooking.
     *
     * Transaction flow:
     * 1. Lock the schedule row (FOR UPDATE) — serializes concurrent bookings
     * 2. Check if user already booked this class
     * 3. Check if class is still available (below capacity)
     * 4. Insert the booking
     * 5. Commit
     *
     * @param userId the member ID
     * @param scheduleId the class schedule ID
     * @return the created booking with ID and timestamp
     * @throws com.femfit.exception.BookingException if user already booked, class is full, or schedule not found
     */
    Booking bookWithLock(Long userId, Long scheduleId);

    /**
     * Finds a booking by ID with all related details (class name, trainer name, room, etc.).
     *
     * @param id the booking ID
     * @return Optional containing the booking if found, empty otherwise
     */
    Optional<Booking> findById(Long id);

    /**
     * Finds all upcoming confirmed bookings for a user (classes that haven't happened yet).
     * Results are ordered by scheduled time (ascending).
     *
     * @param userId the member ID
     * @return list of upcoming bookings, empty list if none found
     */
    List<Booking> findUpcomingByUserId(Long userId);

    /**
     * Finds all bookings for a specific class schedule.
     * Used to see who has booked a particular class slot.
     *
     * @param scheduleId the class schedule ID
     * @return list of bookings for this schedule, empty list if none found
     */
    List<Booking> findByScheduleId(Long scheduleId);

    /**
     * Counts the number of confirmed bookings for a class schedule.
     * Used to determine if a class is full and prevent overbooking.
     *
     * @param scheduleId the class schedule ID
     * @return the count of confirmed bookings
     */
    int countConfirmedByScheduleId(Long scheduleId);

    /**
     * Checks if a user has an existing confirmed booking for a class schedule.
     * Used to prevent duplicate bookings and to display booking status.
     *
     * @param userId the member ID
     * @param scheduleId the class schedule ID
     * @return true if the user has already booked this class, false otherwise
     */
    boolean existsByUserAndSchedule(Long userId, Long scheduleId);

    /**
     * Updates the status of a booking (e.g., CONFIRMED → COMPLETED or CANCELLED).
     *
     * @param bookingId the booking ID
     * @param status the new status (e.g., 'COMPLETED', 'CANCELLED')
     */
    void updateStatus(Long bookingId, String status);

    /**
     * Cancels a booking by changing its status to 'CANCELLED'.
     * Verifies that the booking belongs to the specified user (security check).
     *
     * @param bookingId the booking ID
     * @param userId the member ID (security verification)
     */
    void cancel(Long bookingId, Long userId);

    /**
     * Finds all schedule IDs that have been booked by a member with given email.
     * Used to display booking status on the schedule page (highlight already booked classes).
     *
     * @param email the member's email address
     * @return list of schedule IDs the member has booked (confirmed bookings only),
     *         empty list if user has no bookings or doesn't exist
     */
    List<Long> findBookedScheduleIdsByEmail(String email);
}