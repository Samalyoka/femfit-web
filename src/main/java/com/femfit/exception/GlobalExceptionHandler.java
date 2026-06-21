package com.femfit.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Global exception handler for all application exceptions.
 *
 * All custom business exceptions (BookingException, InvalidPasswordException, etc.)
 * bubble up from controllers/services and are caught here for centralized handling.
 *
 * Security exceptions (403) are handled by Spring Security in SecurityConfig
 * and do NOT reach this handler.
 *
 * Access-denied (403) is intercepted at the filter level before the request
 * reaches the controller, so it's intentionally NOT handled here.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ══════════════════════════════════════════════════════════════
    // BOOKING EXCEPTIONS
    // ══════════════════════════════════════════════════════════════

    /**
     * Handle booking-related exceptions.
     * User tried to book a class but something went wrong (already booked, class full, etc.)
     * Redirect back to schedule with error message.
     */
    @ExceptionHandler(BookingException.class)
    public String handleBookingException(
            BookingException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        log.warn("Booking error: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());

        String referrer = request.getHeader("Referer");
        return "redirect:" + (referrer != null ? referrer : "/schedule");
    }

    // ══════════════════════════════════════════════════════════════
    // AUTHENTICATION EXCEPTIONS
    // ══════════════════════════════════════════════════════════════

    /**
     * Handle invalid/incorrect password exceptions.
     * User entered wrong password when trying to change it.
     * Redirect back to profile with error message.
     */
    @ExceptionHandler(InvalidPasswordException.class)
    public String handleInvalidPasswordException(
            InvalidPasswordException ex,
            RedirectAttributes redirectAttributes
    ) {
        log.warn("Password validation error: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/client/profile";
    }

    /**
     * Handle email already taken exceptions.
     * User tried to register/update with email that's already in use.
     * Redirect back to registration or profile with error message.
     */
    @ExceptionHandler(EmailAlreadyTakenException.class)
    public String handleEmailAlreadyTakenException(
            EmailAlreadyTakenException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        log.warn("Email already taken: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());

        // If coming from profile update, stay on profile; otherwise go to register
        String referrer = request.getHeader("Referer");
        if (referrer != null && referrer.contains("profile")) {
            return "redirect:/client/profile";
        }
        return "redirect:/auth/register";
    }

    // ══════════════════════════════════════════════════════════════
    // VALIDATION EXCEPTIONS
    // ══════════════════════════════════════════════════════════════

    /**
     * Handle general validation exceptions.
     * Business logic validation failed (invalid input, constraint violation, etc.)
     * Redirect back to referrer with error message.
     */
    @ExceptionHandler(ValidationException.class)
    public String handleValidationException(
            ValidationException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        log.warn("Validation error: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());

        String referrer = request.getHeader("Referer");
        return "redirect:" + (referrer != null ? referrer : "/home");
    }

    // ══════════════════════════════════════════════════════════════
    // GENERIC EXCEPTION (CATCH-ALL)
    // ══════════════════════════════════════════════════════════════

    /**
     * Catch any unhandled exception that wasn't caught by specific handlers.
     * Shows 500 error page to user, logs full stack trace.
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);

        ModelAndView mav = new ModelAndView("error/500");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);

        return mav;
    }
}