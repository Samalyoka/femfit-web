package com.femfit.model;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Represents a single bookable, dated class session — a projection joining
 * a {@code class_occurrences} row with its parent {@code class_schedules}
 * template (and the related fitness_classes/trainer info).
 *
 * <p>Despite the name, {@code id} here is the {@code class_occurrences.id}
 * (the value {@code bookings.schedule_id} points to), not the recurring
 * template's id. The class was kept as-is, rather than renamed/split into a
 * separate ClassOccurrence type, specifically so {@link Booking#getScheduleId()}
 * and all existing booking code did not need to change when the schedule
 * became recurring (migration v4) — only the DAO queries behind this
 * projection changed.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSchedule {

    /** class_occurrences.id — the bookable instance, NOT the template id. */
    private Long id;
    private Integer classId;
    private Long trainerId;
    /** Computed as occurrence_date + start_time from the template. */
    private LocalDateTime scheduledAt;
    private String room;
    private boolean cancelled;
    private int capacity;
    private String className;
    private String classNameRu;
    private String classNameKz;
    private String trainerName;
    private String category;
    private String difficultyLevel;
    private int spotsLeft;
    private int durationMinutes;

    /**
     * Returns the class name in the given language, falling back to the
     * English name if no translation is set for that language.
     *
     * @param lang locale language code ("ru", "kz", or anything else for English)
     * @return localized class name, never null (falls back to className)
     */
    public String getLocalizedClassName(String lang) {
        if ("ru".equals(lang) && classNameRu != null && !classNameRu.isBlank()) return classNameRu;
        if ("kz".equals(lang) && classNameKz != null && !classNameKz.isBlank()) return classNameKz;
        return className;
    }
}