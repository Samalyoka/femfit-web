package com.femfit.model;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Represents a client review for a completed training cycle order.
 * Maps to the {@code reviews} table.
 *
 * A review can only be submitted once per order (unique constraint on member_id + order_id).
 * Rating must be between 1 and 5.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    private Long id;
    private Long orderId;
    private Long memberId;
    private Long trainerId;

    /** Rating from 1 to 5 */
    private int rating;

    /** Optional text comment */
    private String comment;

    private LocalDateTime createdAt;

    /** Populated from JOIN — member's full name */
    private String memberName;

    /** Populated from JOIN — cycle title */
    private String cycleTitle;
    private String cycleTitleRu;
    private String cycleTitleKz;

    public String getLocalizedCycleTitle(String lang) {
        if ("ru".equals(lang) && cycleTitleRu != null && !cycleTitleRu.isBlank()) return cycleTitleRu;
        if ("kz".equals(lang) && cycleTitleKz != null && !cycleTitleKz.isBlank()) return cycleTitleKz;
        return cycleTitle;
    }
}