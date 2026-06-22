package com.femfit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs once an hour and triggers class reminder emails
 * for bookings happening within the configured time window.
 */
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);
    private final BookingService bookingService;

    @Autowired
    public ReminderScheduler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedRate = 60 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void runReminderSweep() {
        try {
            int sent = bookingService.sendUpcomingClassReminders();
            log.debug("Reminder sweep done: {} processed", sent);
        } catch (Exception e) {
            log.error("Reminder sweep failed", e);
        }
    }
}
