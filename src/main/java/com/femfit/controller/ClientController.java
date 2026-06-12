package com.femfit.controller;

import com.femfit.dto.ChangePasswordDto;
import com.femfit.exception.BookingException;
import com.femfit.exception.InvalidPasswordException;
import com.femfit.model.*;
import com.femfit.service.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.femfit.dto.ChangePasswordDto;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.List;

/**
 * Handles all client-specific actions:
 * profile, bookings, orders, assignments.
 */
@Controller
@RequestMapping("/client")
public class ClientController {

    private static final Logger log = LoggerFactory.getLogger(ClientController.class);

    private final MemberService userService;
    private final BookingService bookingService;
    private final OrderService orderService;
    private final TrainingCycleService trainingCycleService;
    private final TrainerService trainerService;

    @Autowired
    public ClientController(MemberService userService,
                            BookingService bookingService,
                            OrderService orderService,
                            TrainingCycleService trainingCycleService,
                            TrainerService trainerService) {
        this.userService = userService;
        this.bookingService = bookingService;
        this.orderService = orderService;
        this.trainingCycleService = trainingCycleService;
        this.trainerService = trainerService;
    }

    /**
     * Client profile page with stats and upcoming bookings.
     */
    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Member member = getUser(userDetails);
        List<Booking> bookings = bookingService.getUpcoming(member.getId());
        List<Order> orders = orderService.findByUserId(member.getId());

        model.addAttribute("member", member);
        model.addAttribute("bookings", bookings);
        model.addAttribute("orders", orders);
        model.addAttribute("visitCount", bookingService.countVisitsThisMonth(member.getId()));
        model.addAttribute("changePasswordDto", new ChangePasswordDto());
        return "client/profile";
    }

    /**
     * Books a class for the current member.
     *
     * @param scheduleId the schedule slot to book
     */
    @PostMapping("/book/{scheduleId}")
    public String book(@PathVariable Long scheduleId,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirectAttrs) {
        Member member = getUser(userDetails);
        try {
            bookingService.book(member.getId(), scheduleId);
            redirectAttrs.addFlashAttribute("success", "msg.success.booking");
        } catch (BookingException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/schedule";
    }

    /**
     * Cancels a booking.
     *
     * @param bookingId the booking to cancel
     */
    @PostMapping("/booking/cancel/{bookingId}")
    public String cancelBooking(@PathVariable Long bookingId,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttrs) {
        Member member = getUser(userDetails);
        bookingService.cancel(bookingId, member.getId());
        redirectAttrs.addFlashAttribute("success", "msg.success.cancel");
        return "redirect:/client/profile";
    }

    /**
     * Client's orders list.
     */
    @GetMapping("/orders")
    public String orders(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Member member = getUser(userDetails);
        model.addAttribute("orders", orderService.findByUserId(member.getId()));
        return "client/orders";
    }

    /**
     * View assignment for a specific order.
     */
    @GetMapping("/assignment/{orderId}")
    public String assignment(@PathVariable Long orderId,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model) {
        Member member = getUser(userDetails);
        orderService.findById(orderId).ifPresent(order -> {
            model.addAttribute("order", order);
            orderService.findAssignment(orderId).ifPresent(a ->
                    model.addAttribute("assignment", a));
        });
        return "client/assignment";
    }

    /**
     * Client requests a revision on their assignment.
     *
     * @param assignmentId the assignment id
     */
    @PostMapping("/assignment/revision/{assignmentId}")
    public String requestRevision(@PathVariable Long assignmentId,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttrs) {
        log.info("Revision requested for assignment id={}", assignmentId);
        orderService.requestRevision(assignmentId);
        redirectAttrs.addFlashAttribute("success", "msg.success.revision.requested");
        return "redirect:/client/profile";
    }

    // Helper — loads full User from DB using Spring Security email
    private Member getUser(UserDetails userDetails) {
        return userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Shows all available training cycles for purchase.
     */
    @GetMapping("/cycles")
    public String cycles(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Member member = getUser(userDetails);
        List<TrainingCycle> cycles = trainingCycleService.findAllActive();
        List<Order> myOrders = orderService.findByUserId(member.getId());
        List<Integer> purchasedCycleIds = myOrders.stream()
                .map(Order::getCycleId)
                .toList();
        model.addAttribute("cycles", cycles);
        model.addAttribute("purchasedCycleIds", purchasedCycleIds);
        return "client/cycles";
    }

    /**
     * Shows trainer selection page before placing order.
     */
    @GetMapping("/cycles/{cycleId}/choose-trainer")
    public String chooseTrainer(@PathVariable Integer cycleId,
                                Model model) {
        trainingCycleService.findById(cycleId)
                .ifPresent(cycle -> model.addAttribute("cycle", cycle));
        model.addAttribute("trainers", trainerService.getAllTrainers());
        return "client/choose-trainer";
    }

    /**
     * Places an order with selected trainer.
     */
    @PostMapping("/cycles/{cycleId}/order")
    public String placeOrder(@PathVariable Integer cycleId,
                             @RequestParam(required = false) Long trainerId,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttrs) {
        Member member = getUser(userDetails);
        trainingCycleService.findById(cycleId).ifPresentOrElse(
                cycle -> {
                    orderService.placeOrder(member.getId(), cycleId,
                            cycle.getPrice(), trainerId);
                    redirectAttrs.addFlashAttribute("success",
                            "Order placed successfully!");
                },
                () -> redirectAttrs.addFlashAttribute("error",
                        "Training cycle not found.")
        );
        return "redirect:/client/orders";
    }

  /**
     * Changes the current member's password.
     * Validates new password length/match and verifies the current password
     * before delegating to the service layer.
     */
    @PostMapping("/profile/password")
    public String changePassword(@Valid @ModelAttribute("changePasswordDto") ChangePasswordDto dto,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttrs) {
        if (bindingResult.hasErrors()) {
            log.warn("Password change rejected due to {} validation errors", bindingResult.getErrorCount());
            redirectAttrs.addFlashAttribute("error", "msg.error.password.invalid");
            return "redirect:/client/profile";
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            redirectAttrs.addFlashAttribute("error", "msg.error.password.mismatch");
            return "redirect:/client/profile";
        }

        Member member = getUser(userDetails);
        try {
            userService.changePassword(member.getId(), dto.getCurrentPassword(), dto.getNewPassword());
            redirectAttrs.addFlashAttribute("success", "msg.success.password.changed");
        } catch (InvalidPasswordException e) {
            log.warn("Password change failed for member id={}: {}", member.getId(), e.getMessage());
            redirectAttrs.addFlashAttribute("error", "msg.error.password.current.wrong");
        }
        return "redirect:/client/profile";
    }
}