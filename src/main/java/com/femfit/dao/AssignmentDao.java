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

    /**
     * Records a per-item revision request: sets which specific part(s) of
     * the assignment need to be redone, stores the client's comment, and
     * sets status to REVISION_REQUESTED. Replaces any previous revision
     * request on this assignment.
     *
     * @param assignmentId the assignment id
     * @param exercises    true if exercises need revision
     * @param equipment    true if equipment needs revision
     * @param nutrition    true if nutrition plan needs revision
     * @param schedule     true if schedule needs revision
     * @param comment      optional client comment explaining the request
     */
    void requestRevision(Long assignmentId, boolean exercises, boolean equipment,
                          boolean nutrition, boolean schedule, String comment);

    /** Delete all assignments for an order. */
    void deleteByOrderId(Long orderId);
}