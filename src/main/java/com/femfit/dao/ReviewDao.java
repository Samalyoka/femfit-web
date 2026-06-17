package com.femfit.dao;

import com.femfit.model.Review;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Review operations.
 *
 * Reviews can only be submitted for COMPLETED orders,
 * one review per member per order (enforced by DB unique constraint).
 */
public interface ReviewDao {

    /**
     * Saves a new review to the database.
     *
     * @param review the review to save
     * @return the saved review with id and createdAt populated
     */
    Review save(Review review);

    /**
     * Finds a review by order ID.
     * Used to check if a review already exists for an order.
     *
     * @param orderId the order ID
     * @return Optional containing the review if found, empty otherwise
     */
    Optional<Review> findByOrderId(Long orderId);

    /**
     * Finds all reviews submitted by a specific member.
     *
     * @param memberId the member ID
     * @return list of reviews, ordered by most recent first
     */
    List<Review> findByMemberId(Long memberId);

    /**
     * Finds all reviews for public display on the home page.
     * Results are ordered by most recent first.
     *
     * @param limit maximum number of reviews to return
     * @return list of recent reviews with member name and cycle title
     */
    List<Review> findRecentForDisplay(int limit);

    /**
     * Checks if a review already exists for a given order.
     *
     * @param orderId the order ID
     * @return true if a review exists, false otherwise
     */
    boolean existsByOrderId(Long orderId);
}