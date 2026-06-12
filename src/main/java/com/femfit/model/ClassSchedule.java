package com.femfit.model;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Associative entity linking FitnessClass and Trainer with a time slot.
 * Maps to the {@code class_schedules} table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSchedule {

    private Long id;
    private Integer classId;
    private Long trainerId;
    private LocalDateTime scheduledAt;
    private String room;
    private boolean cancelled;
    private int capacity;
    private String className;
    private String trainerName;
    private String category;
    private int spotsLeft;
    private int durationMinutes;
}