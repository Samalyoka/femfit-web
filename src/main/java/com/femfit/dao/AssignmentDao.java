package com.femfit.dao;

import com.femfit.model.Assignment;
import java.util.Optional;

public interface AssignmentDao {

    /** Save a new assignment or update an existing one.*/
    Assignment save(Assignment assignment);

    /** Find the latest assignment for a given order ID. Returns empty if no assignment exists for that order.*/
    Optional<Assignment> findByOrderId(Long orderId);

    /**
     * Find the latest assignment for a given client ID. Returns empty if no assignment exists for that client.
     */
    Optional<Assignment> findLatestByClientId(long clientId);

    /** Update the content (exercises, equipment, nutrition_plan, schedule_info). */
    void update(Assignment assignment);

    /** Update only the status. */
    void updateStatus(Long assignmentId, String status);

    /** Delete all assignments for an order. */
    void deleteByOrderId(Long orderId);
}