package com.femfit.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Global fallback exception handler.
 *
 * Expected business exceptions (BookingException, InvalidPasswordException,
 * EmailAlreadyTakenException) are handled locally inside the controllers via
 * try/catch + RedirectAttributes or BindingResult, so they never reach here.
 *
 * Access-denied (403) is handled by Spring Security's
 * {@code accessDeniedPage("/error/403")} in {@link com.femfit.config.SecurityConfig},
 * which is intercepted before the request reaches this advice — so it is
 * intentionally NOT duplicated here.
 *
 * This advice covers any other unhandled exception with a dedicated 500 page,
 * so the user never sees a raw stack trace.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Anything unexpected — dedicated 500 page
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        ModelAndView mav = new ModelAndView("error/500");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }
}