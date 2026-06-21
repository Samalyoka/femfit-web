package com.femfit.service;

import com.femfit.dto.ClientOrderDto;
import com.femfit.dto.TrainerDto;
import com.femfit.dto.TrainerProfileDto;
import com.femfit.model.Assignment;
import com.femfit.model.Member;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for trainer dashboard and assignment management.
 */
public interface TrainerService {

    /**
     * Returns all clients assigned to the trainer identified by their member/user id.
     *
     * @param userIdOfTrainer the member id of the logged-in trainer
     * @return list of clients assigned to this trainer, possibly empty
     */
    List<Member> getClientsByTrainerUserId(long userIdOfTrainer);

    /**
     * Returns all clients assigned to the trainer, each paired with their
     * associated order details (cycle, status, dates).
     *
     * @param userIdOfTrainer the member id of the logged-in trainer
     * @return list of client/order pairs, possibly empty
     */
    List<ClientOrderDto> getClientsWithOrderByTrainerUserId(long userIdOfTrainer);

    /**
     * Returns all trainers in the system, regardless of active status or rating.
     *
     * @return list of all trainers, possibly empty
     */
    List<TrainerDto> getAllTrainers();

    /**
     * Returns all trainers along with their aggregated review rating,
     * for display on trainer-selection pages.
     *
     * @return list of trainers with rating info, possibly empty
     */
    List<TrainerDto> getAllTrainersWithRating();

    /**
     * Full public profiles for all active trainers, for the "Our Trainers" page.
     *
     * @return list of trainer profiles, possibly empty
     */
    List<TrainerProfileDto> getAllTrainerProfiles();

    /**
     * @deprecated This returns the most recently updated assignment across
     * ALL of a client's orders, regardless of trainer — which is wrong when
     * a client has orders with multiple trainers. Use
     * {@link #getAssignmentForOrder(long)} instead, scoped to a specific order.
     */
    @Deprecated
    Optional<Assignment> getAssignmentForClient(long clientId);

    /**
     * Returns the assignment tied to a specific order, if any.
     * This is the correct lookup for trainer/client assignment views —
     * assignments belong to an order (assignments.order_id), not to a
     * client directly, so this must be scoped by orderId to avoid one
     * trainer's assignment leaking into another trainer's view of the
     * same client.
     *
     * @param orderId the order id
     * @return Optional with the assignment for this order, or empty if none exists
     */
    Optional<Assignment> getAssignmentForOrder(long orderId);

    /**
     * Creates a new assignment for the given order, or updates the existing
     * one if an assignment for that order already exists (one assignment
     * per order).
     *
     * @param assignment the assignment to save or update; must have orderId set
     */
    void saveOrUpdateAssignment(Assignment assignment);

    /**
     * Permanently removes the assignment associated with the given order.
     *
     * @param orderId the order whose assignment should be deleted
     */
    void deleteAssignment(long orderId);

    /**
     * Updates the status of an assignment (e.g. marking it as completed).
     *
     * @param assignmentId the id of the assignment to update
     * @param status       the new status value
     */
    void updateAssignmentStatus(Long assignmentId, String status);
}