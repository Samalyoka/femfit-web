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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Handles all client-specific actions:
 * profile, bookings, orders, assignments, avatar upload.
 */
@Controller
@RequestMapping("/client")
public class ClientController {

    private static final Logger log = LoggerFactory.getLogger(ClientController.class);

    private static final int PAGE_SIZE_ORDERS = 10;
    private static final int PAGE_SIZE_CYCLES = 9;

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

    /** Upload avatar for CLIENT */
    @PostMapping("/profile/avatar")
    public String uploadAvatar(@RequestParam("avatarFile") MultipartFile file,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttrs) {
        Member member = getUser(userDetails);
        try {
            userService.uploadAvatar(member.getId(), file);
            redirectAttrs.addFlashAttribute("success", "msg.success.avatar.updated");
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", "msg.error.avatar.invalid");
            log.warn("Avatar upload rejected for member {}: {}", member.getId(), e.getMessage());
        }
        return "redirect:/client/profile";
    }

    @PostMapping("/book/{scheduleId}")
    public String book(@PathVariable Long scheduleId,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirectAttrs) {
        Member member = getUser(userDetails);
        bookingService.book(member.getId(), scheduleId);
        redirectAttrs.addFlashAttribute("success", "msg.success.booking");
        return "redirect:/schedule";
    }

    @PostMapping("/booking/cancel/{bookingId}")
    public String cancelBooking(@PathVariable Long bookingId,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttrs) {
        Member member = getUser(userDetails);
        bookingService.cancel(bookingId, member.getId());
        redirectAttrs.addFlashAttribute("success", "msg.success.cancel");
        return "redirect:/client/profile";
    }

    @GetMapping("/orders")
    public String orders(@AuthenticationPrincipal UserDetails userDetails,
                         @RequestParam(defaultValue = "1") int page,
                         Model model) {
        Member member = getUser(userDetails);
        int offset = (page - 1) * PAGE_SIZE_ORDERS;
        List<Order> orders = orderService.findByUserId(member.getId(), offset, PAGE_SIZE_ORDERS);
        List<Long> reviewedOrderIds = orders.stream()
                .map(Order::getId).filter(reviewService::hasReview).toList();
        int totalOrders = orderService.countByUserId(member.getId());
        model.addAttribute("orders", orders);
        model.addAttribute("reviewedOrderIds", reviewedOrderIds);
        model.addAttribute("member", member);
        model.addAttribute("totalPages", (int) Math.ceil(totalOrders / (double) PAGE_SIZE_ORDERS));
        model.addAttribute("currentPage", page);
        return "client/orders";
    }

    @GetMapping("/archive")
    public String archive(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Member member = getUser(userDetails);
        model.addAttribute("entries", orderService.getArchive(member.getId()));
        model.addAttribute("member", member);
        return "client/archive";
    }

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

    @PostMapping("/assignment/revision/{assignmentId}")
    public String requestRevision(@PathVariable Long assignmentId,
                                  @RequestParam(name = "orderId") Long orderId,
                                  @RequestParam(name = "revisionExercises",  defaultValue = "false") boolean revisionExercises,
                                  @RequestParam(name = "revisionEquipment",  defaultValue = "false") boolean revisionEquipment,
                                  @RequestParam(name = "revisionNutrition",  defaultValue = "false") boolean revisionNutrition,
                                  @RequestParam(name = "revisionSchedule",   defaultValue = "false") boolean revisionSchedule,
                                  @RequestParam(name = "revisionComment", required = false) String revisionComment,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttrs) {
        orderService.requestRevision(assignmentId, revisionExercises, revisionEquipment,
                revisionNutrition, revisionSchedule, revisionComment);
        redirectAttrs.addFlashAttribute("success", "msg.success.revision.requested");
        return "redirect:/client/assignment/" + orderId;
    }

    @GetMapping("/cycles")
    public String cycles(@AuthenticationPrincipal UserDetails userDetails,
                         @RequestParam(defaultValue = "1") int page,
                         Model model) {
        Member member = getUser(userDetails);
        int offset = (page - 1) * PAGE_SIZE_CYCLES;
        List<TrainingCycle> cycles = trainingCycleService.findAllActive(offset, PAGE_SIZE_CYCLES);
        List<Order> myOrders = orderService.findByUserId(member.getId());
        List<Integer> purchasedCycleIds = myOrders.stream().map(Order::getCycleId).toList();
        int totalActive = trainingCycleService.countActive();
        model.addAttribute("cycles", cycles);
        model.addAttribute("purchasedCycleIds", purchasedCycleIds);
        model.addAttribute("totalPages", (int) Math.ceil(totalActive / (double) PAGE_SIZE_CYCLES));
        model.addAttribute("currentPage", page);
        return "client/cycles";
    }

    @GetMapping("/cycles/{cycleId}/choose-trainer")
    public String chooseTrainer(@PathVariable Integer cycleId, Model model) {
        trainingCycleService.findById(cycleId)
                .ifPresent(cycle -> model.addAttribute("cycle", cycle));
        model.addAttribute("trainers", trainerService.getAllTrainersWithRating());
        return "client/choose-trainer";
    }

    @PostMapping("/cycles/{cycleId}/order")
    public String placeOrder(@PathVariable Integer cycleId,
                             @RequestParam(required = false) Long trainerId,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttrs) {
        Member member = getUser(userDetails);
        Optional<TrainingCycle> cycleOpt = trainingCycleService.findById(cycleId);
        if (cycleOpt.isEmpty()) {
            redirectAttrs.addFlashAttribute("error", "Training cycle not found.");
            return "redirect:/client/cycles";
        }
        TrainingCycle cycle = cycleOpt.get();
        orderService.placeOrder(member.getId(), cycleId, cycle.getPrice(), trainerId);
        String msgKey = (trainerId != null) ? "order.placed.successfully" : "order.placed.no.trainer";
        redirectAttrs.addFlashAttribute("successMsg", msgKey);
        return "redirect:/client/orders";
    }

    @PostMapping("/profile/password")
    public String changePassword(@Valid @ModelAttribute("changePasswordDto") ChangePasswordDto dto,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttrs) {
        if (bindingResult.hasErrors()) {
            redirectAttrs.addFlashAttribute("error", "msg.error.password.invalid");
            return "redirect:/client/profile";
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            redirectAttrs.addFlashAttribute("error", "msg.error.password.mismatch");
            return "redirect:/client/profile";
        }
        Member member = getUser(userDetails);
        userService.changePassword(member.getId(), dto.getCurrentPassword(), dto.getNewPassword());
        redirectAttrs.addFlashAttribute("success", "msg.success.password.changed");
        return "redirect:/client/profile";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(@Valid @ModelAttribute("updateProfileDto") UpdateProfileDto dto,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttrs) {
        if (bindingResult.hasErrors()) {
            redirectAttrs.addFlashAttribute(
                    "org.springframework.validation.BindingResult.updateProfileDto", bindingResult);
            redirectAttrs.addFlashAttribute("updateProfileDto", dto);
            redirectAttrs.addFlashAttribute("error", "msg.error.profile.invalid");
            return "redirect:/client/profile";
        }
        Member member = getUser(userDetails);
        member.setFirstName(dto.getFirstName());
        member.setLastName(dto.getLastName());
        member.setPhone(dto.getPhone());
        member.setBirthDate(dto.getBirthDate());
        userService.updateProfile(member);
        redirectAttrs.addFlashAttribute("success", "msg.success.profile.updated");
        return "redirect:/client/profile";
    }

    private Member getUser(UserDetails userDetails) {
        return userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Member not found"));
    }
}
