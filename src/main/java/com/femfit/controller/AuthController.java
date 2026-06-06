package com.femfit.controller;

import com.femfit.dto.RegisterDto;
import com.femfit.exception.EmailAlreadyTakenException;
import com.femfit.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles user authentication operations including rendering login/registration forms
 * and processing user registration.
 */
@Controller
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Renders the login page.
     * actual POST authentication request processing is intercepted and handled by Spring Security.
     *
     * @param error  flag appended by Spring Security if authentication fails (?error=true)
     * @param logout flag appended by Spring Security upon successful session termination (?logout=true)
     * @param model  Spring MVC model container
     * @return target login view path
     */
    @GetMapping("/login")
    public String loginPage(@RequestParam(name = "error", required = false) String error,
                            @RequestParam(name = "logout", required = false) String logout,
                            Model model) {
        log.info("Processing login page view request. error_present={}, logout_present={}", error != null, logout != null);
        if (error != null) {
            model.addAttribute("loginError", true);
        }
        if (logout != null) {
            model.addAttribute("logoutMsg", true);
        }
        return "auth/login"; // Ensure template location matches: src/main/resources/templates/auth/login.html
    }

    /**
     * Renders the clean user registration form bound to a blank DTO instance.
     *
     * @param model Spring MVC model container
     * @return target registration view path
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        log.info("Processing account registration page view request.");
        // Explicitly ensuring an uninitialized, clean DTO is pushed to clear form values
        model.addAttribute("registerDto", new RegisterDto());
        return "auth/register"; // Ensure template location matches: src/main/resources/templates/auth/register.html
    }

    /**
     * Processes incoming user registration form submissions.
     * Performs schema validation and handles unique email constraints.
     *
     * @param dto           validated registration form payload
     * @param bindingResult holds validation constraints computation output
     * @param redirectAttrs operational flash attributes mapping for multi-request data transfers
     * @return view redirection routing or fallbacks to the registration form UI state
     */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerDto") RegisterDto dto,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttrs) {
        if (bindingResult.hasErrors()) {
            log.warn("Registration payload processing rejected due to {} validation errors", bindingResult.getErrorCount());
            return "auth/register";
        }
        try {
            userService.register(dto);
            redirectAttrs.addFlashAttribute("success", "Registration successful! Please log in.");
            return "redirect:/auth/login?registered";
        } catch (EmailAlreadyTakenException e) {
            log.warn("Registration operation rejected: email address '{}' is already registered", dto.getEmail());
            bindingResult.rejectValue("email", "error.email.taken", "This email is already in use.");
            return "auth/register";
        } catch (Exception e) {
            log.error("Unexpected failure occurred during user registration sequence: {}", e.getMessage(), e);
            bindingResult.reject("error.general", "An error occurred. Please try again later.");
            return "auth/register";
        }
    }
}