package com.femfit.controller;

import com.femfit.model.Assignment;
import com.femfit.model.Order;
import com.femfit.model.User;
import com.femfit.service.OrderService;
import com.femfit.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Handles all trainer-specific actions:
 * viewing clients, creating and updating assignments.
 */
@Controller
@RequestMapping("/trainer")
public class TrainerController {

    private static final Logger log = LoggerFactory.getLogger(TrainerController.class);

    private final UserService userService;
    private final OrderService orderService;

    @Autowired
    public TrainerController(UserService userService, OrderService orderService) {
        this.userService = userService;
        this.orderService = orderService;
    }

    /**
     * Trainer dashboard — list of active client orders.
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User trainer = getUser(userDetails);
        List<Order> orders = orderService.findActiveByTrainerId(trainer.getId());
        model.addAttribute("orders", orders);
        model.addAttribute("trainer", trainer);
        return "trainer/dashboard";
    }

    /**
     * Shows assignment form for a specific order.
     *
     * @param orderId the order to create/edit assignment for
     */
    @GetMapping("/assignment/{orderId}")
    public String assignmentForm(@PathVariable Long orderId, Model model) {
        orderService.findById(orderId).ifPresent(order -> model.addAttribute("order", order));

        Assignment assignment = orderService.findAssignment(orderId)
                .orElse(Assignment.builder().orderId(orderId).build());
        model.addAttribute("assignment", assignment);
        return "trainer/assignment-form";
    }

    /**
     * Saves or updates an assignment for a client order.
     *
     * @param assignment the assignment data from form
     */
    @PostMapping("/assignment/save")
    public String saveAssignment(@ModelAttribute Assignment assignment,
                                 RedirectAttributes redirectAttrs) {
        orderService.saveAssignment(assignment);
        redirectAttrs.addFlashAttribute("success", "msg.success.save");
        log.info("Assignment saved for order id={}", assignment.getOrderId());
        return "redirect:/trainer/dashboard";
    }

    /**
     * Marks an assignment as completed.
     *
     * @param orderId the order id
     */
    @PostMapping("/order/complete/{orderId}")
    public String completeOrder(@PathVariable Long orderId,
                                RedirectAttributes redirectAttrs) {
        orderService.updateStatus(orderId, "COMPLETED");
        redirectAttrs.addFlashAttribute("success", "msg.success.save");
        return "redirect:/trainer/dashboard";
    }

    private User getUser(UserDetails userDetails) {
        return userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Trainer not found"));
    }
}