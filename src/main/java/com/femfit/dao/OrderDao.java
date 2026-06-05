package com.femfit.dao;

import com.femfit.model.Order;

import java.util.List;
import java.util.Optional;

/**
 * DAO interface for {@link Order} entity.
 */
public interface OrderDao {

    /**
     * Saves a new order. Uses JDBC transaction for data integrity.
     *
     * @param order the order to save
     * @return saved order with generated id
     */
    Order save(Order order);

    /**
     * Finds an order by id with joined client, trainer and cycle info.
     *
     * @param id order id
     * @return Optional with order or empty
     */
    Optional<Order> findById(Long id);

    /**
     * Returns all orders for a client.
     *
     * @param userId client's user id
     * @return list of orders
     */
    List<Order> findByUserId(Long userId);

    /**
     * Returns all active orders assigned to a trainer.
     *
     * @param trainerId trainer's user id
     * @return list of active orders
     */
    List<Order> findActiveByTrainerId(Long trainerId);

    /**
     * Returns all orders with pagination (for admin).
     *
     * @param offset SQL offset
     * @param limit  page size
     * @return list of orders
     */
    List<Order> findAll(int offset, int limit);

    /**
     * Counts total orders (for admin pagination).
     *
     * @return total count
     */
    int countAll();

    /**
     * Updates order status.
     *
     * @param orderId order id
     * @param status  new status: PENDING, ACTIVE, COMPLETED, CANCELLED
     */
    void updateStatus(Long orderId, String status);

    /**
     * Assigns a trainer to an order.
     *
     * @param orderId   order id
     * @param trainerId trainer's user id
     */
    void assignTrainer(Long orderId, Long trainerId);
}