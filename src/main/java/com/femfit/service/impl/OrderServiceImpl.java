package com.femfit.service.impl;

import com.femfit.dao.AssignmentDao;
import com.femfit.dao.OrderDao;
import com.femfit.model.Assignment;
import com.femfit.model.Order;
import com.femfit.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link OrderService}.
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderDao orderDao;
    private final AssignmentDao assignmentDao;

    @Autowired
    public OrderServiceImpl(OrderDao orderDao, AssignmentDao assignmentDao) {
        this.orderDao = orderDao;
        this.assignmentDao = assignmentDao;
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        return orderDao.findByUserId(userId);
    }

    @Override
    public Optional<Order> findById(Long orderId) {
        return orderDao.findById(orderId);
    }

    @Override
    public List<Order> findActiveByTrainerId(Long trainerId) {
        return orderDao.findActiveByTrainerId(trainerId);
    }

    @Override
    public Optional<Assignment> findAssignment(Long orderId) {
        return assignmentDao.findByOrderId(orderId);
    }

    @Override
    public void saveAssignment(Assignment assignment) {
        Optional<Assignment> existing = assignmentDao.findByOrderId(assignment.getOrderId());
        if (existing.isPresent()) {
            assignment.setId(existing.get().getId());
            assignmentDao.update(assignment);
            log.info("Assignment updated for order id={}", assignment.getOrderId());
        } else {
            assignmentDao.save(assignment);
            // Activate the order when assignment is first created
            orderDao.updateStatus(assignment.getOrderId(), "ACTIVE");
            log.info("Assignment created for order id={}", assignment.getOrderId());
        }
    }

    @Override
    public void updateStatus(Long orderId, String status) {
        orderDao.updateStatus(orderId, status);
    }

    @Override
    public void requestRevision(Long assignmentId) {
        assignmentDao.updateStatus(assignmentId, "REVISION_REQUESTED");
        log.info("Revision requested for assignment id={}", assignmentId);
    }

    @Override
    public Order placeOrder(Long userId, Integer cycleId, java.math.BigDecimal price) {
        Order order = Order.builder()
                .userId(userId)
                .cycleId(cycleId)
                .paidAmount(price)
                .status("PENDING")
                .build();
        Order saved = orderDao.save(order);
        log.info("Order placed: userId={}, cycleId={}, orderId={}", userId, cycleId, saved.getId());
        return saved;
    }
}