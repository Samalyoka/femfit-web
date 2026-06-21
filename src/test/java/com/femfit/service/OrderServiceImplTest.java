package com.femfit.service;

import com.femfit.dao.AssignmentDao;
import com.femfit.dao.OrderDao;
import com.femfit.model.Assignment;
import com.femfit.model.Order;
import com.femfit.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OrderServiceImpl}.
 * Covers positive, negative, and edge-case scenarios for order placement,
 * assignment creation/update, and admin pagination flows.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl tests")
class OrderServiceImplTest {

    @Mock private OrderDao orderDao;
    @Mock private AssignmentDao assignmentDao;
    @InjectMocks private OrderServiceImpl orderService;

    // ── findByUserId ─────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUserId: returns orders for client")
    void findByUserId_returnsList() {
        List<Order> orders = List.of(
                Order.builder().id(1L).memberId(7L).status("ACTIVE").build(),
                Order.builder().id(2L).memberId(7L).status("COMPLETED").build()
        );
        when(orderDao.findByUserId(7L)).thenReturn(orders);

        List<Order> result = orderService.findByUserId(7L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMemberId()).isEqualTo(7L);
        verify(orderDao).findByUserId(7L);
    }

    @Test
    @DisplayName("findByUserId: returns empty list when client has no orders")
    void findByUserId_empty() {
        when(orderDao.findByUserId(99L)).thenReturn(Collections.emptyList());

        List<Order> result = orderService.findByUserId(99L);

        assertThat(result).isEmpty();
    }

    // ── findByUserId (paginated) ────────────────────────────────────────

    @Test
    @DisplayName("findByUserId(paged): delegates offset/limit to DAO and returns the page")
    void findByUserIdPaged_returnsPage() {
        List<Order> page = List.of(
                Order.builder().id(1L).memberId(7L).build(),
                Order.builder().id(2L).memberId(7L).build()
        );
        when(orderDao.findByUserId(7L, 0, 10)).thenReturn(page);

        List<Order> result = orderService.findByUserId(7L, 0, 10);

        assertThat(result).hasSize(2);
        verify(orderDao).findByUserId(7L, 0, 10);
    }

    @Test
    @DisplayName("findByUserId(paged): returns empty list for an out-of-range page")
    void findByUserIdPaged_outOfRange() {
        when(orderDao.findByUserId(7L, 100, 10)).thenReturn(Collections.emptyList());

        List<Order> result = orderService.findByUserId(7L, 100, 10);

        assertThat(result).isEmpty();
    }

    // ── countByUserId ────────────────────────────────────────────────────

    @Test
    @DisplayName("countByUserId: returns total order count for the client")
    void countByUserId_returnsCount() {
        when(orderDao.countByUserId(7L)).thenReturn(15);

        int result = orderService.countByUserId(7L);

        assertThat(result).isEqualTo(15);
    }

    @Test
    @DisplayName("countByUserId: returns 0 when client has no orders")
    void countByUserId_zero() {
        when(orderDao.countByUserId(99L)).thenReturn(0);

        int result = orderService.countByUserId(99L);

        assertThat(result).isZero();
    }

