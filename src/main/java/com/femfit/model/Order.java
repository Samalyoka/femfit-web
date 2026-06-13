package com.femfit.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a client order for a training cycle.
 * Associative entity linking User (client), TrainingCycle, and Trainer.
 * Maps to the {@code orders} table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    private Long id;
    private Long memberId;
    private Integer cycleId;
    private Long trainerId;

    /** PENDING, ACTIVE, COMPLETED, CANCELLED */
    private String status;

    private BigDecimal paidAmount;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String clientName;
    private String trainerName;
    private String cycleTitle;
}