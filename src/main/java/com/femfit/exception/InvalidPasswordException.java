package com.femfit.exception;

/**
 * Thrown when password validation fails during password change operations.
 *
 * Common scenarios:
 * - User entered incorrect current password
 * - New password does not meet requirements (length, format, etc.)
 * - New password and confirm password do not match
 *
 * This is a runtime exception and should bubble up to GlobalExceptionHandler
 * which redirects the user back to the profile page with an error message.
 */
public class InvalidPasswordException extends RuntimeException {

    /**
     * Constructs an InvalidPasswordException with the given detail message.
     *
     * @param message the detail message explaining the password validation failure
     */
    public InvalidPasswordException(String message) {
        super(message);
    }
}

