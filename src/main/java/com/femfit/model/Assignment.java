package com.femfit.model;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Weak entity — training assignment created by a trainer for a client order.
 * Maps to the {@code assignments} table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment {

    private Long id;
    private Long orderId;
    private String exercises;
    private String equipment;
    private String nutritionPlan;
    private String scheduleInfo;

    /** ACTIVE, COMPLETED, REVISION_REQUESTED */
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}