
package com.femfit.exception;

/**
 * Thrown when attempting to register or update a user with an email that is already taken.
 *
 * Common scenarios:
 * - User registration with an email that already exists
 * - Profile update with an email that already exists (for another user)
 *
 * This is a runtime exception and should bubble up to GlobalExceptionHandler
 * which redirects the user back to the registration or profile page with an error message.
 */
public class EmailAlreadyTakenException extends RuntimeException {

    /**
     * Constructs an EmailAlreadyTakenException with the given detail message.
     *
     * @param message the detail message explaining that the email is already in use
     */
    public EmailAlreadyTakenException(String message) {
        super(message);
    }
}