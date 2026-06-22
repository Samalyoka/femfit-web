package com.femfit.service.impl;

import com.femfit.dao.BookingDao;
import com.femfit.dto.BookingReminderDto;
import java.time.LocalDateTime;
import com.femfit.model.Booking;
import com.femfit.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of {@link BookingService}.
 *
 * <p>Capacity/duplicate validation and persistence are delegated to
 * {@link BookingDao#bookWithLock}, which performs them atomically within
 * a single database transaction (row-level lock on the schedule slot).
 * This avoids a check-then-act race condition where two concurrent
 * requests could both pass the capacity check and overbook the last spot.</p>
 */
@Service
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final BookingDao bookingDao;
    private final EmailService emailService;

    @org.springframework.beans.factory.annotation.Value("${mail.reminder.hours.before:24}")
    private int reminderHoursBefore;

    @Autowired
    public BookingServiceImpl(BookingDao bookingDao, EmailService emailService) {
        this.bookingDao = bookingDao;
        this.emailService = emailService;
    }

    @Override
    public Booking book(Long userId, Long scheduleId) {
        log.info("Booking request: userId={}, scheduleId={}", userId, scheduleId);
        Booking saved = bookingDao.bookWithLock(userId, scheduleId);
        log.info("Booking created: id={}", saved.getId());
        return saved;
    }

    @Override
    public void cancel(Long bookingId, Long userId) {
        log.info("Cancelling booking id={} by user id={}", bookingId, userId);
        bookingDao.cancel(bookingId, userId);
    }

    @Override
    public List<Booking> getUpcoming(Long userId) {
        return bookingDao.findUpcomingByUserId(userId);
    }

    @Override
    public int countVisitsThisMonth(Long userId) {
        return bookingDao.findUpcomingByUserId(userId).size();
    }

    @Override
    public int sendUpcomingClassReminders() {
        LocalDateTime windowStart = LocalDateTime.now().plusHours(reminderHoursBefore - 1L);
        LocalDateTime windowEnd   = LocalDateTime.now().plusHours(reminderHoursBefore);
        java.util.List<BookingReminderDto> due = bookingDao.findBookingsForReminder(windowStart, windowEnd);
        log.info("Reminder sweep: {} booking(s) due", due.size());
        for (BookingReminderDto b : due) emailService.sendClassReminder(b);
        return due.size();
    }
}