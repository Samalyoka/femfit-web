package com.femfit.dto;

import com.femfit.model.TrainerAvailability;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Projection for trainer selection (e.g. choose-trainer page).
 * Exposes only the fields a client needs to see when picking a trainer —
 * does not leak password hashes, role, discount, or other Member-only fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerDto {

    private long id;
    private String firstName;
    private String lastName;
    private String email;
    private TrainerAvailability availabilityStatus;

    /**
     * Average review rating (1-5) across all this trainer's completed orders.
     * Null if the trainer has no reviews yet.
     */
    private Double averageRating;

    /** Number of reviews behind the averageRating, for display ("4.8 (12 reviews)"). */
    private Integer reviewCount;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    /** True if this trainer has at least one review. */
    public boolean hasRating() {
        return averageRating != null && reviewCount != null && reviewCount > 0;
    }

    /** True if this trainer is currently available to take on new assignments. */
    public boolean isAvailable() {
        return availabilityStatus == null || availabilityStatus == TrainerAvailability.AVAILABLE;
    }
}