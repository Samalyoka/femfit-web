package com.femfit.controller;

import com.femfit.dto.ReviewDto;
import com.femfit.model.Member;
import com.femfit.model.Order;
import com.femfit.service.MemberService;
import com.femfit.service.OrderService;
import com.femfit.service.ReviewService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles client review submission for completed training programmes.
 *
 * Reviews can only be submitted for COMPLETED orders.
 * Each order can only have one review (enforced at DB level).
 */
@Controller
@RequestMapping("/client/review")
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService reviewService;
    private final OrderService orderService;
    private final MemberService memberService;

    @Autowired
    public ReviewController(ReviewService reviewService,
                            OrderService orderService,
                            MemberService memberService) {
        this.reviewService = reviewService;
        this.orderService = orderService;
        this.memberService = memberService;
    }

    /**
     * Shows the review form for a completed order.
     * Redirects if order is not completed or review already exists.
     *
     * @param orderId the order to review
     */
    @GetMapping("/{orderId}")
    public String reviewForm(@PathVariable Long orderId,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model,
                             RedirectAttributes redirectAttrs) {
        Member member = getUser(userDetails);

        // Check order exists and belongs to this member
        Order order = orderService.findById(orderId).orElse(null);
        if (order == null || !order.getMemberId().equals(member.getId())) {
            redirectAttrs.addFlashAttribute("error", "msg.error.not.found");
            return "redirect:/client/orders";
        }

        // Only COMPLETED orders can be reviewed
        if (!"COMPLETED".equals(order.getStatus())) {
            redirectAttrs.addFlashAttribute("error", "review.error.not.completed");
            return "redirect:/client/orders";
        }

        // Already reviewed
        if (reviewService.hasReview(orderId)) {
            redirectAttrs.addFlashAttribute("error", "review.error.already.submitted");
            return "redirect:/client/orders";
        }

        model.addAttribute("order", order);
        model.addAttribute("reviewDto", new ReviewDto());
        return "client/review";
    }

    /**
     * Processes review form submission.
     * Validates rating (1-5) and saves the review.
     *
     * @param orderId   the order being reviewed
     * @param dto       the review form data
     * @param bindingResult validation results
     */
    @PostMapping("/{orderId}")
    public String submitReview(@PathVariable Long orderId,
                               @Valid @ModelAttribute("reviewDto") ReviewDto dto,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttrs,
                               Model model) {
        Member member = getUser(userDetails);

        if (bindingResult.hasErrors()) {
            Order order = orderService.findById(orderId).orElse(null);
            model.addAttribute("order", order);
            return "client/review";
        }

        Order order = orderService.findById(orderId).orElse(null);
        if (order == null || !order.getMemberId().equals(member.getId())) {
            redirectAttrs.addFlashAttribute("error", "msg.error.not.found");
            return "redirect:/client/orders";
        }

        reviewService.submitReview(dto, orderId, member.getId(), order.getTrainerId());
        log.info("Review submitted by memberId={} for orderId={}", member.getId(), orderId);

        redirectAttrs.addFlashAttribute("success", "review.success.submitted");
        return "redirect:/client/orders";
    }

    private Member getUser(UserDetails userDetails) {
        return memberService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Member not found"));
    }
}