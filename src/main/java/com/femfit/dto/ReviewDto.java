package com.femfit.dto;

import jakarta.validation.constraints.*;

/**
 * DTO for submitting a review form.
 * Validated before saving to the database.
 */
@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ReviewDto {

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Size(max = 1000, message = "Comment must be under 1000 characters")
    private String comment;
}