    // ── findById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById: returns order when found")
    void findById_found() {
        Order order = Order.builder().id(5L).status("ACTIVE").build();
        when(orderDao.findById(5L)).thenReturn(Optional.of(order));

        Optional<Order> result = orderService.findById(5L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("findById: returns empty when not found")
    void findById_notFound() {
        when(orderDao.findById(404L)).thenReturn(Optional.empty());

        Optional<Order> result = orderService.findById(404L);

        assertThat(result).isEmpty();
    }

    // ── findActiveByTrainerId ────────────────────────────────────────────

    @Test
    @DisplayName("findActiveByTrainerId: returns active orders for trainer")
    void findActiveByTrainerId_returnsList() {
        List<Order> orders = List.of(Order.builder().id(1L).trainerId(3L).status("ACTIVE").build());
        when(orderDao.findActiveByTrainerId(3L)).thenReturn(orders);

        List<Order> result = orderService.findActiveByTrainerId(3L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTrainerId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("findActiveByTrainerId: returns empty when trainer has no active orders")
    void findActiveByTrainerId_empty() {
        when(orderDao.findActiveByTrainerId(42L)).thenReturn(Collections.emptyList());

        List<Order> result = orderService.findActiveByTrainerId(42L);

        assertThat(result).isEmpty();
    }

    // ── findAssignment ───────────────────────────────────────────────────

    @Test
    @DisplayName("findAssignment: returns assignment for order")
    void findAssignment_found() {
        Assignment assignment = Assignment.builder().id(1L).orderId(10L).exercises("Squats").build();
        when(assignmentDao.findByOrderId(10L)).thenReturn(Optional.of(assignment));

        Optional<Assignment> result = orderService.findAssignment(10L);

        assertThat(result).isPresent();
        assertThat(result.get().getExercises()).isEqualTo("Squats");
    }

    @Test
    @DisplayName("findAssignment: returns empty when order has no assignment yet")
    void findAssignment_empty() {
        when(assignmentDao.findByOrderId(11L)).thenReturn(Optional.empty());

        Optional<Assignment> result = orderService.findAssignment(11L);

        assertThat(result).isEmpty();
    }

    // ── saveAssignment ───────────────────────────────────────────────────

    @Test
    @DisplayName("saveAssignment: creates new assignment and activates order when none exists")
    void saveAssignment_createsNew() {
        Assignment newAssignment = Assignment.builder().orderId(20L).exercises("Push-ups").build();
        when(assignmentDao.findByOrderId(20L)).thenReturn(Optional.empty());

        orderService.saveAssignment(newAssignment);

        verify(assignmentDao).save(newAssignment);
        verify(assignmentDao, never()).update(any());
        verify(orderDao).updateStatus(20L, "ACTIVE");
    }

    @Test
    @DisplayName("saveAssignment: updates existing assignment and does not re-activate order")
    void saveAssignment_updatesExisting() {
        Assignment existing = Assignment.builder().id(99L).orderId(21L).exercises("Old plan").build();
        Assignment incoming = Assignment.builder().orderId(21L).exercises("New plan").build();
        when(assignmentDao.findByOrderId(21L)).thenReturn(Optional.of(existing));

        orderService.saveAssignment(incoming);

        assertThat(incoming.getId()).isEqualTo(99L);
        verify(assignmentDao).update(incoming);
        verify(assignmentDao, never()).save(any());
        verify(orderDao, never()).updateStatus(anyLong(), anyString());
    }

    // ── updateStatus ─────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus: delegates to DAO")
    void updateStatus_delegates() {
        orderService.updateStatus(15L, "COMPLETED");
        verify(orderDao).updateStatus(15L, "COMPLETED");
    }

    // ── requestRevision ──────────────────────────────────────────────────

    @Test
    @DisplayName("requestRevision: sets assignment status to REVISION_REQUESTED")
    void requestRevision_setsStatus() {
        orderService.requestRevision(30L);
        verify(assignmentDao).updateStatus(30L, "REVISION_REQUESTED");
    }

    // ── placeOrder ───────────────────────────────────────────────────────

    @Test
    @DisplayName("placeOrder: builds order with ACTIVE status and saves it")
    void placeOrder_success() {
        BigDecimal price = new BigDecimal("49900.00");
        Order saved = Order.builder().id(50L).memberId(1L).cycleId(2).trainerId(3L)
                .paidAmount(price).status("ACTIVE").build();
        when(orderDao.save(any(Order.class))).thenReturn(saved);

        Order result = orderService.placeOrder(1L, 2, price, 3L);

        assertThat(result.getId()).isEqualTo(50L);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderDao).save(captor.capture());
        Order passed = captor.getValue();
        assertThat(passed.getMemberId()).isEqualTo(1L);
        assertThat(passed.getCycleId()).isEqualTo(2);
        assertThat(passed.getTrainerId()).isEqualTo(3L);
        assertThat(passed.getPaidAmount()).isEqualTo(price);
        assertThat(passed.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("placeOrder: works when trainerId is null (buy without trainer)")
    void placeOrder_withoutTrainer() {
        BigDecimal price = new BigDecimal("29900.00");
        Order saved = Order.builder().id(51L).memberId(1L).cycleId(4).trainerId(null)
                .paidAmount(price).status("ACTIVE").build();
        when(orderDao.save(any(Order.class))).thenReturn(saved);

        Order result = orderService.placeOrder(1L, 4, price, null);

        assertThat(result.getTrainerId()).isNull();

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderDao).save(captor.capture());
        assertThat(captor.getValue().getTrainerId()).isNull();
    }

    // ── findAll / countAll (admin pagination) ───────────────────────────

    @Test
    @DisplayName("findAll: returns a page of orders using offset and limit")
    void findAll_returnsPage() {
        List<Order> page = List.of(
                Order.builder().id(1L).build(),
                Order.builder().id(2L).build()
        );
        when(orderDao.findAll(0, 10)).thenReturn(page);

        List<Order> result = orderService.findAll(0, 10);

        assertThat(result).hasSize(2);
        verify(orderDao).findAll(0, 10);
    }

    @Test
    @DisplayName("findAll: returns empty list for an out-of-range page")
    void findAll_outOfRange() {
        when(orderDao.findAll(1000, 10)).thenReturn(Collections.emptyList());

        List<Order> result = orderService.findAll(1000, 10);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("countAll: returns total order count from DAO")
    void countAll_returnsCount() {
        when(orderDao.countAll()).thenReturn(22);

        int result = orderService.countAll();

        assertThat(result).isEqualTo(22);
    }

    @Test
    @DisplayName("countAll: returns 0 when there are no orders")
    void countAll_zero() {
        when(orderDao.countAll()).thenReturn(0);

        int result = orderService.countAll();

        assertThat(result).isZero();
    }
}