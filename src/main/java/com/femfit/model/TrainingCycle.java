package com.femfit.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a training program available for purchase.
 * Maps to the {@code training_cycles} table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingCycle {

    private Integer id;
    private String title;
    private String description;
    private Integer durationWeeks;
    private BigDecimal price;
    private boolean active;
    private LocalDateTime createdAt;
}