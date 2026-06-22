package com.femfit.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingReminderDto {
    private Long bookingId;
    private String memberEmail;
    private String memberFirstName;
    private String className;
    private LocalDateTime scheduledAt;
    private String room;
}
