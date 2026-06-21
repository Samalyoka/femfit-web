package com.femfit.controller;

import com.femfit.dto.PageDto;
import com.femfit.dto.TrainingCycleDto;
import com.femfit.model.AccountType;
import com.femfit.model.Member;
import com.femfit.model.Order;
import com.femfit.model.Role;
import com.femfit.model.TrainerAvailability;
import com.femfit.model.TrainingCycle;
import com.femfit.service.MemberService;
import com.femfit.service.OrderService;
import com.femfit.service.ReviewService;
import com.femfit.service.TrainerService;
import com.femfit.service.TrainingCycleService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.Set;

/**
 * Handles all admin functions:
 * member management, discounts, training cycle CRUD, reports.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private static final Set<String> ALLOWED_REDIRECTS =
            Set.of("/admin/clients", "/admin/trainers");
    private static final String DEFAULT_REDIRECT = "/admin/clients";

    private final MemberService userService;
    private final OrderService orderService;
    private final TrainingCycleService cycleService;
    private final ReviewService reviewService;
    private final TrainerService trainerService;

    @Autowired
    public AdminController(MemberService userService,
                           OrderService orderService,
                           TrainingCycleService cycleService,
                           ReviewService reviewService,
                           TrainerService trainerService) {
        this.userService = userService;
        this.orderService = orderService;
        this.cycleService = cycleService;
        this.reviewService = reviewService;
        this.trainerService = trainerService;
    }

    // ── Dashboard ────────────────────────────────────────────────

    /**
     * Admin dashboard — overview with key counts.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("clientCount",
                userService.findByRole(Role.CLIENT, 1, 1000).getTotalItems());
        model.addAttribute("trainerCount",
                userService.findByRole(Role.TRAINER, 1, 1000).getTotalItems());
        model.addAttribute("activeCycles", cycleService.countActive());
        model.addAttribute("totalOrders", orderService.countAll());
        return "admin/dashboard";
    }

    // ── Members ──────────────────────────────────────────────────

    /**
     * Paginated list of clients.
     */
    @GetMapping("/clients")
    public String clients(@RequestParam(name = "page", defaultValue = "1") int page,
                          Model model) {
        PageDto<Member> pageDto = userService.findByRole(Role.CLIENT, page, 10);
        model.addAttribute("page", pageDto);
        model.addAttribute("role", "CLIENT");
        return "admin/members";
    }

    /**
     * Paginated list of trainers.
     */
    @GetMapping("/trainers")
    public String trainers(@RequestParam(name = "page", defaultValue = "1") int page,
                           Model model) {
        PageDto<Member> pageDto = userService.findByRole(Role.TRAINER, page, 10);
        model.addAttribute("page", pageDto);
        model.addAttribute("role", "TRAINER");
        // Availability lives in the trainers table, not members — fetch
        // separately and let the template look it up by id.
        model.addAttribute("trainerAvailability", trainerService.getAllTrainersIncludingUnavailable());
        return "admin/members";
    }

    /**
     * Activates a member account.
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
     * Sets a client's account type (REGULAR/CORPORATE). The discount is
     * automatically recalculated to match the new type's rule — see
     * {@link MemberService#setAccountType}.
     */
    @PostMapping("/member/account-type/{memberId}")
    public String setAccountType(@PathVariable Long memberId,
                                 @RequestParam(name = "accountType") AccountType accountType,
                                 @RequestParam(required = false) String redirectTo,
                                 RedirectAttributes redirectAttrs) {
        userService.setAccountType(memberId, accountType);
        redirectAttrs.addFlashAttribute("success", "msg.success.save");
        return "redirect:" + resolveRedirect(redirectTo);
    }

    /**
     * Sets a trainer's availability (AVAILABLE/UNAVAILABLE). An UNAVAILABLE
     * trainer is hidden from the client-facing choose-trainer page; their
     * existing orders are unaffected until reassigned via
     * {@link #reassignOrderTrainer}.
     */
    @PostMapping("/trainer/availability/{trainerId}")
    public String setTrainerAvailability(@PathVariable Long trainerId,
                                         @RequestParam(name = "availabilityStatus") TrainerAvailability availabilityStatus,
                                         @RequestParam(required = false) String redirectTo,
                                         RedirectAttributes redirectAttrs) {
        trainerService.setAvailability(trainerId, availabilityStatus);
        redirectAttrs.addFlashAttribute("success", "msg.success.save");
        return "redirect:" + resolveRedirect(redirectTo);
    }

    // ── Orders ───────────────────────────────────────────────────

    /**
     * All orders with pagination.
     */
    @GetMapping("/orders")
    public String orders(@RequestParam(defaultValue = "1") int page, Model model) {
        int offset = (page - 1) * 10;
        model.addAttribute("orders", orderService.findAll(offset, 10));
        model.addAttribute("totalPages",
                (int) Math.ceil(orderService.countAll() / 10.0));
        model.addAttribute("currentPage", page);
        // For the trainer-reassignment dropdown — only trainers who can
        // currently take on a reassigned order.
        model.addAttribute("availableTrainers", trainerService.getAllTrainersWithRating());
        return "admin/orders";
    }

    /**
     * Marks order as completed, then recalculates the client's automatic
     * discount (completing a cycle may push a regular client into a higher
     * loyalty tier — see {@link MemberService#recalculateDiscount}).
     */
    @PostMapping("/order/complete/{orderId}")
    public String completeOrder(@PathVariable Long orderId, RedirectAttributes ra) {
        Optional<Order> order = orderService.findById(orderId);
        orderService.updateStatus(orderId, "COMPLETED");
        if (order.isPresent()) {
            userService.recalculateDiscount(order.get().getMemberId());
        } else {
            log.warn("Order id={} not found — discount not recalculated", orderId);
        }
        ra.addFlashAttribute("success", "msg.success.save");
        return "redirect:/admin/orders";
    }

    /**
     * Cancels an order.
     */
    @PostMapping("/order/cancel/{orderId}")
    public String cancelOrder(@PathVariable Long orderId, RedirectAttributes ra) {
        orderService.updateStatus(orderId, "CANCELLED");
        ra.addFlashAttribute("success", "msg.success.save");
        return "redirect:/admin/orders";
    }

    /**
     * Reassigns an order's trainer — for when the primary trainer is
     * unavailable (vacation, sick leave) and the client needs to be
     * handed to a different, available trainer.
     */
    @PostMapping("/order/reassign-trainer/{orderId}")
    public String reassignOrderTrainer(@PathVariable Long orderId,
                                       @RequestParam(name = "trainerId") Long trainerId,
                                       RedirectAttributes ra) {
        orderService.assignTrainer(orderId, trainerId);
        ra.addFlashAttribute("success", "msg.success.save");
        return "redirect:/admin/orders";
    }

    // ── Training Cycles ──────────────────────────────────────────

    /**
     * List of all training cycles (active and inactive).
     */
    @GetMapping("/cycles")
    public String cycles(Model model) {
        model.addAttribute("cycles", cycleService.findAll());
        return "admin/cycles";
    }

    /**
     * Show create cycle form.
     */
    @GetMapping("/cycles/new")
    public String newCycleForm(Model model) {
        model.addAttribute("cycleDto", new TrainingCycleDto());
        model.addAttribute("isNew", true);
        return "admin/cycle-form";
    }

    /**
     * Process create cycle form submission.
     */
    @PostMapping("/cycles/new")
    public String createCycle(@Valid @ModelAttribute("cycleDto") TrainingCycleDto dto,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isNew", true);
            return "admin/cycle-form";
        }
        TrainingCycle created = cycleService.create(dto);
        log.info("Admin created training cycle id={}", created.getId());
        ra.addFlashAttribute("success", "msg.success.save");
        return "redirect:/admin/cycles";
    }

    /**
     * Show edit cycle form populated with existing data.
     */
    @GetMapping("/cycles/{id}/edit")
    public String editCycleForm(@PathVariable Integer id, Model model) {
        TrainingCycle cycle = cycleService.findById(id)
                .orElseThrow(() -> new RuntimeException("Cycle not found: " + id));
        TrainingCycleDto dto = new TrainingCycleDto(
                cycle.getTitle(),
                cycle.getDescription(),
                cycle.getDurationWeeks(),
                cycle.getPrice()
        );
        model.addAttribute("cycleDto", dto);
        model.addAttribute("cycle", cycle);
        model.addAttribute("isNew", false);
        return "admin/cycle-form";
    }

    /**
     * Process edit cycle form submission.
     */
    @PostMapping("/cycles/{id}/edit")
    public String updateCycle(@PathVariable Integer id,
                              @Valid @ModelAttribute("cycleDto") TrainingCycleDto dto,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isNew", false);
            model.addAttribute("cycle", cycleService.findById(id).orElse(null));
            return "admin/cycle-form";
        }
        cycleService.update(id, dto);
        log.info("Admin updated training cycle id={}", id);
        ra.addFlashAttribute("success", "msg.success.save");
        return "redirect:/admin/cycles";
    }

    /**
     * Deactivates a training cycle.
     */
    @PostMapping("/cycles/{id}/deactivate")
    public String deactivateCycle(@PathVariable Integer id, RedirectAttributes ra) {
        cycleService.deactivate(id);
        log.info("Admin deactivated training cycle id={}", id);
        ra.addFlashAttribute("success", "msg.success.save");
        return "redirect:/admin/cycles";
    }

    /**
     * Reactivates a training cycle.
     */
    @PostMapping("/cycles/{id}/activate")
    public String activateCycle(@PathVariable Integer id, RedirectAttributes ra) {
        cycleService.activate(id);
        log.info("Admin activated training cycle id={}", id);
        ra.addFlashAttribute("success", "msg.success.save");
        return "redirect:/admin/cycles";
    }

    // ── Statistics ───────────────────────────────────────────────

    /**
     * Admin statistics page — key metrics overview.
     */
    @GetMapping("/stats")
    public String stats(Model model) {
        model.addAttribute("totalClients",
                userService.findByRole(Role.CLIENT, 1, 1000).getTotalItems());
        model.addAttribute("totalTrainers",
                userService.findByRole(Role.TRAINER, 1, 1000).getTotalItems());
        model.addAttribute("totalOrders", orderService.countAll());
        model.addAttribute("activeCycles", cycleService.countActive());
        model.addAttribute("recentReviews", reviewService.getRecentReviews(10));
        return "admin/stats";
    }

    // ── Helpers ──────────────────────────────────────────────────

    private String resolveRedirect(String redirectTo) {
        return ALLOWED_REDIRECTS.contains(redirectTo) ? redirectTo : DEFAULT_REDIRECT;
    }
}