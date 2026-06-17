package com.femfit.service.impl;

import com.femfit.dao.AssignmentDao;
import com.femfit.dao.TrainerDao;
import com.femfit.dto.ClientOrderDto;
import com.femfit.dto.TrainerDto;
import com.femfit.model.Assignment;
import com.femfit.model.Member;
import com.femfit.service.TrainerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link TrainerService}.
 *
 * Provides business logic for trainer operations including:
 * - Retrieving clients assigned to a trainer
 * - Managing training assignments
 * - Listing all available trainers
 */
@Service
public class TrainerServiceImpl implements TrainerService {

    private static final Logger log = LoggerFactory.getLogger(TrainerServiceImpl.class);

    private final TrainerDao trainerDao;
    private final AssignmentDao assignmentDao;

    @Autowired
    public TrainerServiceImpl(TrainerDao trainerDao, AssignmentDao assignmentDao) {
        this.trainerDao = trainerDao;
        this.assignmentDao = assignmentDao;
    }

    /**
     * Retrieves all clients assigned to a trainer.
     * Resolves the trainer ID from the user ID, then fetches their clients.
     *
     * @param userId the trainer's user ID
     * @return list of Member objects representing the trainer's clients
     */
    @Override
    public List<Member> getClientsByTrainerUserId(long userId) {
        long trainerId = trainerDao.findTrainerIdByUserId(userId);
        return trainerDao.findClientsByTrainerId(trainerId);
    }

    /**
     * Retrieves all clients assigned to a trainer with their order details.
     * Returns a DTO combining client info with their most recent order.
     *
     * @param userId the trainer's user ID
     * @return list of ClientOrderDto objects
     */
    @Override
    public List<ClientOrderDto> getClientsWithOrderByTrainerUserId(long userId) {
        long trainerId = trainerDao.findTrainerIdByUserId(userId);
        return trainerDao.findClientsWithOrderByTrainerId(trainerId);
    }

    /**
     * Retrieves the most recent assignment for a client.
     *
     * @param clientId the client/member ID
     * @return Optional containing the latest assignment if found, empty otherwise
     */
    @Override
    public Optional<Assignment> getAssignmentForClient(long clientId) {
        return assignmentDao.findLatestByClientId(clientId);
    }

    /**
     * Saves a new assignment or updates an existing one.
     * Checks if an assignment already exists for the order; if so, updates it;
     * otherwise, creates a new one.
     *
     * @param assignment the assignment to save or update
     */
    @Override
    public void saveOrUpdateAssignment(Assignment assignment) {
        Optional<Assignment> existing = assignmentDao.findByOrderId(assignment.getOrderId());
        if (existing.isPresent()) {
            assignment.setId(existing.get().getId());
            assignmentDao.update(assignment);
            log.debug("Assignment updated: orderId={}", assignment.getOrderId());
        } else {
            assignmentDao.save(assignment);
            log.debug("Assignment created: orderId={}", assignment.getOrderId());
        }
    }

    /**
     * Deletes an assignment for a given order.
     * Typically called when an order is cancelled.
     *
     * @param orderId the order ID
     */
    @Override
    public void deleteAssignment(long orderId) {
        assignmentDao.deleteByOrderId(orderId);
        log.info("Assignment deleted: orderId={}", orderId);
    }

    /**
     * Updates the status of an assignment.
     *
     * @param assignmentId the assignment ID
     * @param status the new status (e.g., 'COMPLETED', 'REVISION_REQUESTED')
     */
    @Override
    public void updateAssignmentStatus(Long assignmentId, String status) {
        assignmentDao.updateStatus(assignmentId, status);
        log.debug("Assignment status set: id={}, status={}", assignmentId, status);
    }

    /**
     * Retrieves all active trainers in the system.
     * Used for trainer selection during order placement.
     *
     * @return list of all active TrainerDto objects
     */
    @Override
    public List<TrainerDto> getAllTrainers() {
        return trainerDao.findAllTrainers();
    }
}