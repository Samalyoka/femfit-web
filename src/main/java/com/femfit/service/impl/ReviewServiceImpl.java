package com.femfit.service.impl;

import com.femfit.dao.ReviewDao;
import com.femfit.dto.ReviewDto;
import com.femfit.model.Review;
import com.femfit.service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link ReviewService}.
 *
 * Handles submission and retrieval of client reviews for completed programmes.
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ReviewDao reviewDao;

    @Autowired
    public ReviewServiceImpl(ReviewDao reviewDao) {
        this.reviewDao = reviewDao;
    }

    /**
     * Submits a review for a completed order.
     * Prevents duplicate reviews via DB unique constraint.
     *
     * @param dto       the review form data
     * @param orderId   the order being reviewed
     * @param memberId  the member submitting the review
     * @param trainerId the trainer assigned to the order
     */
    @Override
    public void submitReview(ReviewDto dto, Long orderId, Long memberId, Long trainerId) {
        Review review = Review.builder()
                .orderId(orderId)
                .memberId(memberId)
                .trainerId(trainerId)
                .rating(dto.getRating())
                .comment(dto.getComment())
                .build();
        reviewDao.save(review);
        log.info("Review submitted: orderId={}, memberId={}, rating={}", orderId, memberId, dto.getRating());
    }

    /**
     * Finds a review by order ID.
     *
     * @param orderId the order ID
     * @return Optional containing the review if found
     */
    @Override
    public Optional<Review> findByOrderId(Long orderId) {
        return reviewDao.findByOrderId(orderId);
    }

    /**
     * Checks if a review already exists for an order.
     *
     * @param orderId the order ID
     * @return true if review exists
     */
    @Override
    public boolean hasReview(Long orderId) {
        return reviewDao.existsByOrderId(orderId);
    }

    /**
     * Returns recent reviews for public display on the home page.
     *
     * @param limit max number of reviews to return
     * @return list of recent reviews
     */
    @Override
    public List<Review> getRecentReviews(int limit) {
        return reviewDao.findRecentForDisplay(limit);
    }
}