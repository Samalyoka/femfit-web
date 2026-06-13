package com.femfit.model;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Weak entity — a booking of a class schedule by a member.
 * Identified by combination of user_id + schedule_id (partial key).
 * Maps to the {@code bookings} table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    private Long id;
    private Long memberId;
    private Long scheduleId;
    private LocalDateTime bookedAt;

    /** CONFIRMED, CANCELLED, ATTENDED */
    private String status;
    private String className;
    private String trainerName;
    private LocalDateTime scheduledAt;
    private String room;
}