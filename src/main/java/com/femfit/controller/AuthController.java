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
 * Handles user authentication: login, registration, logout.
 * Logout is handled by Spring Security.
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
     * Shows the login page.
     *
     * @param error   present when login failed
     * @param logout  present after successful logout
     * @param model   Spring MVC model
     * @return login view
     */
    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null) model.addAttribute("loginError", true);
        if (logout != null) model.addAttribute("logoutMsg", true);
        return "auth/login";
    }

    /**
     * Shows the registration page with an empty form.
     *
     * @param model Spring MVC model
     * @return register view
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDto", new RegisterDto());
        return "auth/register";
    }

    /**
     * Processes the registration form.
     * Validates input, creates account, redirects to login on success.
     *
     * @param dto            registration form data (validated)
     * @param bindingResult  validation errors
     * @param redirectAttrs  flash attributes for success message
     * @return redirect to login or back to register on error
     */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerDto") RegisterDto dto,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttrs) {
        if (bindingResult.hasErrors()) {
            log.debug("Registration form has {} errors", bindingResult.getErrorCount());
            return "auth/register";
        }

        try {
            userService.register(dto);
            redirectAttrs.addFlashAttribute("success", "Registration successful! Please log in.");
            return "redirect:/auth/login";
        } catch (EmailAlreadyTakenException e) {
            bindingResult.rejectValue("email", "error.email.taken");
            return "auth/register";
        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage());
            bindingResult.reject("error.general");
            return "auth/register";
        }
    }
}