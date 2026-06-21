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

    /**
     * Per-item revision flags — which specific part(s) of the assignment
     * the client asked the trainer to redo. {@code status} becomes
     * REVISION_REQUESTED when any of these is true.
     */
    @Builder.Default
    private boolean revisionExercisesRequested = false;
    @Builder.Default
    private boolean revisionEquipmentRequested = false;
    @Builder.Default
    private boolean revisionNutritionRequested = false;
    @Builder.Default
    private boolean revisionScheduleRequested = false;

    /** Optional client comment explaining what needs to change. */
    private String revisionComment;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public long getClientId() {
        return orderId != null ? orderId : -1L;
    }

    /** True if the client has requested a revision on at least one item. */
    public boolean hasAnyRevisionRequested() {
        return revisionExercisesRequested || revisionEquipmentRequested
                || revisionNutritionRequested || revisionScheduleRequested;
    }
}