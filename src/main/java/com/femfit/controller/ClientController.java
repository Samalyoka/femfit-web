package com.femfit.controller;

import com.femfit.dto.ChangePasswordDto;
import com.femfit.dto.UpdateProfileDto;
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

import java.util.List;
import java.util.Optional;

/**
 * Handles all client-specific actions:
 * profile, bookings, orders, assignments.
 *
 * All business exceptions (BookingException, InvalidPasswordException, etc.)
 * bubble up to GlobalExceptionHandler — no try/catch here.
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
    private final ReviewService reviewService;

    @Autowired
    public ClientController(MemberService userService,
                            BookingService bookingService,
                            OrderService orderService,
                            TrainingCycleService trainingCycleService,
                            TrainerService trainerService,
                            ReviewService reviewService) {
        this.userService = userService;
        this.bookingService = bookingService;
        this.orderService = orderService;
        this.trainingCycleService = trainingCycleService;
        this.trainerService = trainerService;
        this.reviewService = reviewService;
    }

    /**
     * Client profile page with stats, upcoming bookings, and edit forms.
     * Pre-fills the profile-edit form with the member's current data,
     * unless a flash attribute (from a failed POST /profile/edit) is present.
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

        if (!model.containsAttribute("updateProfileDto")) {
            model.addAttribute("updateProfileDto", new UpdateProfileDto(
                    member.getFirstName(), member.getLastName(),
                    member.getPhone(), member.getBirthDate()));
        }

        return "client/profile";
    }

    /**
     * Books a class for the current member.
     * BookingException bubbles to GlobalExceptionHandler.
     *
     * @param scheduleId the schedule slot to book
     */
    @PostMapping("/book/{scheduleId}")
    public String book(@PathVariable Long scheduleId,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirectAttrs) {
        Member member = getUser(userDetails);

        // BookingException bubbles to GlobalExceptionHandler
        bookingService.book(member.getId(), scheduleId);
        redirectAttrs.addFlashAttribute("success", "msg.success.booking");

        return "redirect:/femfit/schedule";
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
        return "redirect:/femfit/client/profile";
    }

    /**
     * Client's orders list.
     */
    @GetMapping("/orders")
    public String orders(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Member member = getUser(userDetails);
        List<Order> orders = orderService.findByUserId(member.getId());

        List<Long> reviewedOrderIds = orders.stream()
                .map(Order::getId)
                .filter(reviewService::hasReview)
                .toList();

        model.addAttribute("orders", orders);
        model.addAttribute("reviewedOrderIds", reviewedOrderIds);
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
        return "redirect:/femfit/client/profile";
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
     * May throw ValidationException — bubbles to GlobalExceptionHandler.
     */
    @PostMapping("/cycles/{cycleId}/order")
    public String placeOrder(@PathVariable Integer cycleId,
                             @RequestParam(required = false) Long trainerId,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttrs) {
        Member member = getUser(userDetails);

        Optional<TrainingCycle> cycleOpt = trainingCycleService.findById(cycleId);
        if (cycleOpt.isEmpty()) {
            redirectAttrs.addFlashAttribute("error", "Training cycle not found.");
            return "redirect:/femfit/client/cycles";
        }

        TrainingCycle cycle = cycleOpt.get();

        // May throw ValidationException — bubbles to GlobalExceptionHandler
        orderService.placeOrder(member.getId(), cycleId, cycle.getPrice(), trainerId);
        redirectAttrs.addFlashAttribute("success", "Order placed successfully!");

        return "redirect:/client/orders";
    }

    /**
     * Changes the current member's password.
     * Validates new password length/match and verifies the current password.
     * InvalidPasswordException bubbles to GlobalExceptionHandler.
     */
    @PostMapping("/profile/password")
    public String changePassword(@Valid @ModelAttribute("changePasswordDto") ChangePasswordDto dto,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttrs) {
        if (bindingResult.hasErrors()) {
            log.warn("Password change rejected due to {} validation errors", bindingResult.getErrorCount());
            redirectAttrs.addFlashAttribute("error", "msg.error.password.invalid");
            return "redirect:/femfit/client/profile";
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            redirectAttrs.addFlashAttribute("error", "msg.error.password.mismatch");
            return "redirect:/femfit/client/profile";
        }

        Member member = getUser(userDetails);

        // InvalidPasswordException bubbles to GlobalExceptionHandler
        userService.changePassword(member.getId(), dto.getCurrentPassword(), dto.getNewPassword());
        redirectAttrs.addFlashAttribute("success", "msg.success.password.changed");

        return "redirect:/femfit/client/profile";
    }

    /**
     * Updates the current member's profile (firstName, lastName, phone, birthDate).
     * Email and password are not editable through this endpoint.
     * EmailAlreadyTakenException bubbles to GlobalExceptionHandler.
     *
     * On validation failure, redirects back with flash BindingResult and dto
     * (Post-Redirect-Get pattern) so the form re-displays entered values and errors.
     */
    @PostMapping("/profile/edit")
    public String updateProfile(@Valid @ModelAttribute("updateProfileDto") UpdateProfileDto dto,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttrs) {
        if (bindingResult.hasErrors()) {
            log.warn("Profile update rejected due to {} validation errors", bindingResult.getErrorCount());
            redirectAttrs.addFlashAttribute(
                    "org.springframework.validation.BindingResult.updateProfileDto", bindingResult);
            redirectAttrs.addFlashAttribute("updateProfileDto", dto);
            redirectAttrs.addFlashAttribute("error", "msg.error.profile.invalid");
            return "redirect:/femfit/client/profile";
        }

        Member member = getUser(userDetails);
        member.setFirstName(dto.getFirstName());
        member.setLastName(dto.getLastName());
        member.setPhone(dto.getPhone());
        member.setBirthDate(dto.getBirthDate());

        // May throw EmailAlreadyTakenException — bubbles to GlobalExceptionHandler
        userService.updateProfile(member);
        log.info("Profile updated for member id={}", member.getId());
        redirectAttrs.addFlashAttribute("success", "msg.success.profile.updated");

        return "redirect:/femfit/client/profile";
    }

    // Helper — loads full Member from DB using Spring Security email
    private Member getUser(UserDetails userDetails) {
        return userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Member not found"));
    }
}