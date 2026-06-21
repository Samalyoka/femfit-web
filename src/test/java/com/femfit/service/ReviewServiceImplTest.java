package com.femfit.service;

import com.femfit.dao.ReviewDao;
import com.femfit.dto.ReviewDto;
import com.femfit.model.Review;
import com.femfit.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ReviewServiceImpl}.
 * Covers positive, negative, and boundary scenarios for review submission
 * and retrieval (rating range 1-5, duplicate-review checks).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewServiceImpl tests")
class ReviewServiceImplTest {

    @Mock private ReviewDao reviewDao;
    @InjectMocks private ReviewServiceImpl reviewService;

    // ── submitReview ─────────────────────────────────────────────────────

    @Test
    @DisplayName("submitReview: builds review from DTO and saves it")
    void submitReview_success() {
        ReviewDto dto = new ReviewDto(5, "Incredible energy, intense but rewarding workouts.");

        reviewService.submitReview(dto, 10L, 7L, 3L);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewDao).save(captor.capture());
        Review saved = captor.getValue();

        assertThat(saved.getOrderId()).isEqualTo(10L);
        assertThat(saved.getMemberId()).isEqualTo(7L);
        assertThat(saved.getTrainerId()).isEqualTo(3L);
        assertThat(saved.getRating()).isEqualTo(5);
        assertThat(saved.getComment()).isEqualTo("Incredible energy, intense but rewarding workouts.");
    }

    @Test
    @DisplayName("submitReview: boundary rating value 1 (minimum) is passed through unchanged")
    void submitReview_boundaryRatingMin() {
        ReviewDto dto = new ReviewDto(1, "Not for me.");

        reviewService.submitReview(dto, 11L, 8L, null);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewDao).save(captor.capture());
        assertThat(captor.getValue().getRating()).isEqualTo(1);
    }

    @Test
    @DisplayName("submitReview: boundary rating value 5 (maximum) is passed through unchanged")
    void submitReview_boundaryRatingMax() {
        ReviewDto dto = new ReviewDto(5, "Perfect.");

        reviewService.submitReview(dto, 12L, 9L, 4L);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewDao).save(captor.capture());
        assertThat(captor.getValue().getRating()).isEqualTo(5);
    }

    @Test
    @DisplayName("submitReview: works when trainerId is null (order had no assigned trainer)")
    void submitReview_nullTrainer() {
        ReviewDto dto = new ReviewDto(4, "Good overall.");

        reviewService.submitReview(dto, 13L, 8L, null);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewDao).save(captor.capture());
        assertThat(captor.getValue().getTrainerId()).isNull();
    }

    @Test
    @DisplayName("submitReview: works when comment is null (optional field)")
    void submitReview_nullComment() {
        ReviewDto dto = new ReviewDto(3, null);

        reviewService.submitReview(dto, 14L, 8L, 2L);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewDao).save(captor.capture());
        assertThat(captor.getValue().getComment()).isNull();
    }

    // ── findByOrderId ────────────────────────────────────────────────────

    @Test
    @DisplayName("findByOrderId: returns review when one exists for the order")
    void findByOrderId_found() {
        Review review = Review.builder().id(1L).orderId(20L).rating(5).build();
        when(reviewDao.findByOrderId(20L)).thenReturn(Optional.of(review));

        Optional<Review> result = reviewService.findByOrderId(20L);

        assertThat(result).isPresent();
        assertThat(result.get().getRating()).isEqualTo(5);
    }

    @Test
    @DisplayName("findByOrderId: returns empty when no review exists yet")
    void findByOrderId_empty() {
        when(reviewDao.findByOrderId(21L)).thenReturn(Optional.empty());

        Optional<Review> result = reviewService.findByOrderId(21L);

        assertThat(result).isEmpty();
    }

    // ── hasReview ────────────────────────────────────────────────────────

    @Test
    @DisplayName("hasReview: returns true when a review already exists for the order")
    void hasReview_true() {
        when(reviewDao.existsByOrderId(30L)).thenReturn(true);

        boolean result = reviewService.hasReview(30L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasReview: returns false when no review exists for the order")
    void hasReview_false() {
        when(reviewDao.existsByOrderId(31L)).thenReturn(false);

        boolean result = reviewService.hasReview(31L);

        assertThat(result).isFalse();
    }

    // ── getRecentReviews ─────────────────────────────────────────────────

    @Test
    @DisplayName("getRecentReviews: returns reviews up to the requested limit")
    void getRecentReviews_returnsList() {
        List<Review> reviews = List.of(
                Review.builder().id(1L).rating(5).memberName("Sammy Sammy").build(),
                Review.builder().id(2L).rating(4).memberName("Assem Abc").build()
        );
        when(reviewDao.findRecentForDisplay(3)).thenReturn(reviews);

        List<Review> result = reviewService.getRecentReviews(3);

        assertThat(result).hasSize(2);
        verify(reviewDao).findRecentForDisplay(3);
    }

    @Test
    @DisplayName("getRecentReviews: returns empty list when there are no reviews yet")
    void getRecentReviews_empty() {
        when(reviewDao.findRecentForDisplay(3)).thenReturn(Collections.emptyList());

        List<Review> result = reviewService.getRecentReviews(3);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getRecentReviews: limit of 0 is passed through to DAO unchanged")
    void getRecentReviews_zeroLimit() {
        when(reviewDao.findRecentForDisplay(0)).thenReturn(Collections.emptyList());

        List<Review> result = reviewService.getRecentReviews(0);

        assertThat(result).isEmpty();
        verify(reviewDao).findRecentForDisplay(0);
    }
}