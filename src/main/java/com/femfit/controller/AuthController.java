package com.femfit.controller;

import com.femfit.dto.RegisterDto;
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
 *
 * EmailAlreadyTakenException bubbles to GlobalExceptionHandler (no try/catch here).
 */
@Controller
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final MemberService memberService;

    @Autowired
    public AuthController(MemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * Renders the login page.
     * Actual POST authentication request processing is intercepted and handled by Spring Security.
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
        return "auth/login";
    }

    /**
     * Renders the registration form.
     *
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
        if (!model.containsAttribute("registerDto")) {
            model.addAttribute("registerDto", new RegisterDto());
        }
        return "auth/register";
    }

    /**
     * Processes incoming user registration form submissions.
     * Performs schema validation and handles unique email constraints.
     *
     * Follows the Post-Redirect-Get pattern on every branch (success and error):
     * the response is always a redirect, so refreshing the resulting page (F5)
     * never re-submits the registration form. Validation errors and the
     * submitted values are carried to {@code /auth/register} via flash attributes
     * and restored by {@link #registerPage}.
     *
     * NOTE: EmailAlreadyTakenException is NOT caught here — it bubbles up to
     * GlobalExceptionHandler which handles it with a redirect and flash message.
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

        // EmailAlreadyTakenException will bubble to GlobalExceptionHandler
        // No try/catch needed here
        memberService.register(dto);
        redirectAttrs.addFlashAttribute("success", "Registration successful! Please log in.");
        return "redirect:/auth/login?registered";
    }
}