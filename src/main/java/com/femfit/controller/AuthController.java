package com.femfit.controller;

import com.femfit.dto.RegisterDto;
import com.femfit.exception.EmailAlreadyTakenException;
import com.femfit.service.MemberService;
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
 * Handles member authentication operations including rendering login/registration forms
 * and processing user registration.
 */
@Controller
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final MemberService memberService;

    @Autowired
    public AuthController(MemberService memberService){
        this.memberService = memberService;
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
     * Renders the registration form.
     * <p>
     * If this request follows a redirect from {@link #register} after a validation
     * error (Post-Redirect-Get), Spring restores {@code registerDto} and its
     * {@link BindingResult} from flash scope automatically — in that case we must
     * NOT overwrite it with a blank DTO, otherwise the user's input and validation
     * messages would be lost on redisplay.
     *
     * @param model Spring MVC model container
     * @return target registration view path
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        log.info("Processing account registration page view request.");
        // Only push a blank DTO if one wasn't already restored from a flash redirect
        if (!model.containsAttribute("registerDto")) {
            model.addAttribute("registerDto", new RegisterDto());
        }
        return "auth/register"; // Ensure template location matches: src/main/resources/templates/auth/register.html
    }

    /**
     * Processes incoming user registration form submissions.
     * Performs schema validation and handles unique email constraints.
     * <p>
     * Follows the Post-Redirect-Get pattern on every branch (success and error):
     * the response is always a redirect, so refreshing the resulting page (F5)
     * never re-submits the registration form. Validation errors and the
     * submitted values are carried to {@code /auth/register} via flash attributes
     * and restored by {@link #registerPage}.
     *
     * @param dto           validated registration form payload
     * @param bindingResult holds validation constraints computation output
     * @param redirectAttrs operational flash attributes mapping for multi-request data transfers
     * @return redirect target — never a direct view render
     */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerDto") RegisterDto dto,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttrs) {
        if (bindingResult.hasErrors()) {
            log.warn("Registration payload processing rejected due to {} validation errors", bindingResult.getErrorCount());
            redirectAttrs.addFlashAttribute("org.springframework.validation.BindingResult.registerDto", bindingResult);
            redirectAttrs.addFlashAttribute("registerDto", dto);
            return "redirect:/auth/register";
        }
        try {
            memberService.register(dto);
            redirectAttrs.addFlashAttribute("success", "Registration successful! Please log in.");
            return "redirect:/auth/login?registered";
        } catch (EmailAlreadyTakenException e) {
            log.warn("Registration operation rejected: email address '{}' is already registered", dto.getEmail());
            bindingResult.rejectValue("email", "error.email.taken", "This email is already in use.");
            redirectAttrs.addFlashAttribute("org.springframework.validation.BindingResult.registerDto", bindingResult);
            redirectAttrs.addFlashAttribute("registerDto", dto);
            return "redirect:/auth/register";
        } catch (Exception e) {
            log.error("Unexpected failure occurred during user registration sequence: {}", e.getMessage(), e);
            bindingResult.reject("error.general", "An error occurred. Please try again later.");
            redirectAttrs.addFlashAttribute("org.springframework.validation.BindingResult.registerDto", bindingResult);
            redirectAttrs.addFlashAttribute("registerDto", dto);
            return "redirect:/auth/register";
        }
    }
}