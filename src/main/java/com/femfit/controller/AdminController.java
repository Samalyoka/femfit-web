package com.femfit.controller;

import com.femfit.dto.PageDto;
import com.femfit.model.Member;
import com.femfit.model.Role;
import com.femfit.service.MemberService;
import com.femfit.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

/**
 * Handles all admin functions:
 * member management, discounts, reports.
 *
 * All exceptions bubble to GlobalExceptionHandler — no try/catch here.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    /** Whitelist of pages that activate/deactivate/discount actions may redirect back to. */
    private static final Set<String> ALLOWED_REDIRECTS = Set.of("/femfit/admin/clients", "/femfit/admin/trainers");
    private static final String DEFAULT_REDIRECT = "/femfit/admin/clients";

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
        return "admin/members";
    }

    /**
     * Paginated list of trainers.
     */
    @GetMapping("/trainers")
    public String trainers(@RequestParam(name = "page", defaultValue = "1") int page, Model model) {
        PageDto<Member> pageDto = userService.findByRole(Role.TRAINER, page, 10);
        model.addAttribute("page", pageDto);
        model.addAttribute("role", "TRAINER");
        return "admin/members";
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
     * Activates a member account.
     *
     * @param memberId   member id to activate
     * @param redirectTo page to redirect back to (whitelisted: /admin/clients or /admin/trainers)
     */
    @PostMapping("/member/activate/{memberId}")
    public String activate(@PathVariable Long memberId,
                           @RequestParam(required = false) String redirectTo,
                           RedirectAttributes redirectAttrs) {
        userService.setActive(memberId, true);
        redirectAttrs.addFlashAttribute("success", "msg.success.save");
        log.info("Admin activated member id={}", memberId);
        return "redirect:" + resolveRedirect(redirectTo);
    }

    /**
     * Deactivates a member account.
     *
     * @param memberId   member id to deactivate
     * @param redirectTo page to redirect back to (whitelisted: /admin/clients or /admin/trainers)
     */
    @PostMapping("/member/deactivate/{memberId}")
    public String deactivate(@PathVariable Long memberId,
                             @RequestParam(required = false) String redirectTo,
                             RedirectAttributes redirectAttrs) {
        userService.setActive(memberId, false);
        redirectAttrs.addFlashAttribute("success", "msg.success.save");
        log.info("Admin deactivated member id={}", memberId);
        return "redirect:" + resolveRedirect(redirectTo);
    }

    /**
     * Sets discount for a member.
     *
     * @param memberId        member id
     * @param discountPercent discount 0-100
     * @param redirectTo      page to redirect back to (whitelisted: /admin/clients or /admin/trainers)
     */
    @PostMapping("/member/discount/{memberId}")
    public String setDiscount(@PathVariable Long memberId,
                              @RequestParam(name = "discountPercent") int discountPercent,
                              @RequestParam(required = false) String redirectTo,
                              RedirectAttributes redirectAttrs) {
        userService.setDiscount(memberId, discountPercent);
        redirectAttrs.addFlashAttribute("success", "msg.success.save");
        return "redirect:" + resolveRedirect(redirectTo);
    }

    /**
     * Marks order as completed.
     */
    @PostMapping("/order/complete/{orderId}")
    public String completeOrder(@PathVariable Long orderId,
                                RedirectAttributes ra) {
        orderService.updateStatus(orderId, "COMPLETED");
        ra.addFlashAttribute("success", "Order marked as completed.");
        return "redirect:/femfit/admin/orders";
    }

    /**
     * Cancels an order.
     */
    @PostMapping("/order/cancel/{orderId}")
    public String cancelOrder(@PathVariable Long orderId,
                              RedirectAttributes ra) {
        orderService.updateStatus(orderId, "CANCELLED");
        ra.addFlashAttribute("success", "Order cancelled.");
        return "redirect:/femfit/admin/orders";
    }

    /**
     * Validates the requested redirect target against a whitelist to prevent
     * open-redirect vulnerabilities, falling back to /admin/clients.
     */
    private String resolveRedirect(String redirectTo) {
        return ALLOWED_REDIRECTS.contains(redirectTo) ? redirectTo : DEFAULT_REDIRECT;
    }
}