package com.femfit.dao;

import com.femfit.model.Assignment;

import java.util.Optional;

/**
 * DAO interface for {@link Assignment} entity.
 */
public interface AssignmentDao {

    /**
     * Saves a new assignment.
     *
     * @param assignment the assignment to save
     * @return saved assignment with generated id
     */
    Assignment save(Assignment assignment);

    /**
     * Finds assignment by order id.
     *
     * @param orderId the order id
     * @return Optional with assignment or empty
     */
    Optional<Assignment> findByOrderId(Long orderId);

    /**
     * Updates an existing assignment.
     *
     * @param assignment the assignment with updated fields
     */
    void update(Assignment assignment);

    /**
     * Updates assignment status.
     *
     * @param assignmentId assignment id
     * @param status       new status: ACTIVE, COMPLETED, REVISION_REQUESTED
     */
    void updateStatus(Long assignmentId, String status);
}