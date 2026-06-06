package com.femfit.service;

import com.femfit.dto.ClientOrderDto;
import com.femfit.model.Assignment;
import com.femfit.model.User;
import java.util.List;
import java.util.Optional;

public interface TrainerService {

    /** Старый метод — оставляем для обратной совместимости. */
    List<User> getClientsByTrainerUserId(long userIdOfTrainer);

    /** Новый метод — для dashboard, несёт orderId. */
    List<ClientOrderDto> getClientsWithOrderByTrainerUserId(long userIdOfTrainer);

    Optional<Assignment> getAssignmentForClient(long clientId);

    void saveOrUpdateAssignment(Assignment assignment);

    /** @param orderId — FK в таблице assignments, не clientId */
    void deleteAssignment(long orderId);

    void updateAssignmentStatus(Long assignmentId, String status);
}