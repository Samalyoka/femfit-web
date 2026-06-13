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
     * Marks an assignment as revision requested by client.
     *
     * @param assignmentId the assignment id
     */
    void requestRevision(Long assignmentId);

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
}