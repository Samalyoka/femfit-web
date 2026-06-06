package com.femfit.controller;

import com.femfit.dto.ClientOrderDto;
import com.femfit.model.Assignment;
import com.femfit.model.User;
import com.femfit.service.TrainerService;
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
import java.util.Optional;

@Controller
@RequestMapping("/trainer")
public class TrainerController {

    private static final Logger log = LoggerFactory.getLogger(TrainerController.class);

    private final TrainerService trainerService;
    private final UserService    userService;

    @Autowired
    public TrainerController(TrainerService trainerService, UserService userService) {
        this.trainerService = trainerService;
        this.userService    = userService;
    }

    // ─── Хелпер ───────────────────────────────────────────────────────────────

    private User resolveCurrentUser(UserDetails principal) {
        return userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + principal.getUsername()));
    }

    // ─── Dashboard ────────────────────────────────────────────────────────────

    /**
     * GET /trainer/dashboard
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails principal,
                            Model model) {
        User trainer = resolveCurrentUser(principal);
        List<ClientOrderDto> clients =
                trainerService.getClientsWithOrderByTrainerUserId(trainer.getId());
        model.addAttribute("clients", clients);
        log.debug("Trainer dashboard loaded: userId={}, clients={}",
                trainer.getId(), clients.size());
        return "trainer/trainer-dashboard";
    }

    // ─── Просмотр assignment ──────────────────────────────────────────────────

    /**
     * GET /trainer/client/{clientId}/assignment
     */
    @GetMapping("/client/{clientId}/assignment")
    public String viewAssignment(@PathVariable long clientId,
                                 @RequestParam long orderId,
                                 Model model) {
        Optional<Assignment> assignment =
                trainerService.getAssignmentForClient(clientId);

        model.addAttribute("assignment",     assignment.orElse(null));
        model.addAttribute("clientId",       clientId);
        model.addAttribute("orderId",        orderId);
        model.addAttribute("hasAssignment",  assignment.isPresent());
        return "trainer/assignment-view";
    }

    // ─── Форма create / edit ──────────────────────────────────────────────────

    /**
     * GET /trainer/client/{clientId}/assignment/form?orderId={orderId}
     */
    @GetMapping("/client/{clientId}/assignment/form")
    public String assignmentForm(@PathVariable long clientId,
                                 @RequestParam long orderId,
                                 Model model) {
        Optional<Assignment> existing =
                trainerService.getAssignmentForClient(clientId);

        Assignment assignment = existing.orElseGet(() ->
                Assignment.builder()
                        .orderId(orderId)
                        .status("ACTIVE")
                        .build());

        model.addAttribute("assignment", assignment);
        model.addAttribute("clientId",   clientId);
        model.addAttribute("orderId",    orderId);
        model.addAttribute("editMode",   existing.isPresent());
        return "trainer/assignment-form";
    }

    // ─── Сохранение ───────────────────────────────────────────────────────────

    /**
     * POST /trainer/assignment/save
     *
     * Поля приходят из формы. orderId и clientId — hidden-поля.
     * @ModelAttribute не используем — Assignment имеет getClientId()
     * возвращающий orderId, что сбивает Spring при binding.
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

    // ─── Смена статуса ────────────────────────────────────────────────────────

    /**
     * POST /trainer/assignment/{assignmentId}/status
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

    // ─── Удаление ─────────────────────────────────────────────────────────────

    /**
     * POST /trainer/assignment/delete
     */
    @PostMapping("/assignment/delete")
    public String deleteAssignment(@RequestParam long orderId,
                                   RedirectAttributes ra) {
        trainerService.deleteAssignment(orderId);
        log.info("Assignment deleted: orderId={}", orderId);

        ra.addFlashAttribute("successMsg", "assignment.deleted");
        return "redirect:/trainer/dashboard";
    }

    // ─── Private ──────────────────────────────────────────────────────────────

    private boolean isValidStatus(String status) {
        return switch (status) {
            case "ACTIVE", "COMPLETED", "REVISION_REQUESTED" -> true;
            default -> false;
        };
    }
}