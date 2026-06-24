package com.femfit.controller;

import com.femfit.dto.ClientOrderDto;
import com.femfit.model.Assignment;
import com.femfit.model.Member;
import com.femfit.model.Order;
import com.femfit.service.MemberService;
import com.femfit.service.OrderService;
import com.femfit.service.TrainerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * Trainer panel for managing client assignments.
 * All exceptions bubble to GlobalExceptionHandler — no try/catch here.
 *
 * IMPORTANT: Assignments are tied to a specific ORDER (assignments.order_id),
 * not to a client directly. A client may have multiple orders with different
 * trainers, so every lookup here MUST go through orderId, never just clientId —
 * otherwise one trainer's assignment can leak into another trainer's view of
 * the same client (this was the root cause of the "duplicated assignments" bug).
 */
@Controller
@RequestMapping("/trainer")
public class TrainerController {

    private static final Logger log = LoggerFactory.getLogger(TrainerController.class);

    private final TrainerService trainerService;
    private final MemberService userService;
    private final OrderService orderService;

    @Autowired
    public TrainerController(TrainerService trainerService,
                             MemberService userService,
                             OrderService orderService) {
        this.trainerService = trainerService;
        this.userService = userService;
        this.orderService = orderService;
    }

    /**
     * GET /trainer/dashboard
     * Shows all clients assigned to this trainer.
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails principal,
                            Model model) {
        Member trainer = resolveCurrentUser(principal);
        List<ClientOrderDto> clients =
                trainerService.getClientsWithOrderByTrainerUserId(trainer.getId());
        model.addAttribute("clients", clients);
        model.addAttribute("trainer", trainer);
        log.debug("Trainer dashboard loaded: userId={}, clients={}",
                trainer.getId(), clients.size());
        return "trainer/dashboard";
    }

    /**
     * GET /trainer/client/{clientId}/assignment
     * View current assignment for a client — scoped to THIS order, not
     * just the client, so a client with multiple trainers sees the right plan.
     */
    @GetMapping("/client/{clientId}/assignment")
    public String viewAssignment(@PathVariable long clientId,
                                 @RequestParam long orderId,
                                 Model model) {
        // look up by orderId (assignments.order_id), not by clientId.
        // findLatestByClientId() returns the most recently updated assignment
        // across ALL of the client's orders/trainers — wrong scope here.
        Optional<Assignment> assignment =
                trainerService.getAssignmentForOrder(orderId);

        Order order = orderService.findById(orderId).orElse(null);

        model.addAttribute("assignment", assignment.orElse(null));
        model.addAttribute("clientId", clientId);
        model.addAttribute("orderId", orderId);
        model.addAttribute("hasAssignment", assignment.isPresent());
        model.addAttribute("order", order);
        return "trainer/assignment-view";
    }

    /**
     * GET /trainer/client/{clientId}/assignment/form?orderId={orderId}
     * Shows form to create/edit assignment — scoped to THIS order.
     */
    @GetMapping("/client/{clientId}/assignment/form")
    public String assignmentForm(@PathVariable long clientId,
                                 @RequestParam long orderId,
                                 Model model) {
        // FIX: same as above — scope to orderId, not clientId.
        Optional<Assignment> existing =
                trainerService.getAssignmentForOrder(orderId);

        Assignment assignment = existing.orElseGet(() ->
                Assignment.builder()
                        .orderId(orderId)
                        .status("ACTIVE")
                        .build());

        Order order = orderService.findById(orderId).orElse(null);

        model.addAttribute("assignment", assignment);
        model.addAttribute("clientId", clientId);
        model.addAttribute("orderId", orderId);
        model.addAttribute("editMode", existing.isPresent());
        model.addAttribute("order", order);
        return "trainer/assignment-form";
    }

    /**
     * POST /trainer/assignment/save
     * Creates or updates an assignment.
     */
    @PostMapping("/assignment/save")
    public String saveAssignment(@RequestParam long clientId,
                                 @RequestParam long orderId,
                                 @RequestParam(required = false) String exercises,
                                 @RequestParam(required = false) String equipment,
                                 @RequestParam(required = false) String nutritionPlan,
                                 @RequestParam(required = false) String scheduleInfo,
                                 RedirectAttributes ra) {
        Assignment assignment = Assignment.builder()
                .orderId(orderId)
                .exercises(exercises)
                .equipment(equipment)
                .nutritionPlan(nutritionPlan)
                .scheduleInfo(scheduleInfo)
                .status("ACTIVE")
                .build();

        trainerService.saveOrUpdateAssignment(assignment);
        log.info("Assignment saved: orderId={}, clientId={}", orderId, clientId);

        ra.addFlashAttribute("successMsg", "assignment.saved");
        return "redirect:/trainer/dashboard";
    }

    /**
     * POST /trainer/assignment/{assignmentId}/status
     * Updates assignment status.
     */
    @PostMapping("/assignment/{assignmentId}/status")
    public String updateStatus(@PathVariable long assignmentId,
                               @RequestParam String status,
                               @RequestParam long clientId,
                               @RequestParam long orderId,
                               RedirectAttributes ra) {
        if (!isValidStatus(status)) {
            log.warn("Invalid assignment status attempted: '{}'", status);
            ra.addFlashAttribute("errorMsg", "assignment.status.invalid");
            return "redirect:/trainer/client/" + clientId +
                    "/assignment?orderId=" + orderId;
        }

        trainerService.updateAssignmentStatus(assignmentId, status);
        log.info("Assignment status updated: id={}, status={}", assignmentId, status);

        ra.addFlashAttribute("successMsg", "assignment.status.updated");
        return "redirect:/trainer/client/" + clientId +
                "/assignment?orderId=" + orderId;
    }

    /**
     * POST /trainer/assignment/delete
     * Deletes an assignment.
     */
    @PostMapping("/assignment/delete")
    public String deleteAssignment(@RequestParam long orderId,
                                   RedirectAttributes ra) {
        trainerService.deleteAssignment(orderId);
        log.info("Assignment deleted: orderId={}", orderId);

        ra.addFlashAttribute("successMsg", "assignment.deleted");
        return "redirect:/trainer/dashboard";
    }


    /** Upload avatar for TRAINER */
    @PostMapping("/profile/avatar")
    public String uploadAvatar(@RequestParam("avatarFile") MultipartFile file,
                               @AuthenticationPrincipal UserDetails principal,
                               RedirectAttributes ra) {
        Member trainer = resolveCurrentUser(principal);
        try {
            userService.uploadAvatar(trainer.getId(), file);
            ra.addFlashAttribute("successMsg", "msg.success.avatar.updated");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", "msg.error.avatar.invalid");
        }
        return "redirect:/trainer/dashboard";
    }

    private Member resolveCurrentUser(UserDetails principal) {
        return userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + principal.getUsername()));
    }

    private boolean isValidStatus(String status) {
        return switch (status) {
            case "ACTIVE", "COMPLETED", "REVISION_REQUESTED" -> true;
            default -> false;
        };
    }
}