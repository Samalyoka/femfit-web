package com.femfit.exception;

/**
 * Thrown when a booking operation fails.
 *
 * Common scenarios:
 * - User has already booked this class
 * - Class is fully booked (no available spots)
 * - Class schedule not found
 * - User is not authenticated
 *
 * This is a runtime exception and should bubble up to GlobalExceptionHandler
 * which redirects the user back to the schedule page with an error message.
 */
public class BookingException extends RuntimeException {

    /**
     * Constructs a BookingException with the given detail message.
     *
     * @param message the detail message explaining why the booking failed
     */
    public BookingException(String message) {
        super(message);
    }
}
