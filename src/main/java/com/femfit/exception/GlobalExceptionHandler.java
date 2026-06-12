package com.femfit.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Global fallback exception handler.
 *
 * Business exceptions (e.g. BookingException, ValidationException,
 * InvalidPasswordException) are redirected back to the page the request
 * came from, with an "error" query parameter that templates display via
 * th:if="${param.error}".
 *
 * NOTE: AuthController already handles EmailAlreadyTakenException itself
 * via BindingResult (render-same-view pattern) — this advice is a safety
 * net for everything that is NOT caught locally in a controller.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Business exceptions — redirect back to the originating page with an error message
    @ExceptionHandler({
            BookingException.class,
            InvalidPasswordException.class,
    })
    public ModelAndView handleBusinessException(Exception ex, HttpServletRequest request) {
        log.warn("Business exception on {}: {}", request.getRequestURI(), ex.getMessage());
        return redirectBackWithError(request, ex.getMessage());
    }

    // Access denied — dedicated 403 page
    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());
        ModelAndView mav = new ModelAndView("error/403");
        mav.setStatus(HttpStatus.FORBIDDEN);
        return mav;
    }

    // Anything else — dedicated 500 page
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        ModelAndView mav = new ModelAndView("error/500");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }

    private ModelAndView redirectBackWithError(HttpServletRequest request, String message) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            referer = request.getContextPath() + "/";
        }

        String separator = referer.contains("?") ? "&" : "?";
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);

        return new ModelAndView("redirect:" + referer + separator + "error=" + encoded);
    }
}