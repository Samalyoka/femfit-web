package com.femfit.service;

import com.femfit.model.Booking;

import java.util.List;

/**
 * Service interface for booking business logic.
 */
public interface BookingService {

    /**
     * Books a class schedule slot for a user.
     * Checks capacity and duplicate bookings before saving.
     *
     * @param userId     the user's id
     * @param scheduleId the schedule slot id
     * @return the created booking
     * @throws com.femfit.exception.BookingException if class is full or already booked
     */
    Booking book(Long userId, Long scheduleId);

    /**
     * Cancels an existing booking.
     *
     * @param bookingId the booking id
     * @param userId    the user's id (ownership check)
     */
    void cancel(Long bookingId, Long userId);

    /**
     * Returns all upcoming bookings for a user.
     *
     * @param userId the user's id
     * @return list of upcoming bookings with class info
     */
    List<Booking> getUpcoming(Long userId);

    /**
     * Returns count of visits this month for a user.
     *
     * @param userId the user's id
     * @return visit count
     */
    int countVisitsThisMonth(Long userId);
}