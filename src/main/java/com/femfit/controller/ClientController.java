package com.femfit.controller;

import com.femfit.exception.BookingException;
import com.femfit.model.Booking;
import com.femfit.model.Order;
import com.femfit.model.User;
import com.femfit.service.BookingService;
import com.femfit.service.OrderService;
import com.femfit.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.femfit.model.TrainingCycle;
import com.femfit.service.TrainingCycleService;

import java.util.List;

/**
 * Handles all client-specific actions:
 * profile, bookings, orders, assignments.
 */
@Controller
@RequestMapping("/client")
public class ClientController {

    private static final Logger log = LoggerFactory.getLogger(ClientController.class);

    private final UserService userService;
    private final BookingService bookingService;
    private final OrderService orderService;
    private final TrainingCycleService trainingCycleService;

    @Autowired
    public ClientController(UserService userService,
                            BookingService bookingService,
                            OrderService orderService,
                            TrainingCycleService trainingCycleService) {
        this.userService = userService;
        this.bookingService = bookingService;
        this.orderService = orderService;
        this.trainingCycleService = trainingCycleService;
    }

    /**
     * Client profile page with stats and upcoming bookings.
     */
    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getUser(userDetails);
        List<Booking> bookings = bookingService.getUpcoming(user.getId());
        List<Order> orders = orderService.findByUserId(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("bookings", bookings);
        model.addAttribute("orders", orders);
        model.addAttribute("visitCount", bookingService.countVisitsThisMonth(user.getId()));
        return "client/profile";
    }

    /**
     * Books a class for the current user.
     *
     * @param scheduleId the schedule slot to book
     */
    @PostMapping("/book/{scheduleId}")
    public String book(@PathVariable Long scheduleId,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirectAttrs) {
        User user = getUser(userDetails);
        try {
            bookingService.book(user.getId(), scheduleId);
            redirectAttrs.addFlashAttribute("success", "msg.success.booking");
        } catch (BookingException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/schedule";
    }

    /**
     * Cancels a booking.
     *
     * @param bookingId the booking to cancel
     */
    @PostMapping("/booking/cancel/{bookingId}")
    public String cancelBooking(@PathVariable Long bookingId,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttrs) {
        User user = getUser(userDetails);
        bookingService.cancel(bookingId, user.getId());
        redirectAttrs.addFlashAttribute("success", "msg.success.cancel");
        return "redirect:/client/profile";
    }

    /**
     * Client's orders list.
     */
    @GetMapping("/orders")
    public String orders(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getUser(userDetails);
        model.addAttribute("orders", orderService.findByUserId(user.getId()));
        return "client/orders";
    }

    /**
     * View assignment for a specific order.
     */
    @GetMapping("/assignment/{orderId}")
    public String assignment(@PathVariable Long orderId,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model) {
        User user = getUser(userDetails);
        orderService.findById(orderId).ifPresent(order -> {
            model.addAttribute("order", order);
            orderService.findAssignment(orderId).ifPresent(a ->
                    model.addAttribute("assignment", a));
        });
        return "client/assignment";
    }

    /**
     * Client requests a revision on their assignment.
     *
     * @param assignmentId the assignment id
     */
    @PostMapping("/assignment/revision/{assignmentId}")
    public String requestRevision(@PathVariable Long assignmentId,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttrs) {
        log.info("Revision requested for assignment id={}", assignmentId);
        orderService.requestRevision(assignmentId);
        redirectAttrs.addFlashAttribute("success", "Revision request sent to your trainer.");
        return "redirect:/client/profile";
    }

    // Helper — loads full User from DB using Spring Security email
    private User getUser(UserDetails userDetails) {
        return userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Shows all available training cycles for purchase.
     */
    @GetMapping("/cycles")
    public String cycles(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getUser(userDetails);
        List<TrainingCycle> cycles = trainingCycleService.findAllActive();
        List<Order> myOrders = orderService.findByUserId(user.getId());
        // передаём id уже купленных циклов чтобы показать статус
        List<Integer> purchasedCycleIds = myOrders.stream()
                .map(Order::getCycleId)
                .toList();
        model.addAttribute("cycles", cycles);
        model.addAttribute("purchasedCycleIds", purchasedCycleIds);
        return "client/cycles";
    }

    /**
     * Places an order for a training cycle.
     */
    @PostMapping("/cycles/{cycleId}/order")
    public String placeOrder(@PathVariable Integer cycleId,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttrs) {
        User user = getUser(userDetails);
        trainingCycleService.findById(cycleId).ifPresentOrElse(
                cycle -> {
                    orderService.placeOrder(user.getId(), cycleId, cycle.getPrice());
                    redirectAttrs.addFlashAttribute("success", "Order placed successfully!");
                },
                () -> redirectAttrs.addFlashAttribute("error", "Training cycle not found.")
        );
        return "redirect:/client/orders";
    }
}