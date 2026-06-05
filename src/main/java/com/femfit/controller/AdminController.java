package com.femfit.controller;

import com.femfit.dto.PageDto;
import com.femfit.model.Role;
import com.femfit.model.User;
import com.femfit.service.OrderService;
import com.femfit.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles all admin functions:
 * user management, discounts, reports.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final UserService userService;
    private final OrderService orderService;

    @Autowired
    public AdminController(UserService userService, OrderService orderService) {
        this.userService = userService;
        this.orderService = orderService;
    }

    /**
     * Admin dashboard — overview.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("clientCount", userService.findByRole(Role.CLIENT, 1, 1000).getTotalItems());
        model.addAttribute("trainerCount", userService.findByRole(Role.TRAINER, 1, 1000).getTotalItems());
        return "admin/dashboard";
    }

    /**
     * Paginated list of clients.
     *
     * @param page current page number (1-based)
     */
    @GetMapping("/clients")
    public String clients(@RequestParam(defaultValue = "1") int page, Model model) {
        PageDto<User> pageDto = userService.findByRole(Role.CLIENT, page, 10);
        model.addAttribute("page", pageDto);
        model.addAttribute("role", "CLIENT");
        return "admin/users";
    }

    /**
     * Paginated list of trainers.
     */
    @GetMapping("/trainers")
    public String trainers(@RequestParam(defaultValue = "1") int page, Model model) {
        PageDto<User> pageDto = userService.findByRole(Role.TRAINER, page, 10);
        model.addAttribute("page", pageDto);
        model.addAttribute("role", "TRAINER");
        return "admin/users";
    }

    /**
     * Activates a user account.
     *
     * @param userId user id to activate
     */
    @PostMapping("/user/activate/{userId}")
    public String activate(@PathVariable Long userId, RedirectAttributes redirectAttrs) {
        userService.setActive(userId, true);
        redirectAttrs.addFlashAttribute("success", "msg.success.save");
        log.info("Admin activated user id={}", userId);
        return "redirect:/admin/clients";
    }

    /**
     * Deactivates a user account.
     *
     * @param userId user id to deactivate
     */
    @PostMapping("/user/deactivate/{userId}")
    public String deactivate(@PathVariable Long userId, RedirectAttributes redirectAttrs) {
        userService.setActive(userId, false);
        redirectAttrs.addFlashAttribute("success", "msg.success.save");
        log.info("Admin deactivated user id={}", userId);
        return "redirect:/admin/clients";
    }

    /**
     * Sets discount for a user.
     *
     * @param userId          user id
     * @param discountPercent discount 0-100
     */
    @PostMapping("/user/discount/{userId}")
    public String setDiscount(@PathVariable Long userId,
                              @RequestParam int discountPercent,
                              RedirectAttributes redirectAttrs) {
        userService.setDiscount(userId, discountPercent);
        redirectAttrs.addFlashAttribute("success", "msg.success.save");
        return "redirect:/admin/clients";
    }
}