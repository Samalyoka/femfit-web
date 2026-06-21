package com.femfit.service;

import com.femfit.model.Assignment;
import com.femfit.model.Order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for order and assignment business logic.
 */
public interface OrderService {

    /**
     * Returns all orders for a client.
     *
     * @param userId client's user id
     * @return list of orders
     */
    List<Order> findByUserId(Long userId);

    /**
     * Returns a single page of orders for a client, most recent first.
     *
     * @param userId client's user id
     * @param offset SQL offset
     * @param limit  page size
     * @return list of orders for this page
     */
    List<Order> findByUserId(Long userId, int offset, int limit);

    /**
     * Counts total orders for a client (for client-side pagination).
     *
     * @param userId client's user id
     * @return total number of orders placed by this client
     */
    int countByUserId(Long userId);

    /**
     * Finds an order by id.
     *
     * @param orderId order id
     * @return Optional with order
     */
    Optional<Order> findById(Long orderId);

    /**
     * Returns active orders for a trainer.
     *
     * @param trainerId trainer's user id
     * @return list of active orders
     */
    List<Order> findActiveByTrainerId(Long trainerId);

    /**
     * Finds assignment for an order.
     *
     * @param orderId order id
     * @return Optional with assignment
     */
    Optional<Assignment> findAssignment(Long orderId);

    /**
     * Creates or updates an assignment for an order (trainer function).
     *
     * @param assignment the assignment data
     */
    void saveAssignment(Assignment assignment);

    /**
     * Updates order status.
     *
     * @param orderId order id
     * @param status  new status
     */
    void updateStatus(Long orderId, String status);

    /**
     * Records a client's request to revise specific part(s) of an
     * assignment (exercises, equipment, nutrition, schedule — any
     * combination), with an optional comment explaining what's needed.
     * Sets the assignment's status to REVISION_REQUESTED.
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

    /**
     * Places a new order for a training cycle.
     *
     * @param userId  client's user id
     * @param cycleId training cycle id
     * @param price   amount to pay
     * @param trainerId the trainer's user id
     * @return saved order
     */
    Order placeOrder(Long userId, Integer cycleId, BigDecimal price, Long trainerId);

    /**
     * Returns all orders with pagination (admin).
     *
     * @param offset SQL offset
     * @param limit  page size
     * @return list of orders
     */
    List<Order> findAll(int offset, int limit);

    /**
     * Counts total orders (admin pagination).
     *
     * @return total number of orders
     */
    int countAll();

    /**
     * Reassigns an order to a different trainer (admin function). Used when
     * the order's primary trainer is unavailable (vacation, sick leave) and
     * an admin reassigns the client to another available trainer. Also sets
     * the order's status to ACTIVE.
     *
     * @param orderId   the order to reassign
     * @param trainerId the new trainer's id
     */
    void assignTrainer(Long orderId, Long trainerId);
}