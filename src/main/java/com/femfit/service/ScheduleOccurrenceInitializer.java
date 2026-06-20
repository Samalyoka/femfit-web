package com.femfit.service;

import com.femfit.dao.ClassScheduleDao;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Keeps the public class schedule populated with upcoming, bookable sessions.
 *
 * <p>{@code class_schedules} holds recurring weekly templates (day_of_week +
 * start_time, no specific date); {@code class_occurrences} holds the actual
 * dated sessions members book. Without something to keep generating new
 * occurrences as time moves forward, the schedule page would eventually run
 * out of upcoming dates again — exactly the bug this whole migration (v4)
 * was meant to fix.</p>
 *
 * <p>This runs once on every application startup and tops up occurrences
 * for the next {@link #WEEKS_AHEAD} weeks. It is intentionally idempotent
 * (the underlying SQL uses {@code ON CONFLICT DO NOTHING} on
 * {@code (schedule_id, occurrence_date)}), so restarting the app — or
 * deploying daily — never creates duplicate sessions.</p>
 */
@Component
public class ScheduleOccurrenceInitializer {

    private static final Logger log = LoggerFactory.getLogger(ScheduleOccurrenceInitializer.class);

    /** How many weeks ahead occurrences are kept generated for. */
    private static final int WEEKS_AHEAD = 8;

    private final ClassScheduleDao classScheduleDao;

    @Autowired
    public ScheduleOccurrenceInitializer(ClassScheduleDao classScheduleDao) {
        this.classScheduleDao = classScheduleDao;
    }

    @PostConstruct
    public void ensureUpcomingOccurrencesExist() {
        try {
            classScheduleDao.generateUpcomingOccurrences(WEEKS_AHEAD);
        } catch (Exception e) {
            // Never let schedule generation block application startup —
            // worst case the schedule page is temporarily stale, which is
            // far better than the whole app failing to boot.
            log.error("Failed to generate upcoming class occurrences on startup", e);
        }
    }
}