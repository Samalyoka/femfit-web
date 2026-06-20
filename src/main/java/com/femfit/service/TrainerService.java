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

    List<Member> getClientsByTrainerUserId(long userIdOfTrainer);

    List<ClientOrderDto> getClientsWithOrderByTrainerUserId(long userIdOfTrainer);

    List<TrainerDto> getAllTrainers();

    List<TrainerDto> getAllTrainersWithRating();

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

    void saveOrUpdateAssignment(Assignment assignment);

    void deleteAssignment(long orderId);

    void updateAssignmentStatus(Long assignmentId, String status);
}