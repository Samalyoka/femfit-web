package com.femfit.service;

import com.femfit.dto.ReviewDto;
import com.femfit.model.Review;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing client reviews of completed training programmes.
 */
public interface ReviewService {

    /**
     * Submits a review for a completed order.
     *
     * @param dto      the review form data (rating + comment)
     * @param orderId  the order being reviewed
     * @param memberId the member submitting the review
     * @param trainerId the trainer assigned to the order (may be null)
     */
    void submitReview(ReviewDto dto, Long orderId, Long memberId, Long trainerId);

    /**
     * Finds a review for a specific order.
     *
     * @param orderId the order ID
     * @return Optional containing the review if found
     */
    Optional<Review> findByOrderId(Long orderId);

    /**
     * Checks if a review already exists for an order.
     *
     * @param orderId the order ID
     * @return true if review exists
     */
    boolean hasReview(Long orderId);

    /**
     * Finds recent reviews for public display (home page).
     *
     * @param limit max number of reviews
     * @return list of recent reviews
     */
    List<Review> getRecentReviews(int limit);
}
