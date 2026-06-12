package com.femfit.controller;

import com.femfit.dto.PageDto;
import com.femfit.model.Role;
import com.femfit.model.Member;
import com.femfit.service.OrderService;
import com.femfit.service.MemberService;
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

    private final MemberService userService;
    private final OrderService orderService;

    @Autowired
    public AdminController(MemberService userService, OrderService orderService) {
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
    public String clients(@RequestParam(name = "page", defaultValue = "1") int page, Model model) {
        PageDto<Member> pageDto = userService.findByRole(Role.CLIENT, page, 10);
        model.addAttribute("page", pageDto);
        model.addAttribute("role", "CLIENT");
        return "admin/users";
    }

    /**
     * Paginated list of trainers.
     */
    @GetMapping("/trainers")
    public String trainers(@RequestParam(name = "page", defaultValue = "1") int page, Model model) {
        PageDto<Member> pageDto = userService.findByRole(Role.TRAINER, page, 10);
        model.addAttribute("page", pageDto);
        model.addAttribute("role", "TRAINER");
        return "admin/users";
    }

    /**
     * All orders list with pagination.
     */
    @GetMapping("/orders")
    public String orders(@RequestParam(defaultValue = "1") int page, Model model) {
        int offset = (page - 1) * 10;
        model.addAttribute("orders", orderService.findAll(offset, 10));
        model.addAttribute("totalPages",
                (int) Math.ceil(orderService.countAll() / 10.0));
        model.addAttribute("currentPage", page);
        return "admin/orders";
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
                              @RequestParam(name = "discountPercent") int discountPercent,
                              RedirectAttributes redirectAttrs) {
        userService.setDiscount(userId, discountPercent);
        redirectAttrs.addFlashAttribute("success", "msg.success.save");
        return "redirect:/admin/clients";
    }

    /**
     * Marks order as completed.
     */
    @PostMapping("/order/complete/{orderId}")
    public String completeOrder(@PathVariable Long orderId,
                                RedirectAttributes ra) {
        orderService.updateStatus(orderId, "COMPLETED");
        ra.addFlashAttribute("success", "Order marked as completed.");
        return "redirect:/admin/orders";
    }

    /**
     * Cancels an order.
     */
    @PostMapping("/order/cancel/{orderId}")
    public String cancelOrder(@PathVariable Long orderId,
                              RedirectAttributes ra) {
        orderService.updateStatus(orderId, "CANCELLED");
        ra.addFlashAttribute("success", "Order cancelled.");
        return "redirect:/admin/orders";
    }
}