package com.femfit.service;

import com.femfit.dto.ClientOrderDto;
import com.femfit.dto.TrainerDto;
import com.femfit.model.Assignment;
import com.femfit.model.Member;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for trainer dashboard and assignment management.
 */
public interface TrainerService {

    /**
     * Returns a list of clients assigned to the specified trainer.
     *
     * @param userIdOfTrainer the trainer's member id (members.id)
     * @return list of clients with an active order under this trainer, possibly empty
     * @throws RuntimeException if no trainer record exists for the given member id
     */
    List<Member> getClientsByTrainerUserId(long userIdOfTrainer);

    /**
     * Returns a list of clients with their orders for the specified trainer.
     *
     * @param userIdOfTrainer the trainer's member id (members.id)
     * @return list of client/order projections, possibly empty
     * @throws RuntimeException if no trainer record exists for the given member id
     */
    List<ClientOrderDto> getClientsWithOrderByTrainerUserId(long userIdOfTrainer);

    /**
     * Returns a lightweight projection (id, name, email) of all active trainers,
     * used for trainer-selection UI.
     *
     * @return list of active trainers, possibly empty
     */
    List<TrainerDto> getAllTrainers();

    /**
     * Returns all active trainers enriched with their average review rating
     * and review count. Used on the choose-trainer page so clients can see
     * ratings before picking a trainer. Trainers with no reviews show
     * averageRating = null (see {@link TrainerDto#hasRating()}).
     *
     * @return list of active trainers with rating info, possibly empty
     */
    List<TrainerDto> getAllTrainersWithRating();

    /**
     * Returns the most recently updated assignment for a client, if any.
     *
     * @param clientId the client's member id
     * @return Optional with the latest assignment, or empty if the client has none
     */
    Optional<Assignment> getAssignmentForClient(long clientId);

    /**
     * Creates a new assignment for an order, or updates the existing one
     * if an assignment for that order already exists.
     *
     * @param assignment the assignment data (orderId must be set)
     */
    void saveOrUpdateAssignment(Assignment assignment);

    /**
     * Deletes the assignment associated with the given order.
     *
     * @param orderId the order id whose assignment should be deleted
     */
    void deleteAssignment(long orderId);

    /**
     * Updates the status of an assignment (e.g. ACTIVE, COMPLETED, REVISION_REQUESTED).
     *
     * @param assignmentId the assignment id
     * @param status       the new status
     */
    void updateAssignmentStatus(Long assignmentId, String status);
}