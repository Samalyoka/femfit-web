package com.femfit.service;

import com.femfit.dto.ClientOrderDto;
import com.femfit.dto.TrainerDto;
import com.femfit.model.Assignment;
import com.femfit.model.Member;

import java.util.List;
import java.util.Optional;

public interface TrainerService {

    /** Returns a list of clients assigned to the specified trainer. */
    List<Member> getClientsByTrainerUserId(long userIdOfTrainer);

    /** Returns a list of clients with their orders for the specified trainer. */
    List<ClientOrderDto> getClientsWithOrderByTrainerUserId(long userIdOfTrainer);

    /**
     * Returns a lightweight projection (id, name, email) of all active trainers,
     * used for trainer-selection UI.
     */
    List<TrainerDto> getAllTrainers();

    Optional<Assignment> getAssignmentForClient(long clientId);

    void saveOrUpdateAssignment(Assignment assignment);

    /** Deletes an assignment by its ID. */
    void deleteAssignment(long orderId);

    void updateAssignmentStatus(Long assignmentId, String status);
}